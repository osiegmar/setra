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

package de.siegmar.securetransfer.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "securetransfer")
@Validated
public class SecureTransferConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecureTransferConfiguration.class);

    @NotNull
    private Path baseDir =
        Paths.get(System.getProperty("java.io.tmpdir")).resolve("securetransfer");

    private boolean createBaseDir = true;

    @NotNull
    private String messageRepository;

    @NotNull
    private String fileRepository;

    private String salt;

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(final Path baseDir) {
        this.baseDir = baseDir;
    }

    public boolean isCreateBaseDir() {
        return createBaseDir;
    }

    public void setCreateBaseDir(final boolean createBaseDir) {
        this.createBaseDir = createBaseDir;
    }

    public String getMessageRepository() {
        return messageRepository;
    }

    public void setMessageRepository(final String messageRepository) {
        this.messageRepository = messageRepository;
    }

    public String getFileRepository() {
        return fileRepository;
    }

    public void setFileRepository(final String fileRepository) {
        this.fileRepository = fileRepository;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    @PostConstruct
    public void init() {
        if (!createBaseDir) {
            // Don't create dir in tests
            return;
        }

        LOG.info("Initialize base directory {}", baseDir);

        if (!Files.isDirectory(baseDir)) {
            try {
                Files.createDirectories(baseDir,
                    PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rwx------")));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
