package com.serv.service;

import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PasswordResetTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;

// TODO Handle failed deliveries of emails (no exception thrown sadly). See https://www.baeldung.com/spring-email#handling-send-errors

@Service
@RequiredArgsConstructor
public class MailService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender javaMailSender;
    @Value("${app.mail.sender:no-reply@mondomaine.com}")
    private String messageSender;

    public void sendSimpleMessage(Email to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(messageSender);
            message.setTo(to.toString());
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.send(message);
        } catch (Exception e) {
            // Log propre de l'erreur d'envoi
            System.err.println("Erreur lors de l'envoi du mail simple à " + to + " : " + e.getMessage());
        }
    }

    public void sendHtmlMessage(Email to, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(messageSender);
            helper.setTo(to.toString());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Erreur de format ou d'envoi du mail HTML à " + to + " : " + e.getMessage());
        } catch (MailException e) {
            System.err.println("Erreur SMTP/Réseau lors de l'envoi à " + to + " : " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        passwordResetTokenRepository.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }

    public void sendAlertToLocalAuthorities(Worker worker) {
        // TODO: Implémenter l'envoi aux autorités si besoin
        System.out.println("Alert sent to local authorities for worker " + worker.getId());
    }

}
