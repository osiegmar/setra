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

package de.siegmar.securetransfer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.io.BaseEncoding;

import de.siegmar.securetransfer.component.Cryptor;

@Configuration
public class CryptorConfig {

    private final SecureTransferConfiguration config;

    @Autowired
    public CryptorConfig(final SecureTransferConfiguration config) {
        this.config = config;
    }

    @Bean
    public Cryptor cryptor() {
        if (config.getSalt() != null) {
            return new Cryptor(BaseEncoding.base16().lowerCase().decode(config.getSalt()));
        }

        return new Cryptor(config.getBaseDir());
    }

}
