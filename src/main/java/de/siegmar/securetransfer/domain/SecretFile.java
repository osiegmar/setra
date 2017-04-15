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

public class SecretFile {

    private String id;
    private CryptedData name;
    private long originalFileSize;
    private long fileSize;
    private KeyIv keyIv;
    private Instant expiration;

    public SecretFile() {
    }

    public SecretFile(final String id, final CryptedData name, final long originalFileSize,
                      final long fileSize, final KeyIv keyIv, final Instant expiration) {
        this.id = id;
        this.name = name;
        this.originalFileSize = originalFileSize;
        this.fileSize = fileSize;
        this.keyIv = keyIv;
        this.expiration = expiration;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public CryptedData getName() {
        return name;
    }

    public void setName(final CryptedData name) {
        this.name = name;
    }

    public long getOriginalFileSize() {
        return originalFileSize;
    }

    public void setOriginalFileSize(final long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public KeyIv getKeyIv() {
        return keyIv;
    }

    public void setKeyIv(final KeyIv keyIv) {
        this.keyIv = keyIv;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(final Instant expiration) {
        this.expiration = expiration;
    }

}
