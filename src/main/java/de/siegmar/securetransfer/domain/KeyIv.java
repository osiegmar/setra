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

public class KeyIv {

    private byte[] key;
    private byte[] iv;

    public KeyIv() {
    }

    public KeyIv(final byte[] key, final byte[] iv) {
        this.key = key.clone();
        this.iv = iv.clone();
    }

    public byte[] getKey() {
        return key.clone();
    }

    public void setKey(final byte[] key) {
        this.key = key.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public void setIv(final byte[] iv) {
        this.iv = iv.clone();
    }

}
