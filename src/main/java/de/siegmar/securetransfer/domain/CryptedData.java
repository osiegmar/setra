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

public class CryptedData {

    private byte[] data;
    private byte[] iv;

    public CryptedData() {
    }

    public CryptedData(final byte[] data, final byte[] iv) {
        this.data = data.clone();
        this.iv = iv.clone();
    }

    public byte[] getData() {
        return data.clone();
    }

    public void setData(final byte[] data) {
        this.data = data.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public void setIv(final byte[] iv) {
        this.iv = iv.clone();
    }

}
