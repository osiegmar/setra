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

package de.siegmar.securetransfer.controller.dto;

import java.util.List;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;

public class EncryptMessageCommand {

    public static final int MESSAGE_MAX_LENGTH = 50_000;
    public static final int PASSWORD_MAX_LENGTH = 64;
    public static final int MAX_EXPIRATION = 30;

    @Size(max = MESSAGE_MAX_LENGTH)
    private String message;

    private List<MultipartFile> files;

    @Size(max = PASSWORD_MAX_LENGTH)
    private String password;

    @Range(min = 1, max = MAX_EXPIRATION)
    private Integer expirationDays = 1;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(final List<MultipartFile> files) {
        this.files = files;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(final Integer expirationDays) {
        this.expirationDays = expirationDays;
    }

}
