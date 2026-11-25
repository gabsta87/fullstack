package com.serv.logging;

import com.serv.database.entities.Client;
import com.serv.database.entities.PasswordResetToken;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.requests.Login;
import com.serv.requests.Register;
import com.serv.services.AbuseProtectionService;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class LoggingTest extends IntegrationTest {

    @Autowired PasswordResetTokenRepository resetTokens;

    private void login(String email, String password) throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(toJson(new Login(email, password))));
    }

    @Test
    @DisplayName("Every response carries a request id, also present in the log lines")
    void requestId(CapturedOutput output) throws Exception {
        String id = mvc.perform(get("/admin/users").header("Authorization", bearer(createClient("c@venus.test"))))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(output).contains("[" + id + "]").contains("Refused GET /admin/users -> 403");
    }

    @Test
    @DisplayName("Failed logins and lockouts are logged with the account id, never the email or the password")
    void loginFailures(CapturedOutput output) throws Exception {
        Client c = createClient("alice@venus.test");
        for (int i = 0; i <= AbuseProtectionService.MAX_LOGIN_FAILURES; i++) {
            login("alice@venus.test", "Guess-Number-" + i);
        }
        assertThat(output)
                .contains("Login failed for account " + c.getId())
                .contains("SECURITY login refused, too many failures for account " + c.getId())
                .doesNotContain("alice@venus.test")
                .doesNotContain("Guess-Number-");
    }

    @Test
    @DisplayName("Logs never contain passwords, tokens or email addresses across the main flows")
    void noSecretsInLogs(CapturedOutput output) throws Exception {
        String password = "Very-Secret-Pass-99";
        mvc.perform(post("/auth/register/client").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new Register("bob.private@venus.test", password)))).andExpect(status().isOk());
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new Login("bob.private@venus.test", password)))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asString();

        mvc.perform(get("/account/stream").param("token", token)).andExpect(status().isOk());
        mvc.perform(patch("/account/data").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("password", "Another-Secret-77", "oldPassword", password)))).andExpect(status().isOk());
        mvc.perform(post("/auth/reset-password").param("email", "bob.private@venus.test")).andExpect(status().isOk());

        resetTokens.deleteAll(); // replace the emailed token by one whose value the test knows
        UUID resetToken = UUID.randomUUID();
        resetTokens.save(new PasswordResetToken(resetToken, userRepository.findByEmail("bob.private@venus.test").orElseThrow()));
        mvc.perform(post("/auth/reset-password/confirm").param("resetToken", resetToken.toString())
                .param("newPassword", "Third-Secret-55")).andExpect(status().isOk());

        assertThat(output)
                .contains("registered (CLIENT)")
                .contains("Password reset completed")
                .doesNotContain(password)
                .doesNotContain("Another-Secret-77")
                .doesNotContain("Third-Secret-55")
                .doesNotContain(token)
                .doesNotContain(resetToken.toString())
                .doesNotContain("bob.private@venus.test");
    }

    @Test
    @DisplayName("Admin actions are logged with ids only")
    void adminActions(CapturedOutput output) throws Exception {
        var admin = createAdmin("admin@venus.test");
        var worker = createVisibleWorker("worker.private@venus.test");
        mvc.perform(post("/admin/profiles/" + worker.getId() + "/status").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"banned\":true}")).andExpect(status().isOk());
        assertThat(output)
                .contains("ADMIN UPDATE_WORKER_STATUS by admin " + admin.getId() + " on account " + worker.getId())
                .doesNotContain("worker.private@venus.test");
    }
}
