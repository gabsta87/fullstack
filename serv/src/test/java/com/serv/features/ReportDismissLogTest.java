package com.serv.features;

import com.serv.database.entities.Admin;
import com.serv.database.entities.Client;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportDismissLogTest extends IntegrationTest {

    @Test
    @DisplayName("A dismissed report stays visible: kept in the reports, and the log shows its text, its id, the profile and its email")
    void dismissedReportTraceable() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        Client c = createClient("client@venus.test");
        Worker w = createVisibleWorker("luna@venus.test");

        mvc.perform(post("/account/client/report-account").header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("workerId", w.getId().toString(), "reason", "Fake photos on this profile"))))
                .andExpect(status().isOk());
        String summary = mvc.perform(get("/admin/reports-summary").header("Authorization", bearer(admin)))
                .andReturn().getResponse().getContentAsString();
        long reportId = json.readTree(summary).get(0).get("reports").get(0).get("reportId").asLong();

        mvc.perform(post("/admin/reports/" + reportId + "/dismiss").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        // Still in the reports, as dismissed
        mvc.perform(get("/admin/reports-summary").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].reports[0].reportStatus").value("DISMISSED"))
                .andExpect(jsonPath("$[0].reports[0].reason").value("Fake photos on this profile"));

        JsonNode entry = json.readTree(mvc.perform(get("/admin/logs").header("Authorization", bearer(admin)))
                .andReturn().getResponse().getContentAsString()).get(0);
        assertThat(entry.get("actionType").asString()).isEqualTo("DISMISS_REPORT");
        assertThat(entry.get("targetType").asString()).isEqualTo("WORKER");
        assertThat(entry.get("targetId").asString()).isEqualTo(w.getId().toString());
        assertThat(entry.get("targetEmail").asString()).isEqualTo("luna@venus.test");
        assertThat(entry.get("details").asString()).contains("Fake photos on this profile");
        String raw = entry.get("targetSnapshot").asString();
        JsonNode snapshot = json.readTree(json.readTree(raw).isString() ? json.readTree(raw).asString() : raw);
        assertThat(snapshot.get("reportId").asLong()).isEqualTo(reportId);
        assertThat(snapshot.get("reason").asString()).isEqualTo("Fake photos on this profile");
    }
}
