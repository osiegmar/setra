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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;

import de.siegmar.securetransfer.controller.dto.DecryptMessageCommand;
import de.siegmar.securetransfer.domain.DecryptedFile;
import de.siegmar.securetransfer.domain.DecryptedMessage;
import de.siegmar.securetransfer.domain.KeyIv;
import de.siegmar.securetransfer.service.MessageNotFoundException;
import de.siegmar.securetransfer.service.MessageReceiverService;

@Controller
@RequestMapping("/receive")
public class ReceiveController {

    private static final String FORM_ASK_PASSWORD = "receive/message_ask_password";
    private static final String FORM_CONFIRM = "receive/message_confirm";
    private static final String FORM_MSG_DISPLAY = "receive/message";

    private final MessageReceiverService messageService;

    @Autowired
    public ReceiveController(final MessageReceiverService messageService) {
        this.messageService = messageService;
    }

    /**
     * Ask for message retrieval (return confirm or password dialog).
     */
    @GetMapping("/{id:[a-f0-9]{64}}")
    public String receive(@PathVariable("id") final String id,
        @RequestParam("linkSecret") final String linkSecret,
        final Model model) {
        final boolean isPasswordProtected = messageService.isMessagePasswordProtected(id);

        model.addAttribute("linkSecret", linkSecret);

        if (isPasswordProtected) {
            model
                .addAttribute("id", id)
                .addAttribute("command", new DecryptMessageCommand());
            return FORM_ASK_PASSWORD;
        }

        return FORM_CONFIRM;
    }

    /**
     * Receive non-password protected message.
     */
    @GetMapping("/confirm/{id:[a-f0-9]{64}}")
    public String confirm(
            @PathVariable("id") final String id,
            @ModelAttribute("linkSecret") final String linkSecret,
            final Model model,
            final HttpSession session) {

        prepareMessage(id, linkSecret, null, model, session);

        return FORM_MSG_DISPLAY;
    }

    private void prepareMessage(final String id,
        final String linkSecret,
        final String password, final Model model,
                                final HttpSession session) {

        final DecryptedMessage decryptedMessage =
            messageService.decryptAndBurnMessage(id,
                HashCode.fromString(linkSecret).asBytes()
                , password);

        model.addAttribute("decryptedMessage", decryptedMessage);

        // store iv to session to prevent download link "sharing"
        decryptedMessage.getFiles().forEach(f ->
            session.setAttribute(buildSessionAttr(f.getId()), f.getKeyIv().getIv()));
    }

    private String buildSessionAttr(final String fileId) {
        return "iv_file_" + fileId;
    }

    /**
     * Receive password protected message.
     */
    @PostMapping("/password/{id:[a-f0-9]{64}}")
    public String password(@PathVariable("id") final String id,
                           @ModelAttribute("linkSecret") final String linkSecret,
                           @Valid @ModelAttribute("command") final DecryptMessageCommand cmd,
                           final Errors errors, final Model model,
                           final HttpSession session) {

        if (errors.hasErrors()) {
            model.addAttribute("id", id);
            return FORM_ASK_PASSWORD;
        }

        try {
            prepareMessage(id, linkSecret, cmd.getPassword(), model, session);
            return FORM_MSG_DISPLAY;
        } catch (final IllegalStateException e) {
            errors.rejectValue("password", null, "Invalid password");
            model.addAttribute("id", id);
            return FORM_ASK_PASSWORD;
        }
    }

    /**
     * Download attached file.
     */
    @GetMapping("/file/{id:[a-f0-9]{64}}/{key:[a-f0-9]{64}}")
    public ResponseEntity<StreamingResponseBody> file(@PathVariable("id") final String id,
                                                      @PathVariable("key") final String keyHex,
                                                      final HttpSession session) {

        final KeyIv keyIv =
            new KeyIv(BaseEncoding.base16().lowerCase().decode(keyHex), resolveFileIv(id, session));

        final DecryptedFile decryptedFile = messageService.resolveStoredFile(id, keyIv);

        final HttpHeaders headers = new HttpHeaders();

        // Set application/octet-stream instead of the original mime type to force download
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        if (decryptedFile.getName() != null) {
            headers.setContentDispositionFormData("attachment", decryptedFile.getName(),
                StandardCharsets.UTF_8);
        }
        headers.setContentLength(decryptedFile.getOriginalFileSize());

        final StreamingResponseBody body = out -> {
            try (final InputStream in = messageService.getStoredFileInputStream(id, keyIv)) {
                ByteStreams.copy(in, out);
                out.flush();
            }

            messageService.burnFile(id);
        };

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private byte[] resolveFileIv(final String id, final HttpSession session) {
        final String idInSession = buildSessionAttr(id);
        final byte[] iv = (byte[]) session.getAttribute(idInSession);
        if (iv == null) {
            throw new MessageNotFoundException();
        }
        session.removeAttribute(idInSession);
        return iv;
    }

}
