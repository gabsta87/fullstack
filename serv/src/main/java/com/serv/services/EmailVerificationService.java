package com.serv.services;

import com.serv.database.entities.VenusUser;
import com.serv.database.entities.VerificationToken;
import com.serv.database.repositories.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final EmailTemplates emailTemplates;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * @return the verification URL sent by email
     */
    @Transactional
    public String createTokenAndSendEmail(VenusUser user) {
        // 1. Remove any previous token of this user
        verificationTokenRepository.deleteAllByUserId(user.getId());
        verificationTokenRepository.flush();

        // 2. Generate the activation token
        VerificationToken myToken = new VerificationToken(user);
        verificationTokenRepository.save(myToken);

        // 3. Build the URL and the email content
        // Note the front-end route: /verify-email (not /reset-password)
        String verificationUrl = String.format("%s/verify-email?token=%s", frontendUrl, myToken.getToken());

        EmailTemplates.RenderedEmail email = emailTemplates.render(EmailTemplates.Kind.VERIFY_EMAIL, verificationUrl);

        // 4. Send
        mailService.sendHtmlMessage(user.getEmail(), email.subject(), email.html());
        return verificationUrl;
    }

    /**
     * Purges expired activation tokens every day at 3 am.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredVerificationTokens() {
        verificationTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}