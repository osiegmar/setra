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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.siegmar.securetransfer.component.Cryptor;

public class CryptorTest {

    private final Cryptor cryptor =
        new Cryptor(new byte[]{34, 23, 56, 23, 68, 34, 23, 54});

    @Test
    public void testSuccess() {
        final byte[] key = cryptor.newKey();
        final byte[] iv = cryptor.newIv();

        final String text = "foo";
        final byte[] encrypted = cryptor.encryptString(text, new KeyIv(key, iv));
        final String decrypted = cryptor.decryptString(encrypted, new KeyIv(key, iv));
        assertEquals(text, decrypted);
    }

}
