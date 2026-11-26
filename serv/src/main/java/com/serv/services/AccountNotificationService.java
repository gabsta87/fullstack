package com.serv.services;

import com.serv.database.entities.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Security notices sent when account credentials change, so that the owner learns about a change they did
 * not make: password changed (with a reset link), email changed (sent to the previous address, which can
 * no longer receive reset links). Sent after the commit: a cancelled change sends nothing.
 */
@Service
@RequiredArgsConstructor
public class AccountNotificationService {

    private final EmailTemplates emailTemplates;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.support-email:support@venus-app.com}")
    private String supportEmail;

    public void passwordChanged(Email to) {
        send(to, emailTemplates.render(EmailTemplates.Kind.PASSWORD_CHANGED, frontendUrl + "/forgot-password"));
    }

    /** @param previousAddress the address the account had before the change */
    public void emailChanged(Email previousAddress) {
        send(previousAddress, emailTemplates.render(EmailTemplates.Kind.EMAIL_CHANGED, "mailto:" + supportEmail));
    }

    private void send(Email to, EmailTemplates.RenderedEmail email) {
        Runnable send = () -> mailService.sendHtmlMessage(to, email.subject(), email.html());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }
}
