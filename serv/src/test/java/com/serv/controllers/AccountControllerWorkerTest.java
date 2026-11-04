package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.common.Language;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.*;
import com.serv.dto.SpokenLanguageDTO;
import com.serv.requests.UpdateWorkerLanguage;
import com.serv.service.MailService;
import com.serv.service.MediaStorageService;
import com.serv.service.SseStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountControllerWorker.class)
@AutoConfigureMockMvc
class AccountControllerWorkerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── MOCKS DES DÉPENDANCES DU CONTRÔLEUR WORKER ──────────────────────────
    @MockBean private WorkerRepository workerRepository;
    @MockBean private PhotoRepository photoRepository;
    @MockBean private VideoRepository videoRepository;
    @MockBean private ServiceRepository serviceRepository;
    @MockBean private GeographicZoneRepository geographicZoneRepository;
    @MockBean private CertificationRequestRepository certificationRequestRepository;
    @MockBean private MediaStorageService mediaStorageService;
    @MockBean private SseStreamService sseStreamService;
    @MockBean private MailService emailService;

    private Worker mockWorker;
    private UUID workerId;

    @BeforeEach
    void setUp() {
        workerId = UUID.randomUUID();

        mockWorker = new Worker();
        mockWorker.setId(workerId);
        mockWorker.setUsername("TestWorker");
        mockWorker.setEmail(new Email("worker@venus.com"));
    }

    // ── 1. TEST DE RÉCUPÉRATION DU PROFIL PROPRE (/account/worker/me) ───────

    @Test
    @WithMockUser(roles = "WORKER")
    @DisplayName("GET /account/worker/me - Doit retourner le profil complet du worker connecté")
    void getMe_Success() throws Exception {
        when(workerRepository.findById(workerId)).thenReturn(Optional.of(mockWorker));

        // Note : Si ton argument resolver injecte l'objet Worker via le contexte de sécurité,
        // assure-toi que ton principal / contexte simule bien un ID valide, ou ajuste selon ton setup d'auth.
        when(workerRepository.findByIdWithPhotos(workerId)).thenReturn(Optional.of(mockWorker));

        mockMvc.perform(get("/account/worker/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerId.toString()))
                .andExpect(jsonPath("$.username").value("TestWorker"));
    }

    // ── 2. TEST DE LA GESTION DES LANGUES PARLÉES ──────────────────────────

    @Test
    @WithMockUser(roles = "WORKER")
    @DisplayName("POST /account/worker/spoken-languages - Met à jour les langues parlées du worker")
    void updateSpokenLanguages_Success() throws Exception {
        UpdateWorkerLanguage languageRequest = new UpdateWorkerLanguage(
                List.of(new SpokenLanguageDTO(Language.FR, 4), new SpokenLanguageDTO(Language.EN, 3))
        );

        when(workerRepository.findById(any(UUID.class))).thenReturn(Optional.of(mockWorker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/account/worker/spoken-languages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(languageRequest)))
                .andExpect(status().isOk());

        verify(workerRepository, times(1)).save(any(Worker.class));
    }

    // ── 3. TEST DE VISIBILITÉ DU PROFIL ────────────────────────────────────

    @Test
    @WithMockUser(roles = "WORKER")
    @DisplayName("PATCH /account/worker/visibility - Modifie l'état de visibilité (hidden)")
    void setVisibility_Success() throws Exception {
        var body = java.util.Map.of("hidden", true);

        when(workerRepository.findByIdWithPhotos(any())).thenReturn(Optional.of(mockWorker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/account/worker/visibility")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(true));

        verify(workerRepository, times(1)).save(any(Worker.class));
    }

    // 🛡️ CAS LIMITE : Tentative de supprimer une photo inexistante ou appartenant à un autre (404 Not Found)
    @Test
    @WithMockUser(roles = "WORKER")
    @DisplayName("DELETE /account/worker/photos/{photoId} - Retourne 404 si la photo n'existe pas")
    void deletePhoto_NotFound() throws Exception {
        UUID fakePhotoId = UUID.randomUUID();
        when(workerRepository.findByIdWithPhotos(any())).thenReturn(Optional.of(mockWorker));
        when(photoRepository.findById(fakePhotoId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/account/worker/photos/" + fakePhotoId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // 🛡️ CAS LIMITE : Tentative de modification des langues avec un worker introuvable (404 Not Found)
    @Test
    @WithMockUser(roles = "WORKER")
    @DisplayName("POST /account/worker/spoken-languages - Retourne 404 si le worker n'est pas trouvé")
    void updateSpokenLanguages_WorkerNotFound() throws Exception {
        UpdateWorkerLanguage languageRequest = new UpdateWorkerLanguage(
                List.of(new SpokenLanguageDTO(Language.FR, 4))
        );

        when(workerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/account/worker/spoken-languages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(languageRequest)))
                .andExpect(status().isNotFound());
    }
}