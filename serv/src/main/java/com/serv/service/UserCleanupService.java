package com.serv.service;

import com.serv.database.entities.Client;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.AdminAuditLogRepository;
import com.serv.database.repositories.CertificationRequestRepository;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@RequiredArgsConstructor
@Service
public class UserCleanupService {

    private final MediaStorageService storageService;
    private final CertificationRequestRepository certificationRequestRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
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
                System.err.println("Erreur lors de la suppression des fichiers physiques pour le worker " + worker.getId() + ": " + e.getMessage());
                return;
            }
            // Vider les collections (la cascade ALL + orphanRemoval fera le reste en base)
            worker.getPhotos().clear();
            worker.getVideos().clear();
            worker.getComments().clear();
            worker.getServices().clear();
            userRepository.save(worker);
            certificationRequestRepository.deleteByWorkerId(worker.getId());
        }

        // 2. Nettoyage commun pour tous les types d'utilisateur
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        // 3. Suppression globale de l'utilisateur (gère VenusUser + tables filles automatiquement)
        userRepository.delete(user);
    }
}
