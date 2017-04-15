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

package de.siegmar.securetransfer.repository.disk;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.siegmar.securetransfer.domain.SenderMessage;

public class SenderMessageDiskRepository extends AbstractDiskMessageRepository<SenderMessage> {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public SenderMessageDiskRepository(final Path baseDir)
        throws IOException {
        super(baseDir.resolve("sender_messages"));
    }

    @Override
    SenderMessage deserialize(final Path messageFilePath) throws IOException {
        return mapper.readValue(messageFilePath.toFile(), SenderMessage.class);
    }

    @Override
    void serialize(final Path messageFilePath, final SenderMessage message) throws IOException {
        mapper.writeValue(messageFilePath.toFile(), message);
    }

}
