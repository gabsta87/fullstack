package com.serv.services;

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
    private final EmailTemplates emailTemplates;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * @return the reset URL to be sent to the user's email
     */
    @Transactional
    public String createTokenAndSendEmail(VenusUser user, boolean isInvitation) {
        // 1. Remove previous tokens
        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.flush();

        // 2. Generate the token
        UUID token = UUID.randomUUID();
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(myToken);

        // 3. Build the URL and the email content
        String resetUrl = String.format("%s/reset-password?resetToken=%s", frontendUrl, token);

        EmailTemplates.RenderedEmail email = emailTemplates.render(isInvitation ? EmailTemplates.Kind.ADMIN_INVITATION : EmailTemplates.Kind.RESET_PASSWORD, resetUrl);

        // 4. Send
        mailService.sendHtmlMessage(user.getEmail(), email.subject(), email.html());
        return resetUrl;
    }
}