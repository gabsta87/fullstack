package com.serv.services;

import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PasswordResetTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// TODO Handle failed deliveries of emails (no exception thrown sadly). See https://www.baeldung.com/spring-email#handling-send-errors

@Slf4j
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
            log.error("Could not send email \"{}\": {}", subject, e.getMessage());
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
            log.error("Could not build email \"{}\": {}", subject, e.getMessage());
        } catch (MailException e) {
            log.error("Could not send email \"{}\" (SMTP): {}", subject, e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        passwordResetTokenRepository.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }

    public void sendAlertToLocalAuthorities(Worker worker) {
        // Not implemented yet (see ROADMAP): only logged so that admins can act manually.
        log.warn("SECURITY minor declared by worker {}: profile deactivated, authorities alert NOT sent (not implemented)", worker.getId());
    }

}
