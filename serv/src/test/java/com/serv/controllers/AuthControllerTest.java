package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.configuration.JwtProvider;
import com.serv.controller.AuthController;
import com.serv.database.entities.Client;
import com.serv.database.entities.VerificationToken;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.VerificationTokenRepository;
import com.serv.requests.Register;
import com.serv.service.AuthService;
import com.serv.service.EmailVerificationService;
import com.serv.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; // 👈 Indispensable ici aussi
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ClientRepository clientRepository;
    @MockBean
    private com.serv.database.repositories.WorkerRepository workerRepository;
    @MockBean
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockBean
    private PasswordResetService passwordResetService;
    @MockBean
    private EmailVerificationService verificationTokenService;

    @MockBean
    private AuthService authService;
    @MockBean
    private JwtProvider jwtProvider;

    @Test
    void testRegisterClientSuccess() throws Exception {
        Register registerReq = new Register("newclient@venus.com", "Password123!");

        when(userRepository.findByEmail("newclient@venus.com")).thenReturn(Optional.empty());
        when(verificationTokenService.createTokenAndSendEmail(any(Client.class))).thenReturn("http://localhost/verify?token=abc");

        mockMvc.perform(post("/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.CoreMatchers.containsString("Registration successful")));
    }

    @Test
    void testVerifyEmailSuccess() throws Exception {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setLocked(true);

        VerificationToken token = new VerificationToken();
        token.setToken("valid-token");
        token.setUser(client);
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        mockMvc.perform(get("/auth/verify-email?token=valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account activated successfully. You can now log in."));
    }
}