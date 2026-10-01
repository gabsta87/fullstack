package com.serv.database.listeners;

import com.serv.database.entities.CertificationRequest;
import com.serv.database.entities.Photo;
import com.serv.database.entities.Worker;
import com.serv.service.MediaStorageService; // Ton service qui gère la suppression des fichiers physiques
import jakarta.persistence.PreRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CertificationRequestEventListener {

    private static MediaStorageService mediaStorageService;

    @Autowired
    public void init(MediaStorageService fileStorageService) {
        CertificationRequestEventListener.mediaStorageService = fileStorageService;
    }

    @PreRemove
    public void preRemove(CertificationRequest request) {
        Photo photo = request.getCertificationPhoto();
        Worker w = request.getWorker();
        if (photo != null) {
            mediaStorageService.deletePhotoFiles(w.getId(),photo);
        }
    }
}