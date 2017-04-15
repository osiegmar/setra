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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.ReceiverMessage;
import de.siegmar.securetransfer.domain.SenderMessage;
import de.siegmar.securetransfer.repository.FileRepository;
import de.siegmar.securetransfer.repository.MessageRepository;
import de.siegmar.securetransfer.repository.disk.FileDiskRepository;
import de.siegmar.securetransfer.repository.disk.ReceiverMessageDiskRepository;
import de.siegmar.securetransfer.repository.disk.SenderMessageDiskRepository;
import de.siegmar.securetransfer.repository.memory.FileMemoryRepository;
import de.siegmar.securetransfer.repository.memory.MemoryMessageRepository;

@Configuration
public class RepositoryConfiguration {

    private final SecureTransferConfiguration config;
    private final Cryptor cryptor;

    @Autowired
    public RepositoryConfiguration(final SecureTransferConfiguration config,
                                   final Cryptor cryptor) {
        this.config = config;
        this.cryptor = cryptor;
    }

    @Bean
    public MessageRepository<SenderMessage> senderMessageRepository() throws IOException {
        final String messageRepository = config.getMessageRepository();
        switch (messageRepository) {
            case "disk":
                return new SenderMessageDiskRepository(config.getBaseDir());
            case "memory":
                return new MemoryMessageRepository<>();
            default:
                throw new IllegalStateException("Unknown message repository configured: "
                    + messageRepository);
        }
    }

    @Bean
    public MessageRepository<ReceiverMessage> receiverMessageRepository() throws IOException {
        final String messageRepository = config.getMessageRepository();
        switch (messageRepository) {
            case "disk":
                return new ReceiverMessageDiskRepository(config.getBaseDir());
            case "memory":
                return new MemoryMessageRepository<>();
            default:
                throw new IllegalStateException("Unknown message repository configured: "
                    + messageRepository);
        }
    }

    @Bean
    public FileRepository fileRepositoy() throws IOException {
        final String fileRepository = config.getFileRepository();
        switch (fileRepository) {
            case "disk":
                return new FileDiskRepository(config.getBaseDir(), cryptor);
            case "memory":
                return new FileMemoryRepository(cryptor);
            default:
                throw new IllegalStateException("Unknown file repository configured: "
                    + fileRepository);
        }
    }

}
