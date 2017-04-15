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

import com.google.common.io.BaseEncoding;

public class DecryptedFile {

    private String id;
    private String name;
    private long originalFileSize;
    private KeyIv keyIv;

    public DecryptedFile() {
    }

    public DecryptedFile(final String id, final String name, final long originalFileSize,
                         final KeyIv keyIv) {
        this.id = id;
        this.name = name;
        this.originalFileSize = originalFileSize;
        this.keyIv = keyIv;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getOriginalFileSize() {
        return originalFileSize;
    }

    public void setOriginalFileSize(final long originalFileSize) {
        this.originalFileSize = originalFileSize;
    }

    public KeyIv getKeyIv() {
        return keyIv;
    }

    public String getKeyHex() {
        return BaseEncoding.base16().lowerCase().encode(keyIv.getKey());
    }

    public void setKeyIv(final KeyIv keyIv) {
        this.keyIv = keyIv;
    }

}
