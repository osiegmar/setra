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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import de.siegmar.securetransfer.controller.dto.EncryptMessageCommand;
import de.siegmar.securetransfer.domain.SenderMessage;
import de.siegmar.securetransfer.service.MessageSenderService;

@Controller
@RequestMapping("/send")
public class SendController {

    private static final String FORM_SEND_MSG = "send/send_form";
    private static final String FORM_MSG_STATUS = "send/message_status";

    private final MessageSenderService messageService;

    @Autowired
    public SendController(final MessageSenderService messageService) {
        this.messageService = messageService;
    }

    @ModelAttribute
    public void initModel(final Model model) {
        model
            .addAttribute("message_max_length", EncryptMessageCommand.MESSAGE_MAX_LENGTH)
            .addAttribute("password_max_length", EncryptMessageCommand.PASSWORD_MAX_LENGTH)
            .addAttribute("max_expiration", EncryptMessageCommand.MAX_EXPIRATION);
    }

    /**
     * Display the send form.
     */
    @GetMapping
    public ModelAndView form() {
        return new ModelAndView(FORM_SEND_MSG, "command", new EncryptMessageCommand());
    }

    /**
     * Process the send form.
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("command") final EncryptMessageCommand cmd,
                         final Errors errors,
                         final RedirectAttributes redirectAttributes) {

        // Form submit without files contains one empty file!
        final List<MultipartFile> files = cmd.getFiles() == null ? null
            : cmd.getFiles().stream().filter(f -> !f.isEmpty()).collect(Collectors.toList());

        if (cmd.getMessage() == null && (files == null || files.isEmpty())) {
            errors.reject(null, "Neither message nor files submitted");
        }

        if (errors.hasErrors()) {
            return FORM_SEND_MSG;
        }

        final String senderId = messageService.storeMessage(cmd.getMessage(), files,
            cmd.getPassword(), Instant.now().plus(cmd.getExpirationDays(), ChronoUnit.DAYS));

        redirectAttributes.addFlashAttribute("message", cmd.getMessage());

        return "redirect:/send/" + senderId;
    }

    /**
     * Displays the sent message to the sender after sending.
     */
    @GetMapping("/{id:[a-f0-9]{64}}")
    public String created(@PathVariable("id") final String id,
                          final Model model,
                          final UriComponentsBuilder uriComponentsBuilder) {
        final SenderMessage senderMessage = messageService.getSenderMessage(id);

        final String receiveUrl = MvcUriComponentsBuilder
            .fromMappingName(uriComponentsBuilder, "RC#receive")
            .arg(0, senderMessage.getReceiverId())
            .build();

        model
            .addAttribute("receiveUrl", receiveUrl)
            .addAttribute("senderMessage", senderMessage);
        return FORM_MSG_STATUS;
    }

    /**
     * Handle burn request sent by the sender.
     */
    @DeleteMapping("/{id:[a-f0-9]{64}}")
    public String burn(@PathVariable("id") final String id,
                       final RedirectAttributes redirectAttributes) {

        final SenderMessage senderMessage = messageService.getSenderMessage(id);

        if (senderMessage.getReceived() != null) {
            redirectAttributes.addFlashAttribute("alreadyReceived", true);
        } else if (senderMessage.getBurned() != null) {
            redirectAttributes.addFlashAttribute("alreadyBurned", true);
        } else {
            messageService.burnSenderMessage(senderMessage);
            redirectAttributes.addFlashAttribute("messageBurned", true);
        }

        return "redirect:/send/" + id;
    }

}
