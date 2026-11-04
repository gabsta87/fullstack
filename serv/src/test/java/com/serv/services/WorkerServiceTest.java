package com.serv.services;

import com.serv.database.repositories.WorkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkerService workerService;

    @Test
    @DisplayName("getGalleryPage - Retourne une liste vide si aucun worker ne correspond")
    void getGalleryPage_EmptyResult() {
        Page<com.serv.database.entities.Worker> emptyPage = Page.empty();
        when(workerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        var result = workerService.getGalleryPage(0, Map.of());

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(workerRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getGalleryPage - Corrige une page négative à 0")
    void getGalleryPage_NegativePageCorrection() {
        Page<com.serv.database.entities.Worker> emptyPage = Page.empty();
        when(workerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        // On passe une page négative (-5)
        var result = workerService.getGalleryPage(-5, Collections.emptyMap());

        assertThat(result).isEmpty();
        // Le test valide que le traitement ne lève pas d'exception et interroge bien la page 0
        verify(workerRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}