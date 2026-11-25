package com.serv.integration;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIntegrationTest extends com.serv.support.IntegrationTest {

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
        // Main admin who performs the requests
        admin = new Admin();
        admin.setEmail(new Email("master.admin@venus.com"));
        admin.setUsername("master.admin@venus.com");
        admin.setPassword("Password123!");
        adminRepository.save(admin);

        adminToken = jwtProvider.generateToken(admin);
    }

    @Test
    @DisplayName("WORKFLOW - service lifecycle: creation -> deletion -> check")
    void serviceLifecycleWorkflow() throws Exception {
        UpdateService newServiceReq = new UpdateService(null, "FIRST", "Description du service");

        // 1. CREATE THE SERVICE
        mockMvc.perform(post("/admin/service")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newServiceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'FIRST')]").exists());

        // Check in the H2 database
        Service savedService = serviceRepository.findByName("FIRST").orElseThrow();
        assertThat(savedService.getName()).isEqualTo("FIRST");

        // 2. SUPPRESSION DU SERVICE
        mockMvc.perform(delete("/admin/services/" + savedService.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'FIRST')]").doesNotExist());

        // 3. CHECK AFTER DELETION (must be gone from the H2 database)
        assertThat(serviceRepository.findById(savedService.getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - admin lifecycle: invitation -> stored in database -> deletion by a super admin only")
    void adminInvitationAndDeletionWorkflow() throws Exception {
        AdminInvitation invitationReq = new AdminInvitation("new.admin@venus.com");

        // 1. INVITATION / PRE-CREATION OF THE ADMIN
        mockMvc.perform(post("/admin/invite")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invitationReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.admin@venus.com"));

        // Check in the H2 database that it exists and is locked
        var createdAdmin = adminRepository.findByEmail(new Email("new.admin@venus.com"));
        assertThat(createdAdmin).isPresent();
        assertThat(createdAdmin.get().isLocked()).isTrue();

        // 2. ADMIN ACCOUNT DELETION: refused to a plain admin, allowed to the super admin
        mockMvc.perform(delete("/admin/users/" + createdAdmin.get().getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        String superAdminToken = jwtProvider.generateToken(createSuperAdmin("root@venus.com"));
        mockMvc.perform(delete("/admin/users/" + createdAdmin.get().getId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        // 3. CHECK AFTER DELETION (must not be found)
        assertThat(userRepository.findById(createdAdmin.get().getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - security: an admin cannot delete their own account")
    void adminCannotDeleteSelf() throws Exception {
        mockMvc.perform(delete("/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An admin cannot suppress himself"));

        // Check in the database: the admin is still there
        assertThat(adminRepository.findById(admin.getId())).isPresent();
    }

    @Test
    @DisplayName("WORKFLOW - region lifecycle: root -> child -> child deletion")
    void regionLifecycleWorkflow() throws Exception {
        // 1. CREATE A ROOT REGION (e.g. Île-de-France)
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

        // 2. CREATE A SUB-REGION (e.g. Paris 8e under Île-de-France)
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

        // 3. DELETE THE CHILD REGION (should succeed: no worker attached)
        // Note: depending on the cascade settings, deleting the parent may delete the child or violate a constraint.
        // So the sub-region (Paris 8e), which has no workers, is deleted.
        mockMvc.perform(delete("/admin/regions/" + childZone.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Paris 8e')]").doesNotExist());

        // Check in the H2 database
        assertThat(zoneRepository.findById(childZone.getId())).isEmpty();
    }

    @Test
    @DisplayName("WORKFLOW - region security: a region with attached workers cannot be deleted (409 Conflict)")
    void deleteRegion_BlockedWhenWorkersAttached() throws Exception {
        // 1. Create a region in the H2 database
        GeographicZone zone = new GeographicZone();
        zone.setName("Lyon");
        GeographicZone savedZone = zoneRepository.save(zone);

        // 2. Create a worker attached to this region
        Worker worker = new Worker();
        worker.setEmail(new Email("worker.lyon@venus.com"));
        worker.setUsername("workerlyon");
        worker.setPassword("Password123!");
        worker.setGeographicZone(savedZone);
        userRepository.save(worker);

        // 3. Try to delete the region (must fail with 409 Conflict)
        mockMvc.perform(delete("/admin/regions/" + savedZone.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists()); // The explicit error object is returned

        // 4. Check in the database: the region must NOT have been deleted
        assertThat(zoneRepository.findById(savedZone.getId())).isPresent();
    }
}