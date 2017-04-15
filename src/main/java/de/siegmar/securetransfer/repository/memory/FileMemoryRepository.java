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

package de.siegmar.securetransfer.repository.memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.io.ByteStreams;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.CryptedData;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.SecretFile;
import de.siegmar.securetransfer.repository.FileRepository;

public class FileMemoryRepository implements FileRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FileMemoryRepository.class);

    private final Cryptor cryptor;
    private final Map<String, SecretFile> meta = new ConcurrentHashMap<>();
    private final Map<String, byte[]> data = new ConcurrentHashMap<>();

    public FileMemoryRepository(final Cryptor cryptor) {
        this.cryptor = cryptor;
    }

    @Override
    public SecretFile resolveStoredFile(final String id) {
        LOG.info("Read file {}", id);
        return meta.get(id);
    }

    @Override
    public InputStream getStoredFileInputStream(final String id, final KeyIv key) {
        LOG.info("Get stream for file {}", id);
        try {
            return cryptor.getCryptIn(new ByteArrayInputStream(data.get(id)),
                key);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public SecretFile storeFile(final String id, final CryptedData originalName,
                                final InputStream in, final KeyIv key,
                                final Instant expiration) {

        LOG.info("Store file {}", id);

        final ByteArrayOutputStream dataOut = new ByteArrayOutputStream();

        try {
            final long originalFileSize;

            try (final OutputStream cryptOut = cryptor.getCryptOut(
                dataOut, key)) {
                originalFileSize = ByteStreams.copy(in, cryptOut);
            }

            final SecretFile secretFile =
                new SecretFile(id, originalName, originalFileSize, dataOut.size(),
                    key, expiration);

            meta.put(id, secretFile);
            data.put(id, dataOut.toByteArray());

            return secretFile;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void burnFile(final String id) {
        LOG.info("Burn file {}", id);

        data.remove(id);
        meta.remove(id);
    }

    @Scheduled(fixedDelay = 900_000)
    private void cleanup() {
        LOG.info("Starting file cleanup Job");

        final Instant now = Instant.now();

        int messageCnt = 0;
        for (final Iterator<Map.Entry<String, SecretFile>> it = meta.entrySet().iterator();
             it.hasNext();) {

            final Map.Entry<String, SecretFile> entry = it.next();

            if (now.isAfter(entry.getValue().getExpiration())) {
                try {
                    burnFile(entry.getKey());
                } catch (final UncheckedIOException e) {
                    LOG.error("Error deleting file {}", entry.getKey(), e);
                }
                it.remove();
                messageCnt++;
            }
        }

        LOG.info("Cleaned up {} files", messageCnt);
    }

}
