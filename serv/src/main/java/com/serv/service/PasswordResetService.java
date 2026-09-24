package com.serv.service;

import com.serv.database.entities.PasswordResetToken;
import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public void createTokenAndSendEmail(VenusUser user, boolean isInvitation) {
        // 1. Nettoyage des anciens tokens
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.flush();

        // 2. Génération du token
        UUID token = UUID.randomUUID();
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(myToken);

        // 3. Construction de l'URL et du contenu du mail
        String resetUrl = String.format("%s/reset-password?resetToken=%s", frontendUrl, token);

        // On adapte le texte selon qu'il s'agit d'une invitation ou d'un oubli de mot de passe
        String subject = isInvitation ? "Activation de votre compte Administrateur" : "Réinitialisation de votre mot de passe";
        String introContext = isInvitation
                ? "Un compte administrateur a été créé pour vous sur Venus. Veuillez cliquer sur le lien ci-dessous pour configurer votre mot de passe et activer votre compte (valable 24h) :"
                : "Vous avez demandé la réinitialisation de votre mot de passe. Veuillez cliquer sur le lien ci-dessous pour configurer un nouveau mot de passe (valable 24h) :";

        String htmlContent = "<html><body>"
                + "<p>Bonjour,</p>"
                + "<p>" + introContext + "</p>"
                + "<p style='margin: 20px 0;'>"
                + "  <a href='" + resetUrl + "' style='background-color: #3880ff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block;'>"
                + "    Configurer mon mot de passe"
                + "  </a>"
                + "</p>"
                + "<p>Si le bouton ne fonctionne pas, vous pouvez copier-coller ce lien dans votre navigateur :<br>"
                + "<a href='" + resetUrl + "'>" + resetUrl + "</a></p>"
                + "<p>Cordialement,<br>L'équipe Venus</p>"
                + "</body></html>";

        System.out.println("Link for email "+user.getEmail()+" to reset password : " + resetUrl);

        // 4. Envoi
        mailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
    }
}