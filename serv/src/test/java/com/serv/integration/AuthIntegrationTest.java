package com.serv.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.database.repositories.UserRepository;
import com.serv.requests.Register;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends com.serv.support.IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;


    @Test
    @DisplayName("Intégration - Inscription d'un client puis connexion réussie")
    void testRegisterAndLoginIntegrationWorkflow() throws Exception {
        // 1. Inscription
        Register registerReq = new Register("integration@venus.com", "SecurePassword123!");

        // On désactive provisoirement la vérif mail par configuration ou on simule
        mockMvc.perform(post("/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk());

        // Vérifie que l'utilisateur est bien présent en base H2
        assertThat(userRepository.findByEmail("integration@venus.com")).isPresent();

        // 2. Connexion
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.serv.requests.Login("integration@venus.com", "SecurePassword123!"))))
                .andExpect(status().isOk());
    }
}