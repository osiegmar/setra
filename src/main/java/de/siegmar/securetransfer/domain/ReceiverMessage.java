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

package de.siegmar.securetransfer.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceiverMessage extends Message {

    private String senderId;
    private String password;
    private CryptedData message;
    private KeyIv keyIv;
    private List<SecretFile> files;
    private AtomicInteger decryptAttempts = new AtomicInteger(0);

    public ReceiverMessage() {
    }

    public ReceiverMessage(final String receiverId, final String senderId, final String password,
                           final KeyIv keyIv, final CryptedData message,
                           final List<SecretFile> files, final Instant expiration) {
        super(receiverId, expiration);
        this.senderId = Objects.requireNonNull(senderId);
        this.password = password;
        this.keyIv = keyIv;
        this.message = message;
        this.files = files;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(final String senderId) {
        this.senderId = senderId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public KeyIv getKeyIv() {
        return keyIv;
    }

    public void setKeyIv(final KeyIv keyIv) {
        this.keyIv = keyIv;
    }

    public CryptedData getMessage() {
        return message;
    }

    public void setMessage(final CryptedData message) {
        this.message = message;
    }

    public List<SecretFile> getFiles() {
        return files;
    }

    public void setFiles(final List<SecretFile> files) {
        this.files = files;
    }

    public AtomicInteger getDecryptAttempts() {
        return decryptAttempts;
    }

    public void setDecryptAttempts(final AtomicInteger decryptAttempts) {
        this.decryptAttempts = decryptAttempts;
    }

    public int incrementDecryptAttempt() {
        return decryptAttempts.incrementAndGet();
    }

}
