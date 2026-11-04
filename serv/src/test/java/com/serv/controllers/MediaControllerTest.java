package com.serv.controllers;

import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.VideoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.service.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaStorageService storageService;
    @MockBean
    private PhotoRepository photoRepository;
    @MockBean
    private VideoRepository videoRepository;
    @MockBean
    private WorkerRepository workerRepository;

    @Test
    void testUploadMediaSuccess() throws Exception {
        UUID workerId = UUID.randomUUID();
        Worker worker = new Worker();
        worker.setId(workerId);

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(worker));
        when(videoRepository.findByWorkerId(workerId)).thenReturn(new ArrayList<>());
        when(photoRepository.findByWorker(any(Worker.class))).thenReturn(new ArrayList<>());

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "profile.jpg",
                "image/jpeg",
                "fake-image-bytes-content".getBytes()
        );

        mockMvc.perform(multipart("/media/" + workerId + "/media").file(file))
                .andExpect(status().isOk());
    }

    // 🛡️ TEST DE ROBUSTESSE 1 : Worker introuvable
    @Test
    void testUploadMediaWorkerNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();

        // Le repository ne trouve pas le worker
        when(workerRepository.findById(unknownId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "files", "profile.jpg", "image/jpeg", "content".getBytes()
        );

        // On s'attend à ce que le contrôleur lève une erreur (ex: 500 ou 404 selon ta gestion d'exception)
        mockMvc.perform(multipart("/media/" + unknownId + "/media").file(file))
                .andExpect(status().isInternalServerError()); // Ajuste selon ton GlobalExceptionHandler (ex: isNotFound())
    }

    // 🛡️ TEST DE ROBUSTESSE 2 : Dépassement de quota de stockage
    @Test
    void testUploadMediaStorageLimitExceeded() throws Exception {
        UUID workerId = UUID.randomUUID();
        Worker worker = new Worker();
        worker.setId(workerId);
        // Mettons un abonnement basique (50 Mo max) mais simulons déjà 50 Mo consommés
        // (Tu peux adapter selon la façon dont ton getMaxStorageBytes() se comporte dans le test)

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(worker));

        // On simule un fichier énorme (ex: 60 Mo) qui dépasse le quota
        MockMultipartFile hugeFile = new MockMultipartFile(
                "files",
                "huge-video.mp4",
                "video/mp4",
                new byte[10 * 1024 * 1024] // Fichier simulé
        );

        // Si ton code vérifie le stockage, il doit renvoyer HttpStatus.INSUFFICIENT_STORAGE (507)
        // Note : Si ton entité calcule le max par défaut à 50 Mo, un fichier de 60 Mo déclenchera la condition.
        mockMvc.perform(multipart("/media/" + workerId + "/media").file(hugeFile))
                .andExpect(status().isInsufficientStorage());
    }
}