/*
 * Copyright 2017 Oliver Siegmar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siegmar.securetransfer.repository.disk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import de.siegmar.securetransfer.domain.Message;
import de.siegmar.securetransfer.repository.MessageRepository;

public abstract class AbstractDiskMessageRepository<T extends Message>
    implements MessageRepository<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path messagePath;

    private final Map<String, Instant> messages = new ConcurrentHashMap<>();

    public AbstractDiskMessageRepository(final Path messagePath) throws IOException {
        this.messagePath = Files.createDirectories(messagePath);
    }

    @PostConstruct
    public void init() throws IOException {
        final AtomicInteger initCnt = new AtomicInteger();
        Files.list(messagePath).forEach(file -> {
            try {
                final T m = deserialize(file);
                messages.put(m.getId(), m.getExpiration());
                initCnt.incrementAndGet();
            } catch (final IOException e) {
                log.error("Error reading file {}", file, e);
            }
        });
        log.info("Initialized {} messages on disk", initCnt);
    }

    abstract T deserialize(final Path messageFilePath) throws IOException;

    abstract void serialize(final Path messageFilePath, final T message) throws IOException;

    @Override
    public void create(final String messageId, final T message) {
        log.info("Create message {}", messageId);

        final Path messageFilePath = this.messagePath.resolve(messageId);
        if (Files.exists(messageFilePath)) {
            throw new IllegalStateException("Message ID " + messageId + " already exists!");
        }

        messages.put(messageId, message.getExpiration());
        try {
            serialize(messageFilePath, message);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void update(final String messageId, final T message) {
        log.info("Update message {}", messageId);

        final Path messageFilePath = this.messagePath.resolve(messageId);
        if (!Files.exists(messageFilePath)) {
            throw new IllegalStateException("Message ID " + messageId + " does not exist!");
        }

        try {
            serialize(messageFilePath, message);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public T read(final String messageId) {
        log.info("Read message {}", messageId);

        final Path messageFilePath = this.messagePath.resolve(messageId);
        if (!Files.exists(messageFilePath)) {
            return null;
        }

        final T message;
        try {
            message = deserialize(messageFilePath);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        if (Instant.now().isAfter(message.getExpiration())) {
            // Delete will be handled by cleanup job
            return null;
        }

        return message;
    }

    @Override
    public boolean delete(final String messageId) {
        log.info("Delete message {}", messageId);

        final Path messageFilePath = this.messagePath.resolve(messageId);
        final boolean exists;
        try {
            exists = Files.deleteIfExists(messageFilePath);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        messages.remove(messageId);
        return exists;
    }

    @Scheduled(fixedDelay = 900_000)
    private void cleanup() {
        log.info("Starting message cleanup Job");

        final Instant now = Instant.now();

        int messageCnt = 0;
        for (final Iterator<Map.Entry<String, Instant>> it = messages.entrySet().iterator();
             it.hasNext();) {

            final Map.Entry<String, Instant> entry = it.next();

            if (now.isAfter(entry.getValue())) {
                try {
                    delete(entry.getKey());
                } catch (final UncheckedIOException e) {
                    log.error("Error deleting file {}", entry.getKey(), e);
                }
                it.remove();
                messageCnt++;
            }
        }

        log.info("Cleaned up {} messages", messageCnt);
    }

}
