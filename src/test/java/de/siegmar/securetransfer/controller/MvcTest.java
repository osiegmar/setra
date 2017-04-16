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

package de.siegmar.securetransfer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import de.siegmar.securetransfer.controller.dto.EncryptMessageCommand;
import de.siegmar.securetransfer.domain.DecryptedFile;
import de.siegmar.securetransfer.domain.DecryptedMessage;
import de.siegmar.securetransfer.domain.SenderMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SuppressWarnings("checkstyle:executablestatementcount")
public class MvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getForm() throws Exception {
        mockMvc.perform(get("/send"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("send/send_form"))
            .andExpect(model().attributeExists("command"))
            .andExpect(model().attribute("message_max_length",
                EncryptMessageCommand.MESSAGE_MAX_LENGTH))
            .andExpect(model().attribute("password_max_length",
                EncryptMessageCommand.PASSWORD_MAX_LENGTH));
    }

    @Test
    public void invalidFormSubmit() throws Exception {
        // message missing
        mockMvc.perform(post("/send"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(model().hasErrors());
    }

    @Test
    public void messageWithoutFileWithoutPassword() throws Exception {
        final String messageToSend = "my secret message";

        // Create new message and expect redirect with flash message after store
        final MvcResult createMessageResult = mockMvc.perform(post("/send")
            .param("message", messageToSend))
            .andExpect(status().isFound())
            .andExpect(redirectedUrlPattern("/send/**"))
            .andExpect(flash().attribute("message", messageToSend))
            .andReturn();

        // receive data after redirect
        final String messageStatusUrl = createMessageResult.getResponse().getRedirectedUrl();

        final MvcResult messageStatusResult = mockMvc.perform(get(messageStatusUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("send/message_status"))
            .andReturn();

        final SenderMessage senderMessage =
            (SenderMessage) messageStatusResult.getModelAndView().getModel().get("senderMessage");

        assertNotNull(senderMessage);
        assertNotNull(senderMessage.getId());
        assertNotNull(senderMessage.getReceiverId());
        assertNotNull(senderMessage.getExpiration());
        assertNull(senderMessage.getReceived());
        assertFalse(senderMessage.isPasswordEncrypted());

        final String receiveUrl =
            (String) messageStatusResult.getModelAndView().getModel().get("receiveUrl");

        assertNotNull(receiveUrl);

        // call receiver URL
        final MvcResult confirmPage = mockMvc.perform(get(receiveUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("receive/message_confirm"))
            .andReturn();

        final Document confirmPageDoc = Jsoup.parse(confirmPage.getResponse().getContentAsString());
        final String confirmUrl =
            confirmPageDoc.getElementsByTag("form").attr("action");

        // Receive message
        final MvcResult messageResult = mockMvc.perform(get(confirmUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("receive/message"))
            .andReturn();

        final DecryptedMessage decryptedMessage =
            (DecryptedMessage) messageResult.getModelAndView().getModel().get("decryptedMessage");

        assertEquals(messageToSend, decryptedMessage.getMessage());
        assertEquals(0, decryptedMessage.getFiles().size());

        // Check message is burned
        mockMvc.perform(get(receiveUrl))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("message_not_found"));

        // Check sender status page
        final MvcResult messageStatusResult2 = mockMvc.perform(get(messageStatusUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("send/message_status"))
            .andReturn();

        final SenderMessage senderMessage2 =
            (SenderMessage) messageStatusResult2.getModelAndView().getModel().get("senderMessage");

        assertNotNull(senderMessage2);
        assertNotNull(senderMessage2.getId());
        assertNotNull(senderMessage2.getReceiverId());
        assertNotNull(senderMessage2.getExpiration());
        assertNotNull(senderMessage2.getReceived());
        assertFalse(senderMessage.isPasswordEncrypted());
    }

    @Test
    public void messageWithFileWithPassword() throws Exception {
        final String messageToSend = "my secret message";
        final String password = "top secret password";
        final String fileContent = "test file content";

        // Create new message and expect redirect with flash message after store
        final MvcResult createMessageResult = mockMvc.perform(fileUpload("/send")
            .file("files", fileContent.getBytes(StandardCharsets.UTF_8))
            .param("message", messageToSend)
            .param("password", password))
            .andExpect(status().isFound())
            .andExpect(redirectedUrlPattern("/send/**"))
            .andExpect(flash().attribute("message", messageToSend))
            .andReturn();

        // receive data after redirect
        final String messageStatusUrl = createMessageResult.getResponse().getRedirectedUrl();

        final MvcResult messageStatusResult = mockMvc.perform(get(messageStatusUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("send/message_status"))
            .andReturn();

        final SenderMessage senderMessage =
            (SenderMessage) messageStatusResult.getModelAndView().getModel().get("senderMessage");

        assertNotNull(senderMessage);
        assertNotNull(senderMessage.getId());
        assertNotNull(senderMessage.getReceiverId());
        assertNotNull(senderMessage.getExpiration());
        assertNull(senderMessage.getReceived());
        assertTrue(senderMessage.isPasswordEncrypted());

        final String receiveUrl =
            (String) messageStatusResult.getModelAndView().getModel().get("receiveUrl");

        assertNotNull(receiveUrl);

        // call receiver URL
        final MvcResult confirmPage = mockMvc.perform(get(receiveUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("receive/message_ask_password"))
            .andReturn();

        final Document confirmPageDoc = Jsoup.parse(confirmPage.getResponse().getContentAsString());
        final String passwordUrl =
            confirmPageDoc.getElementsByTag("form").attr("action");

        // Receive message
        final MvcResult messageResult = mockMvc.perform(post(passwordUrl)
            .param("password", password))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("receive/message"))
            .andReturn();

        final DecryptedMessage decryptedMessage =
            (DecryptedMessage) messageResult.getModelAndView().getModel().get("decryptedMessage");

        assertEquals(messageToSend, decryptedMessage.getMessage());
        assertEquals(1, decryptedMessage.getFiles().size());

        final DecryptedFile file = decryptedMessage.getFiles().get(0);
        final String fileId = file.getId();
        final String fileKey = file.getKeyHex();

        // Download file
        final MvcResult downloadResult = mockMvc
            .perform(get("/receive/file/{id}/{key}", fileId, fileKey)
                .sessionAttr("iv_file_" + fileId, file.getKeyIv().getIv()))
            .andExpect(request().asyncStarted())
            //.andExpect(request().asyncResult("Deferred result"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andReturn();

        downloadResult.getAsyncResult();
        assertEquals(fileContent, downloadResult.getResponse().getContentAsString());

        // Check message is burned
        mockMvc.perform(get(receiveUrl))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("message_not_found"));

        // Check file is burned
        mockMvc
            .perform(get("/receive/file/{id}/{key}", fileId, fileKey)
                .sessionAttr("iv_file_" + fileId, file.getKeyIv().getIv()))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("message_not_found"));

        // Check sender status page
        final MvcResult messageStatusResult2 = mockMvc.perform(get(messageStatusUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/html;charset=UTF-8"))
            .andExpect(view().name("send/message_status"))
            .andReturn();

        final SenderMessage senderMessage2 =
            (SenderMessage) messageStatusResult2.getModelAndView().getModel().get("senderMessage");

        assertNotNull(senderMessage2);
        assertNotNull(senderMessage2.getId());
        assertNotNull(senderMessage2.getReceiverId());
        assertNotNull(senderMessage2.getExpiration());
        assertNotNull(senderMessage2.getReceived());
        assertTrue(senderMessage.isPasswordEncrypted());
    }

}
