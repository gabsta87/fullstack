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

        String subject = "Validation de votre adresse e-mail";
        String introContext = "Bienvenue sur Venus ! Veuillez cliquer sur le lien ci-dessous pour valider votre adresse e-mail et activer votre compte (valable 24h) :";

        String htmlContent = "<html><body>"
                + "<p>Bonjour,</p>"
                + "<p>" + introContext + "</p>"
                + "<p style='margin: 20px 0;'>"
                + "  <a href='" + verificationUrl + "' style='background-color: #3880ff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block;'>"
                + "    Activer mon compte"
                + "  </a>"
                + "</p>"
                + "<p>Si le bouton ne fonctionne pas, vous pouvez copier-coller ce lien dans votre navigateur :<br>"
                + "<a href='" + verificationUrl + "'>" + verificationUrl + "</a></p>"
                + "<p>Cordialement,<br>L'équipe Venus</p>"
                + "</body></html>";

        // 4. Envoi du mail
        mailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
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