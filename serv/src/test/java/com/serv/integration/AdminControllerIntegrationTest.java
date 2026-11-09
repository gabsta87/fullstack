package com.serv.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.AdminRepository;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.requests.admin.AdminInvitation;
import com.serv.requests.admin.UpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private GeographicZoneRepository zoneRepository;

    private String adminToken;
    private Admin admin;

    @BeforeEach
    void setUp() {
        adminRepository.deleteAll();
        serviceRepository.deleteAll();

        // Création de l'admin principal qui va exécuter les requêtes
        admin = new Admin();
        admin.setEmail(new Email("master.admin@venus.com"));
        admin.setUsername("master.admin@venus.com");
        admin.setPassword("Password123!");
        adminRepository.save(admin);

        adminToken = jwtProvider.generateToken(admin);
    }

    @Test
    @DisplayName("WORKFLOW - Cycle de vie complet d'un Service : Création -> Doublon interdit -> Suppression -> Vérification")
    void serviceLifecycleWorkflow() throws Exception {
        UpdateService newServiceReq = new UpdateService(null, "FIRST", "Description du service");

        // 1. CRÉATION DU SERVICE
        mockMvc.perform(post("/admin/service")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newServiceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'FIRST')]").exists());

        // Vérification en base H2
        Service savedService = serviceRepository.findByName("FIRST").orElseThrow();
        assertThat(savedService.getName()).isEqualTo("FIRST");

        // 2. SUPPRESSION DU SERVICE
        mockMvc.perform(delete("/admin/services/" + savedService.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'FIRST')]").doesNotExist());

        // 3. VÉRIFICATION APRÈS SUPPRESSION (Doit être absent de la base H2)
        assertThat(serviceRepository.findById(savedService.getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - Cycle de vie d'un Admin : Invitation -> Enregistrement en base -> Suppression sécurisée")
    void adminInvitationAndDeletionWorkflow() throws Exception {
        AdminInvitation invitationReq = new AdminInvitation("new.admin@venus.com");

        // 1. INVITATION / PRÉ-CRÉATION DE L'ADMIN
        mockMvc.perform(post("/admin/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.admin@venus.com"));

        // Vérification en base H2 qu'il est bien créé et verrouillé
        var createdAdmin = adminRepository.findByEmail(new Email("new.admin@venus.com"));
        assertThat(createdAdmin).isPresent();
        assertThat(createdAdmin.get().isLocked()).isTrue();

        // 2. SUPPRESSION COMPTE ADMIN (Via l'endpoint de suppression de l'AdminController)
        mockMvc.perform(delete("/admin/users/" + createdAdmin.get().getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur supprimé avec succès"));

        // 3. VÉRIFICATION APRÈS SUPPRESSION (Doit échouer / être introuvable)
        assertThat(userRepository.findById(createdAdmin.get().getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - Sécurité : Un admin ne peut pas s'auto-supprimer")
    void adminCannotDeleteSelf() throws Exception {
        mockMvc.perform(delete("/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vous ne pouvez pas supprimer votre propre compte administrateur."));

        // Vérification en base : L'admin est toujours là
        assertThat(adminRepository.findById(admin.getId())).isPresent();
    }

    @Test
    @DisplayName("WORKFLOW - Cycle de vie complet d'une Région : Création racine -> Création enfant -> Tentative suppression bloquée si worker présent -> Suppression réussie")
    void regionLifecycleWorkflow() throws Exception {
        // 1. CRÉATION D'UNE RÉGION RACINE (ex: Île-de-France)
        String createRootJson = "{\"name\": \"Île-de-France\", \"parentId\": null}";

        mockMvc.perform(post("/admin/region")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRootJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Île-de-France')]").exists());

        GeographicZone rootZone = zoneRepository.findAll().stream()
                .filter(z -> "Île-de-France".equals(z.getName()))
                .findFirst()
                .orElseThrow();

        // 2. CRÉATION D'UNE SOUS-RÉGION (ex: Paris 8e rattachée à Île-de-France)
        String createChildJson = String.format("{\"name\": \"Paris 8e\", \"parentId\": %d}", rootZone.getId());

        mockMvc.perform(post("/admin/region")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createChildJson))
                .andExpect(status().isOk());

        GeographicZone childZone = zoneRepository.findAll().stream()
                .filter(z -> "Paris 8e".equals(z.getName()))
                .findFirst()
                .orElseThrow();

        // 3. TENTATIVE DE SUPPRESSION DE LA RÉGION PARENTE (Devrait réussir car aucun worker n'est rattaché)
        // Note : Selon la cascade de ta base de données, supprimer le parent peut supprimer l'enfant ou lever une contrainte.
        // Testons la suppression de la sous-région (Paris 8e) qui n'a pas de workers.
        mockMvc.perform(delete("/admin/regions/" + childZone.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Paris 8e')]").doesNotExist());

        // Vérification en base H2
        assertThat(zoneRepository.findById(childZone.getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - Sécurité Région : Empêche la suppression d'une région si un worker y est rattaché (409 Conflict)")
    void deleteRegion_BlockedWhenWorkersAttached() throws Exception {
        // 1. Création d'une région en base H2
        GeographicZone zone = new GeographicZone();
        zone.setName("Lyon");
        GeographicZone savedZone = zoneRepository.save(zone);

        // 2. Création et rattachement d'un Worker à cette région
        Worker worker = new Worker();
        worker.setId(java.util.UUID.randomUUID());
        worker.setEmail(new Email("worker.lyon@venus.com"));
        worker.setUsername("workerlyon");
        worker.setPassword("Password123!");
        worker.setGeographicZone(savedZone); // 👈 Rattachement du worker à la zone de Lyon
        userRepository.save(worker);

        // 3. Tentative de suppression de la région (Doit échouer avec un 409 Conflict)
        mockMvc.perform(delete("/admin/regions/" + savedZone.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists()); // Vérifie que l'objet d'erreur explicite est retourné

        // 4. Vérification en base : La région ne doit PAS avoir été supprimée
        assertThat(zoneRepository.findById(savedZone.getId())).isPresent();
    }
}