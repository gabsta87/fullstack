package com.serv.features;

import com.serv.database.entities.Admin;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import com.serv.support.TestImages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSettingsTest extends IntegrationTest {

    private ResultActions put(Admin admin, String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/admin/settings")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int value(Admin admin, String key) throws Exception {
        String body = mvc.perform(get("/admin/settings").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode n : json.readTree(body)) {
            if (n.get("key").asString().equals(key)) return n.get("value").asInt();
        }
        throw new AssertionError("missing " + key);
    }

    @Test
    @DisplayName("Defaults are listed with their range; a change is saved, logged with old and new values, and kept")
    void updateAndAudit() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        mvc.perform(get("/admin/settings").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'CERTIFICATION_VALIDITY_MONTHS')].value").value(8))
                .andExpect(jsonPath("$[?(@.key == 'CERTIFICATION_RENEWAL_MONTHS')].value").value(2))
                .andExpect(jsonPath("$[?(@.key == 'STORAGE_LIMIT_MB')].value").value(50))
                .andExpect(jsonPath("$[?(@.key == 'STORAGE_LIMIT_MB')].max").value(10_240));

        put(admin, "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":12,\"STORAGE_LIMIT_MB\":50}}").andExpect(status().isOk());
        assertThat(value(admin, "CERTIFICATION_VALIDITY_MONTHS")).isEqualTo(12);

        // Only the real change is logged, as JSON
        String snapshot = jdbc.queryForObject(
                "SELECT target_snapshot FROM admin_audit_logs WHERE action_type = 'SETTINGS_UPDATED'", String.class);
        // H2 keeps the JSON column as a JSON string; MySQL as an object
        String raw = json.readTree(snapshot).isString() ? json.readTree(snapshot).asString() : snapshot;
        JsonNode changes = json.readTree(raw).get("changes");
        assertThat(changes.get("CERTIFICATION_VALIDITY_MONTHS").get("from").asInt()).isEqualTo(8);
        assertThat(changes.get("CERTIFICATION_VALIDITY_MONTHS").get("to").asInt()).isEqualTo(12);
        assertThat(changes.has("STORAGE_LIMIT_MB")).isFalse();
        mvc.perform(get("/admin/logs").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].actionType").value("SETTINGS_UPDATED"))
                .andExpect(jsonPath("$[0].targetType").value("SETTINGS"));

        // The same values again change nothing and log nothing
        put(admin, "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":12}}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Invalid settings are refused with 400 and nothing is saved (all or nothing)")
    void invalidRefused() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        String[] bodies = {
                "{\"values\":{\"UNKNOWN\":3}}",
                "{\"values\":{\"certification_validity_months\":3}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":0}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":37}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":-5}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":null}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":\"eight\"}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":99999999999}}",
                "{\"values\":{\"STORAGE_LIMIT_MB\":9}}",
                // Renewal must stay shorter than the validity, in one request or against the stored value
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":3,\"CERTIFICATION_RENEWAL_MONTHS\":3}}",
                "{\"values\":{\"CERTIFICATION_VALIDITY_MONTHS\":2}}",
                // A valid value next to an invalid one is not saved either
                "{\"values\":{\"STORAGE_LIMIT_MB\":100,\"CERTIFICATION_RENEWAL_MONTHS\":13}}",
                "{\"values\":{}}",
                "{}",
                "[]",
                "not json"
        };
        for (String body : bodies) {
            put(admin, body).andExpect(status().isBadRequest());
        }
        assertThat(value(admin, "STORAGE_LIMIT_MB")).isEqualTo(50);
        assertThat(value(admin, "CERTIFICATION_VALIDITY_MONTHS")).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_settings", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_logs", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Workers and clients can neither read nor change the settings")
    void onlyAdmins() throws Exception {
        Worker w = createWorker("w@venus.test");
        mvc.perform(get("/admin/settings").header("Authorization", bearer(w))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/admin/settings")
                        .header("Authorization", bearer(w)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"STORAGE_LIMIT_MB\":10240}}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_settings", Integer.class)).isZero();
    }

    @Test
    @DisplayName("The storage limit set by the admins applies at once to photos and videos together")
    void storageLimitApplies() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        Worker w = createWorker("w@venus.test");
        put(admin, "{\"values\":{\"STORAGE_LIMIT_MB\":10}}").andExpect(status().isOk());

        mvc.perform(get("/account/worker/storage").header("Authorization", bearer(w)))
                .andExpect(jsonPath("$.maxBytes").value(10L * 1024 * 1024));

        // 10.5 MB already used (as if by a video): a small photo no longer fits
        Path dir = MEDIA_DIR.resolve(w.getId().toString()).resolve("videos");
        Files.createDirectories(dir);
        Files.write(dir.resolve("big.mp4"), new byte[10 * 1024 * 1024 + 512 * 1024]);
        MockMultipartFile photo = new MockMultipartFile("files", "me.jpg", "image/jpeg", TestImages.jpeg(200, 200));
        mvc.perform(multipart("/account/worker/media").file(photo).header("Authorization", bearer(w)))
                .andExpect(status().is(413));

        put(admin, "{\"values\":{\"STORAGE_LIMIT_MB\":20}}").andExpect(status().isOk());
        mvc.perform(multipart("/account/worker/media").file(photo).header("Authorization", bearer(w)))
                .andExpect(status().isOk());
    }
}
