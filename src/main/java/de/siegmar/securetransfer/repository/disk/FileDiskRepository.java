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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.ByteStreams;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.CryptedData;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.SecretFile;
import de.siegmar.securetransfer.repository.FileRepository;

public class FileDiskRepository implements FileRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FileDiskRepository.class);
    private static final String META_SUFFIX = ".meta";
    private static final String DATA_SUFFIX = ".data";
    private static final String TMP_SUFFIX = ".tmp";

    private final Path storePath;
    private final Cryptor cryptor;
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Map<String, SecretFile> files = new ConcurrentHashMap<>();

    public FileDiskRepository(final Path baseDir, final Cryptor cryptor) throws IOException {
        this.storePath = Files.createDirectories(baseDir.resolve("store"));
        this.cryptor = cryptor;
    }

    @PostConstruct
    public void init() throws IOException {
        final List<Path> storedFiles = Files.list(storePath).collect(Collectors.toList());

        storedFiles.stream()
            .filter(p -> p.getFileName().toString().endsWith(DATA_SUFFIX + TMP_SUFFIX))
            .forEach(file -> {
                try {
                    LOG.info("Clean up stale upload tmp file: {}", file);
                    Files.delete(file);
                } catch (final IOException e) {
                    LOG.error("Error deleting stale upload tmp file: {}", file, e);
                }
            });


        final AtomicInteger initCnt = new AtomicInteger();
        storedFiles.stream()
            .filter(p -> p.getFileName().toString().endsWith(META_SUFFIX))
            .forEach(file -> {
                try {
                    final SecretFile secretFile = mapper.readValue(file.toFile(), SecretFile.class);
                    files.put(secretFile.getId(), secretFile);
                    initCnt.incrementAndGet();
                } catch (final IOException e) {
                    LOG.error("Error reading file {}", file, e);
                }
            });
        LOG.info("Initialized {} files on disk", initCnt);
    }

    @Override
    public SecretFile resolveStoredFile(final String id) {
        LOG.info("Read file {}", id);
        return files.get(id);
    }

    @Override
    public InputStream getStoredFileInputStream(final String id, final KeyIv key) {
        LOG.info("Get stream for file {}", id);
        try {
            return cryptor.getCryptIn(Files.newInputStream(resolveDataPath(id)),
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

        final Path metaFile = resolveMetaPath(id);
        final Path dataFile = resolveDataPath(id);
        final Path dataTmpFile = dataFile.resolveSibling(dataFile.getFileName() + TMP_SUFFIX);

        try {
            final long originalFileSize;

            try (final OutputStream cryptOut = cryptor.getCryptOut(
                Files.newOutputStream(dataTmpFile), key)) {
                originalFileSize = ByteStreams.copy(in, cryptOut);
            } catch (final IOException e) {
                Files.delete(dataTmpFile);
                throw e;
            }

            Files.move(dataTmpFile, dataFile, StandardCopyOption.ATOMIC_MOVE);

            final SecretFile secretFile =
                new SecretFile(id, originalName, originalFileSize, Files.size(dataFile),
                    key, expiration);

            mapper.writeValue(metaFile.toFile(), secretFile);

            files.put(id, secretFile);

            return secretFile;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveDataPath(final String id) {
        return storePath.resolve(id + DATA_SUFFIX);
    }

    private Path resolveMetaPath(final String id) {
        return storePath.resolve(id + META_SUFFIX);
    }

    @Override
    public void burnFile(final String id) {
        LOG.info("Burn file {}", id);

        try {
            Files.delete(resolveDataPath(id));
            Files.delete(resolveMetaPath(id));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        files.remove(id);
    }

    @Scheduled(fixedDelay = 900_000)
    private void cleanup() {
        LOG.info("Starting file cleanup Job");

        final Instant now = Instant.now();

        int messageCnt = 0;
        for (final Iterator<Map.Entry<String, SecretFile>> it = files.entrySet().iterator();
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
