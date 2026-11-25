package com.serv.security;

import com.serv.database.entities.Client;
import com.serv.database.entities.Worker;
import com.serv.requests.Login;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Anonymous visitors browse the whole public site. A browser that still holds a bad token
 * (expired, revoked, signed with an old key...) must be treated as anonymous on public routes,
 * otherwise the visitor cannot even reach the login page.
 */
class PublicAccessTest extends IntegrationTest {

    /** A token signed with the key used before JWT_SECRET existed. */
    private static final String OLD_KEY_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4QHkueiIsInVzZXJJZCI6IjEyMyJ9.invalidsig";

    private List<String> badTokens() {
        Client revoked = createClient("revoked@venus.test");
        String revokedToken = bearer(revoked);
        Client fresh = reload(revoked);
        fresh.revokeTokens();
        clientRepository.save(fresh);

        Client banned = createClient("banned@venus.test");
        String bannedToken = bearer(banned);
        Client b = reload(banned);
        b.setBanned(true);
        clientRepository.save(b);

        List<String> tokens = new ArrayList<>();
        tokens.add("Bearer " + OLD_KEY_TOKEN);
        tokens.add("Bearer garbage");
        tokens.add("Bearer ");
        tokens.add(revokedToken);
        tokens.add(bannedToken);
        return tokens;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/workers", "/workers?page=0", "/common/services", "/common/regions"})
    @DisplayName("Public pages answer 200 to anonymous visitors, with or without a bad token")
    void publicPagesWithBadTokens(String url) throws Exception {
        createVisibleWorker("luna@venus.test");
        mvc.perform(get(url)).andExpect(status().isOk());
        for (String token : badTokens()) {
            mvc.perform(get(url).header("Authorization", token)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("A public profile opens with a bad token")
    void publicProfileWithBadToken() throws Exception {
        Worker w = createVisibleWorker("luna@venus.test");
        for (String token : badTokens()) {
            mvc.perform(get("/workers/" + w.getId()).header("Authorization", token)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Login and registration work even if the browser sends a stale token")
    void loginWithStaleToken() throws Exception {
        createClient("alice@venus.test");
        mvc.perform(post("/auth/login").header("Authorization", "Bearer " + OLD_KEY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(new Login("alice@venus.test", PASSWORD))))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/register/client").header("Authorization", "Bearer " + OLD_KEY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@venus.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/reset-password").header("Authorization", "Bearer " + OLD_KEY_TOKEN)
                .param("email", "alice@venus.test")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A bad token never grants anything on protected routes (401)")
    void protectedRoutesStillRejectBadTokens() throws Exception {
        for (String token : badTokens()) {
            mvc.perform(get("/auth/session-check").header("Authorization", token)).andExpect(status().isUnauthorized());
            mvc.perform(get("/account/client/me").header("Authorization", token)).andExpect(status().isUnauthorized());
            mvc.perform(get("/admin/users").header("Authorization", token)).andExpect(status().isUnauthorized());
            mvc.perform(post("/account/client/comment").header("Authorization", token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("A hidden profile stays hidden when the token is invalid (no admin rights from a bad token)")
    void hiddenProfileWithBadToken() throws Exception {
        Worker w = createVisibleWorker("luna@venus.test");
        Worker hidden = workerRepository.findById(w.getId()).orElseThrow();
        hidden.setHidden(true);
        workerRepository.save(hidden);
        for (String token : badTokens()) {
            mvc.perform(get("/workers/" + w.getId()).header("Authorization", token)).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Anonymous visitors cannot comment, favorite or report")
    void anonymousCannotAct() throws Exception {
        Worker w = createVisibleWorker("luna@venus.test");
        mvc.perform(post("/account/client/comment").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hi\",\"workerId\":\"" + w.getId() + "\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/account/client/favorites/" + w.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/account/client/report-account").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"x\",\"workerId\":\"" + w.getId() + "\"}")).andExpect(status().isUnauthorized());
        assertThat(commentRepository.count()).isZero();
    }
}
