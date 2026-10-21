package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.controller.AdminController;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.requests.admin.UpdateService;
import com.serv.requests.admin.UpdateWorkerDays;
import com.serv.service.PasswordResetService;
import com.serv.service.SseStreamService;
import com.serv.service.UserCleanupService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── MOCKS DES DÉPENDANCES DU CONTRÔLEUR ─────────────────────────────────
    @MockBean private WorkerRepository workerRepository;
    @MockBean private UserRepository userRepository;
    @MockBean private AdminRepository adminRepository;
    @MockBean private AdminAuditLogRepository auditLogRepository;
    @MockBean private GeographicZoneRepository geographicZoneRepository;
    @MockBean private ServiceRepository serviceRepository;
    @MockBean private LegalTextRepository legalTextRepository;
    @MockBean private SseStreamService sseStreamService;
    @MockBean private PasswordResetService passwordResetService;
    @MockBean private CommentRepository commentRepository;
    @MockBean private GeographicZoneRepository zoneRepository;
    @MockBean private CertificationRequestRepository certificationRequestRepository;
    @MockBean private UserCleanupService userCleanupService;
    @MockBean private ReportRepository reportRepository;

    private Admin mockAdmin;
    private Worker mockWorker;
    private UUID workerId;

    @BeforeEach
    void setUp() {
        workerId = UUID.randomUUID();

        mockAdmin = new Admin();
        mockAdmin.setId(UUID.randomUUID());
        mockAdmin.setEmail(new Email("admin@venus.com"));

        mockWorker = new Worker();
        mockWorker.setId(workerId);
        mockWorker.setEmail(new Email("worker@venus.com"));
        mockWorker.setRemainingDaysCredit(10);
    }

    // ── 1. TEST DE SÉCURITÉ & SÉLECTION DES RÔLES ────────────────────────────

    @Test
    @DisplayName("GET /admin/users sans authentification doit retourner 401 ou 403")
    void getAllUsers_Unauthorized() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized()); // ou isForbidden() selon ta config Spring Security
    }

    // ── 2. TESTS DES ENDPOINTS LECTURE (GET) ────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/services - Doit retourner la liste des services")
    void getServices_Success() throws Exception {
        Service service = new Service();
        service.setName("Massage");
        when(serviceRepository.findAll()).thenReturn(List.of(service));

        mockMvc.perform(get("/admin/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Massage"));

        verify(serviceRepository, times(1)).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/workers/{id} - Cas succès")
    void getWorkerProfileForAdmin_Success() throws Exception {
        when(workerRepository.findByIdWithPhotos(workerId)).thenReturn(Optional.of(mockWorker));

        mockMvc.perform(get("/admin/workers/{workerId}", workerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/workers/{id} - Cas Worker non trouvé (404)")
    void getWorkerProfileForAdmin_NotFound() throws Exception {
        when(workerRepository.findByIdWithPhotos(workerId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/workers/{workerId}", workerId))
                .andExpect(status().isNotFound());
    }

    // ── 3. TESTS DES ACTIONS & MODIFICATIONS (POST / DELETE) ─────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/profiles/update-days - Succès de la modification du crédit de jours")
    void updateDays_Success() throws Exception {
        UpdateWorkerDays dto = new UpdateWorkerDays(workerId.toString(), 30, "Achat crédit promotionnel");

        when(workerRepository.findById(workerId)).thenReturn(Optional.of(mockWorker));
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/admin/profiles/update-days")
                        .with(csrf()) // Gestion CSRF Spring Security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingDaysCredit").value(30));

        // Vérification de la persistance et du log d'audit
        verify(workerRepository, times(1)).save(mockWorker);
        verify(auditLogRepository, times(1)).save(any(AdminAuditLog.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /admin/users/{id} - Un admin ne peut pas supprimer son propre compte")
    void deleteUser_SelfDeletion_Forbidden() throws Exception {
        // Simule que l'utilisateur cible est le même que l'admin connecté
        when(userRepository.findById(mockAdmin.getId())).thenReturn(Optional.of(mockAdmin));

        mockMvc.perform(delete("/admin/users/{id}", mockAdmin.getId())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vous ne pouvez pas supprimer votre propre compte administrateur."));

        verify(userCleanupService, never()).deleteUserCompletely(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/service - Création d'un nouveau service (cas sans conflit)")
    void createService_Success() throws Exception {
        UpdateService dto = new UpdateService(null, "Nouveau Service", "Description du service");

        when(serviceRepository.findByName("Nouveau Service")).thenReturn(Optional.empty());

        mockMvc.perform(post("/admin/service")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(serviceRepository, times(1)).save(any(Service.class));
        verify(sseStreamService, times(1)).broadcastAdminEvent(eq("SERVICES_UPDATED"), any());
    }
}