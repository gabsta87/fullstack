package com.serv.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.Client;
import com.serv.database.entities.VerificationToken;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.VerificationTokenRepository;
import com.serv.requests.Login;
import com.serv.requests.Register;
import com.serv.services.AuthService;
import com.serv.services.EmailVerificationService;
import com.serv.services.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserRepository userRepository;
    @MockBean private ClientRepository clientRepository;
    @MockBean private com.serv.database.repositories.WorkerRepository workerRepository;
    @MockBean private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean private VerificationTokenRepository verificationTokenRepository;

    @MockBean private PasswordResetService passwordResetService;
    @MockBean private EmailVerificationService verificationTokenService;
    @MockBean private AuthService authService;
    @MockBean private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /auth/register/client - Succès de l'inscription client")
    void testRegisterClientSuccess() throws Exception {
        Register registerReq = new Register("newclient@venus.com", "Password123!");

        when(userRepository.findByEmail("newclient@venus.com")).thenReturn(Optional.empty());
        when(verificationTokenService.createTokenAndSendEmail(any(Client.class))).thenReturn("http://localhost/verify?token=abc");

        // Ligne modifiée pour accepter l'objet JSON enveloppé de type Map.of("message", ...)
        mockMvc.perform(post("/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.CoreMatchers.containsString("Registration successful")));
    }

    @Test
    @DisplayName("POST /auth/register/client - Échec si l'e-mail existe déjà (409)")
    void testRegisterClientEmailAlreadyTaken() throws Exception {
        Register registerReq = new Register("existing@venus.com", "Password123!");

        when(userRepository.findByEmail("existing@venus.com")).thenReturn(Optional.of(new Client()));

        mockMvc.perform(post("/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/login - Succès même si le compte est verrouillé (pour permettre la complétion du profil)")
    void testLoginAccountLockedAllowed() throws Exception {
        Login loginReq = new Login("locked@venus.com", "Password123!");
        Client lockedClient = new Client();
        lockedClient.setLocked(true); // Compte verrouillé mais non banni

        when(authService.verifyCredentials("locked@venus.com", "Password123!")).thenReturn(lockedClient);
        when(jwtProvider.generateToken(any())).thenReturn("mock-jwt-token");

        // Doit retourner 200 OK pour autoriser l'accès au tableau de bord de complétion
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("POST /auth/login - Échec si le compte est banni (403 Forbidden)")
    void testLoginAccountBanned() throws Exception {
        Login loginReq = new Login("banned@venus.com", "Password123!");
        Client bannedClient = new Client();
        bannedClient.setBanned(true); // Compte banni

        when(authService.verifyCredentials("banned@venus.com", "Password123!")).thenReturn(bannedClient);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /auth/verify-email - Succès de l'activation du compte")
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

    @Test
    @DisplayName("GET /auth/verify-email - Échec si le token a expiré")
    void testVerifyEmailExpiredToken() throws Exception {
        VerificationToken expiredToken = new VerificationToken();
        expiredToken.setToken("expired-token");
        expiredToken.setExpiryDate(Instant.now().minusSeconds(3600));

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        mockMvc.perform(get("/auth/verify-email?token=expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Link has expired."));
    }
}