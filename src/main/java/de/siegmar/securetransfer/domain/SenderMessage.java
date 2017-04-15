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
import java.util.Objects;

public class SenderMessage extends Message {

    private String receiverId;
    private boolean passwordEncrypted;
    private Instant received;

    public SenderMessage() {
    }

    public SenderMessage(final String senderId, final String receiverId,
                         final boolean passwordEncrypted, final Instant expiration) {
        this(senderId, receiverId, passwordEncrypted, null, expiration);
    }

    public SenderMessage(final String senderId, final String receiverId,
                         final boolean passwordEncrypted, final Instant received,
                         final Instant expiration) {
        super(senderId, expiration);
        this.receiverId = Objects.requireNonNull(receiverId);
        this.passwordEncrypted = passwordEncrypted;
        this.received = received;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(final String receiverId) {
        this.receiverId = receiverId;
    }

    public boolean isPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(final boolean passwordEncrypted) {
        this.passwordEncrypted = passwordEncrypted;
    }

    public Instant getReceived() {
        return received;
    }

    public void setReceived(final Instant received) {
        this.received = received;
    }
}
