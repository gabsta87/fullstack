package com.serv.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.serv.database.entities.Client;
import com.serv.database.entities.PasswordResetToken;
import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.requests.Login;
import com.serv.requests.Register;
import com.serv.services.AbuseProtectionService;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationTest extends IntegrationTest {

    @Autowired PasswordResetTokenRepository resetTokens;

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new Login(email, password))));
    }

    private ResultActions register(String role, String body) throws Exception {
        return mvc.perform(post("/auth/register/" + role).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions sessionCheck(String authorization) throws Exception {
        return mvc.perform(get("/auth/session-check").header("Authorization", authorization));
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Nested
    class Login_ {

        @Test
        @DisplayName("Valid credentials return a working token")
        void validCredentials() throws Exception {
            createClient("alice@venus.test");
            String body = login("alice@venus.test", PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.role").value("CLIENT"))
                    .andReturn().getResponse().getContentAsString();
            String token = json.readTree(body).get("token").asText();
            sessionCheck("Bearer " + token).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Email is case-insensitive and trimmed")
        void emailCaseInsensitive() throws Exception {
            createClient("alice@venus.test");
            login("  ALICE@Venus.Test ", PASSWORD).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Wrong password and unknown email give the same answer (no account enumeration)")
        void sameErrorForUnknownAndWrong() throws Exception {
            createClient("alice@venus.test");
            String wrong = login("alice@venus.test", "wrong-password").andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();
            String unknown = login("nobody@venus.test", "wrong-password").andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();
            assertThat(unknown).isEqualTo(wrong);
        }

        @Test
        @DisplayName("Banned accounts cannot log in")
        void bannedCannotLogin() throws Exception {
            Client c = createClient("alice@venus.test");
            c.setBanned(true);
            clientRepository.save(c);
            login("alice@venus.test", PASSWORD).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Brute force: the account refuses logins after 5 failures, even with the right password")
        void bruteForceLockout() throws Exception {
            createClient("alice@venus.test");
            for (int i = 0; i < AbuseProtectionService.MAX_LOGIN_FAILURES; i++) {
                login("alice@venus.test", "guess-" + i).andExpect(status().isUnauthorized());
            }
            login("alice@venus.test", PASSWORD).andExpect(status().isTooManyRequests());
            login("ALICE@venus.test", PASSWORD).andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("A successful login resets the failure counter")
        void successResetsCounter() throws Exception {
            createClient("alice@venus.test");
            for (int i = 0; i < AbuseProtectionService.MAX_LOGIN_FAILURES - 1; i++) {
                login("alice@venus.test", "guess-" + i);
            }
            login("alice@venus.test", PASSWORD).andExpect(status().isOk());
            login("alice@venus.test", "guess").andExpect(status().isUnauthorized());
            login("alice@venus.test", PASSWORD).andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"{}", "{\"email\":null,\"password\":null}", "{\"email\":\"a@b.c\"}", "not json", "[]"})
        @DisplayName("Malformed login bodies are rejected with 400, never 500")
        void malformedBodies(String body) throws Exception {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Oversized inputs are rejected before hashing")
        void oversizedInputs() throws Exception {
            login("a".repeat(300) + "@venus.test", PASSWORD).andExpect(status().isBadRequest());
            login("alice@venus.test", "x".repeat(10_000)).andExpect(status().isBadRequest());
        }
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Nested
    class Registration {

        @ParameterizedTest
        @ValueSource(strings = {"client", "worker"})
        @DisplayName("Registration succeeds with valid data and stores a normalized email")
        void registerOk(String role) throws Exception {
            register(role, toJson(new Register("New.User@Venus.Test", PASSWORD))).andExpect(status().isOk());
            assertThat(userRepository.findByNormalizedEmail("new.user@venus.test")).isPresent();
        }

        @ParameterizedTest
        @ValueSource(strings = {"client", "worker"})
        @DisplayName("The same email cannot be registered twice, whatever its case")
        void duplicateEmail(String role) throws Exception {
            createClient("taken@venus.test");
            register(role, toJson(new Register("TAKEN@venus.test", PASSWORD))).andExpect(status().isConflict());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "plainaddress", "@venus.test", "a@", "a b@venus.test", "a@venus..test", "<script>@x.com"})
        @DisplayName("Invalid emails are rejected")
        void invalidEmail(String email) throws Exception {
            register("client", toJson(new Register(email, PASSWORD))).andExpect(status().isBadRequest());
            assertThat(userRepository.count()).isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "       ", "short", "1234567"})
        @DisplayName("Weak passwords are rejected")
        void weakPassword(String password) throws Exception {
            register("client", toJson(new Register("weak@venus.test", password))).andExpect(status().isBadRequest());
            assertThat(userRepository.count()).isZero();
        }

        @Test
        @DisplayName("Passwords longer than 72 bytes are rejected (BCrypt would silently truncate them)")
        void tooLongPassword() throws Exception {
            register("client", toJson(new Register("long@venus.test", "é".repeat(40)))).andExpect(status().isBadRequest());
            register("client", toJson(new Register("long@venus.test", "a".repeat(73)))).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Unknown fields cannot escalate privileges (mass assignment)")
        void massAssignment() throws Exception {
            String body = "{\"email\":\"sneaky@venus.test\",\"password\":\"" + PASSWORD + "\","
                    + "\"role\":\"ADMIN\",\"isBanned\":false,\"userType\":\"SUPER_ADMIN\",\"tokenVersion\":99}";
            register("client", body).andExpect(status().isOk());
            VenusUser user = userRepository.findByEmail("sneaky@venus.test").orElseThrow();
            assertThat(user.isAdmin()).isFalse();
            assertThat(user.getTokenVersion()).isZero();
        }

        @Test
        @DisplayName("Missing or null body fields give 400")
        void missingFields() throws Exception {
            register("client", "{}").andExpect(status().isBadRequest());
            register("worker", "{\"email\":\"x@venus.test\"}").andExpect(status().isBadRequest());
            register("client", "").andExpect(status().isBadRequest());
        }
    }

    // ── Tokens ───────────────────────────────────────────────────────────────

    @Nested
    class Tokens {

        private String sign(String secret, JwtClaimsSet claims) {
            var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            var jwk = new OctetSequenceKey.Builder(key).algorithm(JWSAlgorithm.HS256).build();
            var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
            return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        }

        private JwtClaimsSet claimsFor(VenusUser user, Instant expiresAt) {
            return JwtClaimsSet.builder()
                    .issuer("venus-api")
                    .issuedAt(expiresAt.minus(1, ChronoUnit.HOURS))
                    .expiresAt(expiresAt)
                    .subject(user.getEmail().getValue())
                    .claim("userId", user.getId().toString())
                    .claim("tv", user.getTokenVersion())
                    .build();
        }

        @Test
        @DisplayName("A token signed with another key is rejected (the old hard-coded key no longer works)")
        void forgedToken() throws Exception {
            Client c = createClient("alice@venus.test");
            String forged = sign("MaCleSecreteSuperSecuriseeDePlusDe32Caracteres!", claimsFor(c, Instant.now().plusSeconds(3600)));
            sessionCheck("Bearer " + forged).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("An expired token is rejected")
        void expiredToken() throws Exception {
            Client c = createClient("alice@venus.test");
            String expired = sign("test-only-secret-key-that-is-long-enough-for-hs256",
                    claimsFor(c, Instant.now().minus(2, ChronoUnit.HOURS)));
            sessionCheck("Bearer " + expired).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("An unsigned token (alg=none) is rejected")
        void unsignedToken() throws Exception {
            Client c = createClient("alice@venus.test");
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
            String payload = b64.encodeToString(("{\"sub\":\"alice@venus.test\",\"userId\":\"" + c.getId()
                    + "\",\"tv\":0,\"exp\":" + (Instant.now().getEpochSecond() + 3600) + "}").getBytes());
            sessionCheck("Bearer " + header + "." + payload + ".").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Changing the payload of a valid token breaks its signature")
        void tamperedToken() throws Exception {
            Client c = createClient("alice@venus.test");
            var admin = createAdmin("admin@venus.test");
            String[] parts = jwtProvider.generateToken(c).split("\\.");
            String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"sub\":\"admin@venus.test\",\"userId\":\"" + admin.getId() + "\",\"tv\":0,\"exp\":"
                            + (Instant.now().getEpochSecond() + 3600) + "}").getBytes());
            sessionCheck("Bearer " + parts[0] + "." + forgedPayload + "." + parts[2]).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A banned user's existing token stops working immediately")
        void bannedUserTokenRevoked() throws Exception {
            Client c = createClient("alice@venus.test");
            String auth = bearer(c);
            sessionCheck(auth).andExpect(status().isOk());

            Client fresh = reload(c);
            fresh.setBanned(true);
            clientRepository.save(fresh);

            sessionCheck(auth).andExpect(status().isUnauthorized());
            mvc.perform(get("/account/client/me").header("Authorization", auth)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unbanning does not resurrect tokens issued before the ban")
        void unbanDoesNotRestoreOldTokens() throws Exception {
            Client c = createClient("alice@venus.test");
            String auth = bearer(c);
            Client fresh = reload(c);
            fresh.setBanned(true);
            clientRepository.save(fresh);
            fresh = reload(c);
            fresh.setBanned(false);
            clientRepository.save(fresh);
            sessionCheck(auth).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Tokens of a deleted account are rejected")
        void deletedUserToken() throws Exception {
            Client c = createClient("alice@venus.test");
            String auth = bearer(c);
            clientRepository.deleteById(c.getId());
            sessionCheck(auth).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A password reset revokes every token issued before")
        void passwordResetRevokesTokens() throws Exception {
            Client c = createClient("alice@venus.test");
            String auth = bearer(c);
            UUID token = UUID.randomUUID();
            resetTokens.save(new PasswordResetToken(token, reload(c)));

            mvc.perform(post("/auth/reset-password/confirm")
                            .param("resetToken", token.toString())
                            .param("newPassword", "Brand-New-Pass-1"))
                    .andExpect(status().isOk());

            sessionCheck(auth).andExpect(status().isUnauthorized());
            login("alice@venus.test", "Brand-New-Pass-1").andExpect(status().isOk());
            login("alice@venus.test", PASSWORD).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("The ?token= parameter is only accepted for event streams")
        void queryTokenOnlyForStreams() throws Exception {
            Client c = createClient("alice@venus.test");
            String token = jwtProvider.generateToken(c);
            mvc.perform(get("/account/client/me").param("token", token)).andExpect(status().isUnauthorized());
            mvc.perform(get("/auth/session-check").param("token", token)).andExpect(status().isUnauthorized());
            mvc.perform(get("/account/stream").param("token", token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("The account stream rejects invalid, revoked and banned tokens")
        void streamRejectsBadTokens() throws Exception {
            Client c = createClient("alice@venus.test");
            String token = jwtProvider.generateToken(c);
            mvc.perform(get("/account/stream").param("token", "garbage")).andExpect(status().isUnauthorized());
            mvc.perform(get("/account/stream")).andExpect(status().isUnauthorized());
            Client fresh = reload(c);
            fresh.setBanned(true);
            clientRepository.save(fresh);
            mvc.perform(get("/account/stream").param("token", token)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A token is bound to its account email: changing the email invalidates it")
        void emailChangeInvalidatesToken() throws Exception {
            Client c = createClient("alice@venus.test");
            String auth = bearer(c);
            Client fresh = reload(c);
            fresh.setEmail(new com.serv.database.entities.Email("other@venus.test"));
            clientRepository.save(fresh);
            sessionCheck(auth).andExpect(status().isUnauthorized());
        }
    }

    // ── Password reset ───────────────────────────────────────────────────────

    @Nested
    class PasswordReset {

        @Test
        @DisplayName("Same answer for known and unknown emails (no account enumeration)")
        void noEnumeration() throws Exception {
            createClient("alice@venus.test");
            String known = mvc.perform(post("/auth/reset-password").param("email", "alice@venus.test"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String unknown = mvc.perform(post("/auth/reset-password").param("email", "ghost@venus.test"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(unknown).isEqualTo(known);
            assertThat(resetTokens.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Reset emails are throttled per address (no mail bombing)")
        void throttled() throws Exception {
            createClient("alice@venus.test");
            for (int i = 0; i < AbuseProtectionService.MAX_RESET_REQUESTS + 3; i++) {
                mvc.perform(post("/auth/reset-password").param("email", "alice@venus.test")).andExpect(status().isOk());
            }
            org.mockito.Mockito.verify(mailSender, org.mockito.Mockito.times(AbuseProtectionService.MAX_RESET_REQUESTS))
                    .send(org.mockito.ArgumentMatchers.any(jakarta.mail.internet.MimeMessage.class));
        }

        @Test
        @DisplayName("Unknown, malformed and expired reset tokens are refused")
        void badTokens() throws Exception {
            Client c = createClient("alice@venus.test");
            mvc.perform(post("/auth/reset-password/confirm")
                            .param("resetToken", UUID.randomUUID().toString()).param("newPassword", "Brand-New-Pass-1"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/auth/reset-password/confirm")
                            .param("resetToken", "not-a-uuid").param("newPassword", "Brand-New-Pass-1"))
                    .andExpect(status().isBadRequest());

            UUID expired = UUID.randomUUID();
            PasswordResetToken t = new PasswordResetToken(expired, reload(c));
            t.setExpiryDate(Instant.now().minusSeconds(1));
            resetTokens.save(t);
            mvc.perform(post("/auth/reset-password/confirm")
                            .param("resetToken", expired.toString()).param("newPassword", "Brand-New-Pass-1"))
                    .andExpect(status().isBadRequest());
            login("alice@venus.test", PASSWORD).andExpect(status().isOk());
        }

        @Test
        @DisplayName("A reset token can only be used once")
        void singleUse() throws Exception {
            Client c = createClient("alice@venus.test");
            UUID token = UUID.randomUUID();
            resetTokens.save(new PasswordResetToken(token, reload(c)));
            mvc.perform(post("/auth/reset-password/confirm")
                    .param("resetToken", token.toString()).param("newPassword", "Brand-New-Pass-1")).andExpect(status().isOk());
            mvc.perform(post("/auth/reset-password/confirm")
                    .param("resetToken", token.toString()).param("newPassword", "Another-Pass-22")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("The new password must follow the password policy")
        void policyOnReset() throws Exception {
            Client c = createClient("alice@venus.test");
            UUID token = UUID.randomUUID();
            resetTokens.save(new PasswordResetToken(token, reload(c)));
            mvc.perform(post("/auth/reset-password/confirm")
                    .param("resetToken", token.toString()).param("newPassword", "123")).andExpect(status().isBadRequest());
            assertThat(resetTokens.findById(token)).isPresent();
        }
    }
}
