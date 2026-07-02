package com.serv.service;

import com.serv.database.entities.Worker;
import com.serv.database.repositories.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SubscriptionScheduler {

    @Autowired
    private WorkerRepository workerRepository;

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);

    /**
     * S'exécute tous les jours à minuit pile (00:00:00)
     * Zone de temps : Europe/Paris (à adapter selon ta cible)
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Paris")
    @Transactional
    public void processDailySubscriptionDeduction() {
        // 1. On cherche tous ceux qui ont été actifs aujourd'hui OU qui le sont encore à minuit
        List<Worker> workersToCharge = workerRepository.findByHasBeenActiveTodayTrueOrIsAvailableTrue();

        for (Worker worker : workersToCharge) {
            if (worker.getRemainingDaysCredit() > 0) {
                // On décompte le jour consommé
                worker.setRemainingDaysCredit(worker.getRemainingDaysCredit() - 1);
            }

            // Si le crédit tombe à 0, coupure immédiate
            if (worker.getRemainingDaysCredit() <= 0) {
                worker.setExpired(true);
                worker.setRemainingDaysCredit(0);
            }

            // 🎯 RÈGLE CLÉ : Si le profil finit la journée EN LIGNE, il reste valide d'office pour demain
            // Sinon s'il était hors-ligne à minuit, il devra re-déclencher la bascule demain.
            worker.setHasBeenActiveToday(worker.isAvailable());
        }

        workerRepository.saveAll(workersToCharge);
    }
}