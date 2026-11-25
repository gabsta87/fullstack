package com.serv.services;

import com.serv.common.PhotoType;
import com.serv.database.entities.Photo;
import com.serv.database.repositories.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

/**
 * One-off, idempotent migration run at startup: certification photos stored by older versions in
 * the public media directory are moved to the private directory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationPhotoMigration implements ApplicationRunner {

    private final PhotoRepository photoRepository;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migrated = 0;
        for (Photo photo : photoRepository.findAll()) {
            if (photo.getType() != PhotoType.CERTIFICATION || photo.getWorker() == null) continue;
            try {
                Optional<String> newUrl = mediaStorageService.migrateLegacyCertificationPhoto(photo.getWorker().getId(), photo.getUrl());
                if (newUrl.isPresent()) {
                    photo.setUrl(newUrl.get());
                    photoRepository.save(photo);
                    migrated++;
                }
            } catch (IOException e) {
                log.error("Could not move certification photo {} to private storage", photo.getId(), e);
            }
        }
        if (migrated > 0) {
            log.info("Moved {} certification photo(s) to private storage", migrated);
        }
    }
}
