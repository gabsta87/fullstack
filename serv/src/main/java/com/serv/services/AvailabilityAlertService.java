package com.serv.services;

import com.serv.common.InputLimits;
import com.serv.database.entities.AvailabilityAlert;
import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.AvailabilityAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

/** "Notify me when back" alerts: visitors wait for an unavailable profile; one email at its next return. */
@Slf4j
@Service
public class AvailabilityAlertService {

    private final AvailabilityAlertRepository alertRepository;
    private final EmailTemplates emailTemplates;
    private final MailService mailService;
    /** Emails go out in the background: a worker coming back must not wait for dozens of SMTP exchanges. */
    private final Executor mailExecutor;

    public AvailabilityAlertService(AvailabilityAlertRepository alertRepository, EmailTemplates emailTemplates,
                                    MailService mailService, @Qualifier("applicationTaskExecutor") Executor mailExecutor) {
        this.alertRepository = alertRepository;
        this.emailTemplates = emailTemplates;
        this.mailService = mailService;
        this.mailExecutor = mailExecutor;
    }

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public List<UUID> watchedWorkerIds(Client client) {
        return alertRepository.findWorkerIdsByClientId(client.getId());
    }

    /** Idempotent. Only for a public profile that is currently unavailable. */
    @Transactional
    public void subscribe(Client client, Worker worker) {
        if (worker.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This profile is already available.");
        }
        if (alertRepository.findByClientIdAndWorkerId(client.getId(), worker.getId()).isPresent()) {
            return;
        }
        if (alertRepository.countByClientId(client.getId()) >= InputLimits.AVAILABILITY_ALERTS_MAX) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Too many alerts.");
        }
        alertRepository.save(new AvailabilityAlert(client, worker, LocaleContextHolder.getLocale().toLanguageTag()));
    }

    /** Idempotent. */
    @Transactional
    public void unsubscribe(Client client, UUID workerId) {
        alertRepository.findByClientIdAndWorkerId(client.getId(), workerId).ifPresent(alertRepository::delete);
    }

    /**
     * The worker just became available: the alerts are deleted in the current transaction and the emails
     * sent once it commits (a failed email never cancels the worker's change).
     */
    @Transactional
    public void onBackOnline(Worker worker) {
        List<AvailabilityAlert> alerts = alertRepository.findAllWithClientByWorkerId(worker.getId());
        if (alerts.isEmpty()) return;

        String profileUrl = frontendUrl + "/profile?id=" + worker.getId();
        String name = worker.getUsername() != null ? worker.getUsername() : "";
        List<Pending> pending = alerts.stream()
                .map(a -> new Pending(a.getClient().getEmail(), Locale.forLanguageTag(a.getLanguage())))
                .toList();
        alertRepository.deleteAll(alerts);

        Runnable send = () -> mailExecutor.execute(() -> pending.forEach(p -> {
            EmailTemplates.RenderedEmail email = emailTemplates.render(EmailTemplates.Kind.BACK_ONLINE, profileUrl, p.locale(), name);
            mailService.sendHtmlMessage(p.to(), email.subject(), email.html());
        }));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
        log.info("Back-online alerts for worker {}: {} email(s) queued", worker.getId(), pending.size());
    }

    private record Pending(Email to, Locale locale) {}
}
