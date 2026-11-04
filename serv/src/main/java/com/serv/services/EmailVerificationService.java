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
     * @return l'URL de vérification à envoyer par e-mail
     */
    @Transactional
    public String createTokenAndSendEmail(VenusUser user) {
        // 1. Nettoyage des anciens tokens éventuels pour cet utilisateur
        verificationTokenRepository.deleteAllByUserId(user.getId());
        verificationTokenRepository.flush();

        // 2. Génération du token d'activation
        VerificationToken myToken = new VerificationToken(user); // Utilise le constructeur de ton entité
        verificationTokenRepository.save(myToken);

        // 3. Construction de l'URL et du contenu du mail
        // Note bien la route front-end différente : /verify-email au lieu de /reset-password
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

        System.out.println("Verification URL for email " + user.getEmail() + " : " + verificationUrl);

        // 4. Envoi du mail
        mailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
        return verificationUrl;
    }

    /**
     * Purge automatique des tokens d'activation expirés tous les jours à 3h du matin.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredVerificationTokens() {
        // Attention à utiliser le type de date correspondant à ton entité (Instant ou LocalDateTime)
        verificationTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}