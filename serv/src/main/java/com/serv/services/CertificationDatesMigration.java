package com.serv.services;

import com.serv.common.SettingKey;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-off, idempotent migration run at startup: certifications approved before end dates were stored
 * get the end date of the rule they were approved under (8 months), and the current renewal period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationDatesMigration implements ApplicationRunner {

    /** Validity of every certification approved before the setting existed. */
    static final int LEGACY_VALIDITY_MONTHS = 8;

    private final WorkerRepository workerRepository;
    private final SettingsService settingsService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Worker> legacy = workerRepository.findByCertifiedAtIsNotNullAndCertificationExpiresAtIsNull();
        if (legacy.isEmpty()) return;
        int renewal = Math.min(settingsService.get(SettingKey.CERTIFICATION_RENEWAL_MONTHS), LEGACY_VALIDITY_MONTHS - 1);
        for (Worker w : legacy) {
            w.certify(w.getCertifiedAt(), LEGACY_VALIDITY_MONTHS, renewal);
        }
        workerRepository.saveAll(legacy);
        log.info("Stored the end date of {} certification(s) approved before end dates were kept", legacy.size());
    }
}
