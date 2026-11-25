package com.serv.services;

import com.serv.database.entities.Email;
import com.serv.database.repositories.PasswordResetTokenRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private MailService mailService;

    @org.junit.jupiter.api.BeforeEach
    void setSender() {
        org.springframework.test.util.ReflectionTestUtils.setField(mailService, "messageSender", "no-reply@venus.test");
    }

    @Test
    @DisplayName("sendSimpleMessage - sends a plain email through JavaMailSender")
    void sendSimpleMessage_Success() {
        Email to = new Email("recipient@venus.com");

        mailService.sendSimpleMessage(to, "Sujet Test", "Contenu Test");

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendHtmlMessage - sends an HTML email through JavaMailSender")
    void sendHtmlMessage_Success() {
        Email to = new Email("recipient@venus.com");
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);

        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendHtmlMessage(to, "Sujet HTML", "<p>Contenu HTML</p>");

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}