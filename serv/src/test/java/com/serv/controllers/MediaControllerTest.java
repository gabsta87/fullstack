package com.serv.controllers;

import com.serv.controller.MediaController;
import com.serv.database.entities.Photo;
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

import java.util.Collections;
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
        // worker.setSubscriptionTier(SubscriptionTier.BASIC); // Si tu as un enum pour les abonnements

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(worker));
        when(videoRepository.findByWorkerId(workerId)).thenReturn(Collections.emptyList());
        when(photoRepository.findByWorker(any(Worker.class))).thenReturn(Collections.emptyList());

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "profile.jpg",
                "image/jpeg",
                "fake-image-bytes-content".getBytes()
        );

        when(storageService.savePhoto(any(), any())).thenAnswer(inv -> {
            Photo photo = new Photo();
            return photo;
        });

        mockMvc.perform(multipart("/media/" + workerId + "/media").file(file))
                .andExpect(status().isOk());
    }
}