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

package de.siegmar.securetransfer.repository;

import java.io.InputStream;
import java.time.Instant;

import de.siegmar.securetransfer.domain.CryptedData;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.domain.SecretFile;

public interface FileRepository {

    SecretFile resolveStoredFile(String id);

    InputStream getStoredFileInputStream(String id, KeyIv key);

    SecretFile storeFile(String id, CryptedData fileName, InputStream in, KeyIv key,
                         Instant expiration);

    void burnFile(String id);

}
