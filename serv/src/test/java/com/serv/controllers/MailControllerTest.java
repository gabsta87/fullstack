package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.controller.MailController;
import com.serv.database.entities.Email;
import com.serv.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MailController.class)
class MailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MailService mailService;

    @Test
    void testSendEmail() throws Exception {
        Email email = new Email("test@venus.com");

        doNothing().when(mailService).sendSimpleMessage(any(Email.class), anyString(), anyString());

        // Note : Si le contrôleur utilise plusieurs @RequestBody, s'assurer qu'il gère bien le payload global
        // ou adapter l'appel selon la structure exacte reçue par la route /sendMail.
        mockMvc.perform(post("/sendMail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(email)))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}