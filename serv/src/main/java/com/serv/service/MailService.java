package com.serv.service;

import com.serv.database.entities.Email;
import com.serv.database.repositories.PasswordResetTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    private final String messageSender = "no-reply-venus@gmail.com";

    public void sendFormattedMessage(Email to, String subject, String content){
        String text = String.format(Objects.requireNonNull(templateSimpleMessage().getText()), content);
        sendSimpleMessage(to, subject, text);
    }

    public void sendSimpleMessage(Email to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(messageSender);
        message.setTo(to.toString());
        message.setSubject(subject);
        message.setText(content);
        getJavaMailSender().send(message);
    }

    public void sendHtmlMessage(Email to, String subject, String htmlBody) {
        MimeMessage message = getJavaMailSender().createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to.toString());
            helper.setSubject(subject);

            helper.setText(htmlBody, true);

            getJavaMailSender().send(message);
        } catch (MessagingException e) {
            // Gérer l'exception (log)
        }
    }

    private JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    public SimpleMailMessage templateSimpleMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setText("Voici votre lien de réinitialisation de mot de passe :\n%s\n");
        return message;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        passwordResetTokenRepository.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }
}
