package com.serv.controller;

import com.serv.database.Email;
import com.serv.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MailController {

    private final MailService mailManager;

    @Autowired
    public MailController(MailService mailManager) {
        this.mailManager = mailManager;
    }

    @PostMapping("/sendMail")
    public ResponseEntity<String> sendEmail(@RequestBody Email to, @RequestBody String subject, @RequestBody String content) {
        mailManager.sendSimpleMessage(to,subject,content);
        return ResponseEntity.ok().body("OK");
    }

//    @GetMapping("/forgotPassword")
//    public String forgotPassword(){
//        return "security/forgotPassword";
//    }
}
