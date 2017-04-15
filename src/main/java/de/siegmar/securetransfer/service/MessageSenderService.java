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

package de.siegmar.securetransfer.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.CryptedData;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.ReceiverMessage;
import de.siegmar.securetransfer.domain.SecretFile;
import de.siegmar.securetransfer.domain.SenderMessage;
import de.siegmar.securetransfer.repository.FileRepository;
import de.siegmar.securetransfer.repository.MessageRepository;

@Service
public class MessageSenderService {

    private static final String DEFAULT_PASSWORD = "dummy";

    private final MessageRepository<SenderMessage> senderMsgRepository;
    private final MessageRepository<ReceiverMessage> receiverMsgRepository;
    private final FileRepository fileRepository;
    private final Cryptor cryptor;

    @Autowired
    public MessageSenderService(final MessageRepository<SenderMessage> senderMsgRepository,
                                final MessageRepository<ReceiverMessage> receiverMsgRepository,
                                final FileRepository fileRepository,
                                final Cryptor cryptor) {
        this.senderMsgRepository = senderMsgRepository;
        this.receiverMsgRepository = receiverMsgRepository;
        this.fileRepository = fileRepository;
        this.cryptor = cryptor;
    }

    /**
     * Stores a new secure message and return the senderId.
     */
    public String storeMessage(final String message, final List<MultipartFile> files,
                               final String password, final Instant expiration) {

        final boolean isMessagePasswordProtected = password != null;

        final String senderId = newRandomId();

        final String receiverId = storeMessage(senderId, message, files, password, expiration);

        saveSenderMessage(senderId,
            new SenderMessage(senderId, receiverId, isMessagePasswordProtected, expiration));

        return senderId;
    }

    String storeMessage(final String senderId, final String message,
                        final List<MultipartFile> multipartFiles, final String password,
                        final Instant expiration) {

        Objects.requireNonNull(senderId, "senderId must not be null");

        // Create encryptionKey and initialization vector (IV) to encrypt data
        final KeyIv encryptionKey = new KeyIv(cryptor.newKey(), cryptor.newIv());

        final String receiverId = newRandomId();

        final String hashedPassword =
            password != null ? BCrypt.hashpw(password, BCrypt.gensalt()) : null;

        final ReceiverMessage receiverMessage = new ReceiverMessage(
            receiverId,
            senderId,
            hashedPassword,
            encryptKey(MoreObjects.firstNonNull(password, DEFAULT_PASSWORD), encryptionKey),
            encryptMessage(message, encryptionKey.getKey()),
            encryptFiles(multipartFiles, encryptionKey, expiration),
            expiration
        );

        receiverMsgRepository.create(receiverId, receiverMessage);

        return receiverId;
    }

    private List<SecretFile> encryptFiles(final List<MultipartFile> multipartFiles,
                                          final KeyIv encryptionKey, final Instant expiration) {
        if (multipartFiles == null) {
            return Collections.emptyList();
        }

        return multipartFiles.stream()
            .map(multipartFile -> encryptFile(multipartFile, encryptionKey, expiration))
            .collect(Collectors.toList());
    }

    private SecretFile encryptFile(final MultipartFile multipartFile, final KeyIv encryptionKey,
                                   final Instant expiration) {
        try (final InputStream in = multipartFile.getInputStream()) {
            final byte[] fileIv = cryptor.newIv();
            final KeyIv fileKey = new KeyIv(encryptionKey.getKey(), fileIv);

            final byte[] encryptedFilename = cryptor.encryptString(
                multipartFile.getOriginalFilename(), fileKey);

            return fileRepository.storeFile(newRandomId(),
                new CryptedData(encryptedFilename, fileIv), in, fileKey, expiration);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private KeyIv encryptKey(final String password, final KeyIv encryptionKey) {
        final byte[] saltedPasswordHash = cryptor.keyFromSaltedPassword(password);
        final byte[] encryptedKey = cryptor.encrypt(encryptionKey.getKey(),
            new KeyIv(saltedPasswordHash, encryptionKey.getIv()));

        return new KeyIv(encryptedKey, encryptionKey.getIv());
    }

    private CryptedData encryptMessage(final String message, final byte[] encryptionKey) {
        if (message == null) {
            return null;
        }

        final byte[] messageIv = cryptor.newIv();
        final KeyIv messageKey = new KeyIv(encryptionKey, messageIv);

        return new CryptedData(cryptor.encryptString(message, messageKey), messageIv);
    }

    void saveSenderMessage(final String senderId, final SenderMessage message) {
        senderMsgRepository.create(senderId, message);
    }

    public SenderMessage getSenderMessage(final String senderId) {
        final SenderMessage senderMessage = senderMsgRepository.read(senderId);
        if (senderMessage == null) {
            throw new MessageNotFoundException();
        }

        return senderMessage;
    }

    String newRandomId() {
        final UUID uuid = UUID.randomUUID();
        return Hashing.sha256().newHasher()
            .putLong(System.nanoTime())
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .hash().toString();
    }

    public void burnSenderMessage(final String senderId) {
        if (!senderMsgRepository.delete(senderId)) {
            throw new MessageNotFoundException();
        }
    }

}
