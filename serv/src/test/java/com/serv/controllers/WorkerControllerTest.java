package com.serv.controllers;

import com.serv.database.repositories.WorkerRepository;
import com.serv.services.WorkerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerController.class)
class WorkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerRepository workerRepository;

    @MockBean
    private WorkerService galleryService;

    @Test
    void testGetGallery() throws Exception {
        when(galleryService.getGalleryPage(anyInt(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/workers")
                        .param("zoneId", "1")
                        .param("gender", "female"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetProfileNotFound() throws Exception {
        UUID workerId = UUID.randomUUID();
        when(workerRepository.findByIdWithPhotos(workerId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/workers/" + workerId))
                .andExpect(status().isNotFound());
    }
}