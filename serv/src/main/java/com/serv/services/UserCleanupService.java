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
        // 1. Nettoyage spécifique selon le type d'utilisateur
        if (user instanceof Client client) {
            // Vider les tables de jointure ManyToMany / OneToMany pour éviter les contraintes FK
            client.getFavorites().clear();
            client.getPreferredServices().clear();
            client.getPreferredLanguages().clear();
            userRepository.save(client); // Met à jour les tables de jointure
        }
        else if (user instanceof Worker worker) {
            // Supprimer les fichiers physiques du disque via le dossier unique de l'user
            try {
                storageService.deleteAllForUser(worker.getId());
            } catch (IOException e) {
                log.error("Could not delete the files of worker {}: account kept", worker.getId(), e);
                return;
            }

            // Vider les collections (la cascade ALL + orphanRemoval fera le reste en base)
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

        // 2. Nettoyage commun pour tous les types d'utilisateur
        adminAuditLogRepository.nullifyUserTarget(user.getId());
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        // 3. Suppression globale de l'utilisateur (gère VenusUser + tables filles automatiquement)
        userRepository.delete(user);
        log.info("Account {} ({}) deleted with all its data", user.getId(), user.getRole());
    }
}
