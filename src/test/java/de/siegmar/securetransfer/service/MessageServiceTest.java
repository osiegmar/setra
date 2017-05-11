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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;

import org.junit.Test;

import com.google.common.hash.HashCode;

import de.siegmar.securetransfer.component.Cryptor;
import de.siegmar.securetransfer.domain.DecryptedMessage;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.ReceiverMessage;
import de.siegmar.securetransfer.domain.SenderMessage;
import de.siegmar.securetransfer.repository.FileRepository;
import de.siegmar.securetransfer.repository.disk.FileDiskRepository;
import de.siegmar.securetransfer.repository.memory.MemoryMessageRepository;

public class MessageServiceTest {

    private final MessageSenderService messageService;
    private final MessageReceiverService messageReceiverService;

    public MessageServiceTest() throws IOException {
        final Cryptor cryptor = new Cryptor(new byte[]{34, 23, 56, 23, 68, 34, 23, 54});

        final MemoryMessageRepository<SenderMessage> senderMsgRepository =
            new MemoryMessageRepository<>();

        final MemoryMessageRepository<ReceiverMessage> receiverMsgRepository =
            new MemoryMessageRepository<>();

        final FileRepository fileRepository =
            new FileDiskRepository(Paths.get(System.getProperty("java.io.tmpdir")), cryptor);

        messageService = new MessageSenderService(senderMsgRepository, receiverMsgRepository,
            fileRepository, cryptor);

        messageReceiverService = new MessageReceiverService(senderMsgRepository,
            receiverMsgRepository, fileRepository, cryptor);
    }

    @Test
    public void withoutPassword() {
        // Store without password
        final String message = "secure message";
        final String senderId = messageService.newRandomId();
        final String noPassword = null;
        final byte[] linkSecret = HashCode.fromString(messageService.newRandomId()).asBytes();
        final Instant expiration = Instant.now().plusSeconds(60);
        final KeyIv encryptionKey = messageService.newEncryptionKey();
        final String receiverId =
            messageService.storeMessage(
                senderId, message, encryptionKey, null,
                linkSecret, noPassword, expiration);
        messageService.saveSenderMessage(senderId,
            new SenderMessage(senderId, receiverId, false, expiration));

        // Message is not password encrypted
        assertFalse(messageReceiverService.isMessagePasswordProtected(receiverId));

        // Retrieve
        final DecryptedMessage retrievedMessage =
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, noPassword);
        assertEquals(message, retrievedMessage.getMessage());

        // No retrieval possible anymore
        try {
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, noPassword);
            fail("MessageNotFoundException expected");
        } catch (final MessageNotFoundException e) {
            // expected
        }

        // Private info now has received date stored
        final SenderMessage senderMessage3 = messageService.getSenderMessage(senderId);
        assertNotNull(senderMessage3.getReceived());
    }

    @Test
    public void withPassword() {
        // Store without password
        final String message = "secure message";
        final byte[] linkSecret = messageService.newEncryptionKey().getKey();
        final String password = "key";
        final String senderId = messageService.newRandomId();
        final Instant expiration = Instant.now().plusSeconds(60);
        final KeyIv encryptionKey = messageService.newEncryptionKey();
        final String receiverId = messageService.storeMessage(senderId, message, encryptionKey,
            null, linkSecret, password, expiration);
        messageService.saveSenderMessage(senderId,
            new SenderMessage(senderId, receiverId, true, expiration));

        // Message is password encrypted
        assertTrue(messageReceiverService.isMessagePasswordProtected(receiverId));

        // Message can't be retrieved without password
        try {
            messageReceiverService.decryptAndBurnMessage(
                receiverId, linkSecret, null/*no password*/);
            fail("IllegalStateException expected");
        } catch (final IllegalStateException e) {
            // expected
        }

        // Retrieve
        final DecryptedMessage retrievedMessage =
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, password);
        assertEquals(message, retrievedMessage.getMessage());

        // No retrieval possible anymore
        try {
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, password);
            fail("MessageNotFoundException expected");
        } catch (final MessageNotFoundException e) {
            // expected
        }

        // Private info now has received date stored
        final SenderMessage senderMessage3 = messageService.getSenderMessage(senderId);
        assertNotNull(senderMessage3.getReceived());
    }

    @Test
    public void tooManyAttempts() {
        // Store without password
        final String message = "secure message";
        final byte[] linkSecret = messageService.newEncryptionKey().getKey();
        final String password = "key";
        final String senderId = messageService.newRandomId();
        final Instant expiration = Instant.now().plusSeconds(60);
        final KeyIv encryptionKey = messageService.newEncryptionKey();
        final String receiverId = messageService.storeMessage(senderId, message, encryptionKey,
            null, linkSecret, password, expiration);
        messageService.saveSenderMessage(senderId,
            new SenderMessage(senderId, receiverId, true, expiration));

        // Message is password encrypted
        assertTrue(messageReceiverService.isMessagePasswordProtected(receiverId));

        // Message can't be retrieved without password
        try {
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, "wrong");
            fail("IllegalStateException expected");
        } catch (final IllegalStateException e) {
            // expected
        }

        // Message can't be retrieved without password
        try {
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, "wrong");
            fail("IllegalStateException expected");
        } catch (final IllegalStateException e) {
            // expected
        }

        // Message can't be retrieved without password
        try {
            messageReceiverService.decryptAndBurnMessage(receiverId, linkSecret, "wrong");
            fail("MessageNotFoundException expected");
        } catch (final MessageNotFoundException e) {
            // expected
        }
    }

}
