package com.serv.services;

import com.serv.database.entities.Client;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserCleanupService {

    private final MediaStorageService storageService;
    private final CertificationRequestRepository certificationRequestRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteUserCompletely(VenusUser user) {
        // 1. Cleanup specific to the user type
        if (user instanceof Client client) {
            // Clear the join tables (ManyToMany / OneToMany) to avoid FK violations
            client.getFavorites().clear();
            client.getPreferredServices().clear();
            client.getPreferredLanguages().clear();
            userRepository.save(client); // Updates the join tables
        }
        else if (user instanceof Worker worker) {
            // Delete the user's media directory from disk
            try {
                storageService.deleteAllForUser(worker.getId());
            } catch (IOException e) {
                log.error("Could not delete the files of worker {}: account kept", worker.getId(), e);
                return;
            }

            // Clear the collections (cascade ALL + orphanRemoval removes the rows)
            worker.setMainPhoto(null);
            worker.setLastCertificationRequest(null);
            userRepository.save(worker);

            clientRepository.removeWorkerFromAllFavorites(worker.getId());

            worker.getPhotos().clear();
            worker.getVideos().clear();
            worker.getComments().clear();
            worker.getServices().clear();
            userRepository.save(worker);
            certificationRequestRepository.deleteByWorkerId(worker.getId());
        }

        // 2. Cleanup common to every user type
        adminAuditLogRepository.nullifyUserTarget(user.getId());
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        // 3. Delete the user (VenusUser and subclass tables)
        userRepository.delete(user);
        log.info("Account {} ({}) deleted with all its data", user.getId(), user.getRole());
    }
}
