package com.serv.controller;

import com.serv.database.entities.Email;
import com.serv.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class MailController {

    private final MailService mailManager;

    @PostMapping("/sendMail")
    public ResponseEntity<String> sendEmail(@RequestBody Email to, @RequestBody String subject, @RequestBody String content) {
        mailManager.sendSimpleMessage(to,subject,content);
        return ResponseEntity.ok().body("OK");
    }

}
