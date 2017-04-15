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

package de.siegmar.securetransfer.repository.memory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.siegmar.securetransfer.domain.Message;
import de.siegmar.securetransfer.repository.MessageRepository;

public class MemoryMessageRepository<T extends Message> implements MessageRepository<T> {

    private final Map<String, T> messages = new ConcurrentHashMap<>();

    @Override
    public void create(final String messageId, final T message) {
        if (messages.containsKey(messageId)) {
            throw new IllegalStateException("Message ID " + messageId + " already exists!");
        }

        messages.put(messageId, message);
    }

    @Override
    public void update(final String messageId, final T message) {
        if (!messages.containsKey(messageId)) {
            throw new IllegalStateException("Message ID " + messageId + " does not exist!");
        }

        messages.put(messageId, message);
    }

    @Override
    public T read(final String messageId) {
        return messages.get(messageId);
    }

    @Override
    public boolean delete(final String messageId) {
        return messages.remove(messageId) != null;
    }

}
