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

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.CryptedData;
import de.siegmar.securetransfer.domain.DecryptedFile;
import de.siegmar.securetransfer.domain.DecryptedMessage;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.ReceiverMessage;
import de.siegmar.securetransfer.domain.SecretFile;
import de.siegmar.securetransfer.domain.SenderMessage;
import de.siegmar.securetransfer.repository.FileRepository;
import de.siegmar.securetransfer.repository.MessageRepository;

@Service
public class MessageReceiverService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageReceiverService.class);

    private static final String DEFAULT_PASSWORD = "dummy";

    private final MessageRepository<SenderMessage> senderMsgRepository;
    private final MessageRepository<ReceiverMessage> receiverMsgRepository;
    private final FileRepository fileRepository;
    private final Cryptor cryptor;

    @Autowired
    public MessageReceiverService(final MessageRepository<SenderMessage> senderMsgRepository,
                                  final MessageRepository<ReceiverMessage> receiverMsgRepository,
                                  final FileRepository fileRepository,
                                  final Cryptor cryptor) {
        this.senderMsgRepository = senderMsgRepository;
        this.receiverMsgRepository = receiverMsgRepository;
        this.fileRepository = fileRepository;
        this.cryptor = cryptor;
    }

    public boolean isMessagePasswordProtected(final String receiverId) {
        return getReceiverMessage(receiverId).getPassword() != null;
    }

    private ReceiverMessage getReceiverMessage(final String receiverId) {
        final ReceiverMessage receiverMessage = receiverMsgRepository.read(receiverId);

        if (receiverMessage == null) {
            throw new MessageNotFoundException();
        }

        return receiverMessage;
    }

    public DecryptedMessage decryptAndBurnMessage(final String receiverId, final byte[] linkSecret, final String password) {
        final ReceiverMessage receiverMessage = getReceiverMessage(receiverId);

        if (password != null) {
            validatePassword(receiverId, password, receiverMessage);
        } else if (receiverMessage.getPassword() != null) {
            throw new IllegalStateException("Message is password protected");
        }

        receiverMsgRepository.delete(receiverId);
        updateSenderMessageReceived(receiverMessage.getSenderId());

        // Decrypt the encryption key with the given (validated) password
        final byte[] encryptionKey =
            decryptEncryptionKey(
                linkSecret,
                MoreObjects.firstNonNull(password, DEFAULT_PASSWORD),
                receiverMessage);

        final List<DecryptedFile> decryptedFiles = receiverMessage.getFiles() == null
            ? null
            : receiverMessage.getFiles().stream()
            .map(f -> decryptFile(f, new KeyIv(encryptionKey, f.getKeyIv().getIv())))
            .collect(Collectors.toList());

        return new DecryptedMessage(
            decryptMessage(receiverMessage.getMessage(), encryptionKey),
            decryptedFiles
        );
    }

    private void validatePassword(final String receiverId, final String password,
                                  final ReceiverMessage receiverMessage) {

        if (receiverMessage.getPassword() == null) {
            throw new IllegalStateException("Message is not password protected");
        }

        final int decryptAttempts = receiverMessage.incrementDecryptAttempt();

        receiverMsgRepository.update(receiverMessage.getId(), receiverMessage);

        if (BCrypt.checkpw(password, receiverMessage.getPassword())) {
            return;
        }

        // invalid password ...

        if (decryptAttempts > 2) {
            // burn if too many failed attempts
            receiverMsgRepository.delete(receiverId);

            // inform the sender about invalidation
            updateSenderMessageInvalidated(receiverMessage.getSenderId());

            throw new MessageNotFoundException();
        }

        throw new IllegalStateException("Incorrect password");
    }

    private byte[] decryptEncryptionKey(
        final byte[] linkSecret,
        final String password, final ReceiverMessage message) {

        Preconditions.checkNotNull(linkSecret);
        Preconditions.checkNotNull(password);
        final byte[] saltedSecretHash =
            cryptor.keyFromSaltedPasswordAndSecret(password, linkSecret);

        final byte[] key = message.getKeyIv().getKey();
        final byte[] iv = message.getKeyIv().getIv();

        return cryptor.decrypt(key, new KeyIv(saltedSecretHash, iv));
    }

    private String decryptMessage(final CryptedData message, final byte[] encryptionKey) {
        if (message == null) {
            return null;
        }

        return cryptor.decryptString(message.getData(), new KeyIv(encryptionKey, message.getIv()));
    }

    private void updateSenderMessageReceived(final String senderId) {
        final SenderMessage senderMessage = senderMsgRepository.read(senderId);
        if (senderMessage.getReceived() == null) {
            senderMessage.setReceived(Instant.now());
            senderMsgRepository.update(senderId, senderMessage);
        }
    }

    private void updateSenderMessageInvalidated(final String senderId) {
        final SenderMessage senderMessage = senderMsgRepository.read(senderId);
        if (senderMessage.getInvalidated() == null) {
            senderMessage.setInvalidated(Instant.now());
            senderMsgRepository.update(senderId, senderMessage);
        }
    }

    public DecryptedFile resolveStoredFile(final String id, final KeyIv keyIv) {
        final SecretFile secretFile = fileRepository.resolveStoredFile(id);

        if (secretFile == null) {
            throw new MessageNotFoundException();
        }

        return decryptFile(secretFile, keyIv);
    }

    private DecryptedFile decryptFile(final SecretFile secretFile,
                                      final KeyIv keyIv) {

        return new DecryptedFile(secretFile.getId(),
            cryptor.decryptString(secretFile.getName().getData(), keyIv),
            secretFile.getOriginalFileSize(), secretFile.getKeyIv());
    }

    public InputStream getStoredFileInputStream(final String id, final KeyIv keyIv) {
        return fileRepository.getStoredFileInputStream(id, keyIv);
    }

    public void burnFile(final String fileId) {
        fileRepository.burnFile(fileId);
    }

}
