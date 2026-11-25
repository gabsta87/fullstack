package com.serv.security;

import com.serv.database.entities.Admin;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProfileIssuesTest extends IntegrationTest {

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = createAdmin("admin@venus.test");
    }

    private String issuesOf(Worker worker) {
        return "$[?(@.id == '" + worker.getId() + "')].profileIssues[*]";
    }

    @Test
    @DisplayName("Admins see every field missing from a new worker profile")
    void newWorkerMissesEverything() throws Exception {
        Worker worker = createWorker("new@venus.test");
        mvc.perform(get("/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(issuesOf(worker)).value(contains(
                        "USERNAME", "DESCRIPTION", "LOCATION", "PHONE", "BIRTHDATE", "SERVICES", "PHOTOS")));
    }

    @Test
    @DisplayName("A complete profile has no issue; a hidden or banned one says why")
    void statusIssues() throws Exception {
        Worker complete = createVisibleWorker("ok@venus.test");
        Worker hidden = createVisibleWorker("hidden@venus.test");
        hidden.setHidden(true);
        hidden.setBanned(true);
        workerRepository.save(hidden);

        mvc.perform(get("/admin/users").header("Authorization", bearer(admin)))
                .andExpect(jsonPath(issuesOf(complete)).value(empty()))
                .andExpect(jsonPath(issuesOf(hidden)).value(contains("BANNED", "HIDDEN")));
    }

    @Test
    @DisplayName("A minor is reported as such, even when every field is filled")
    void minor() throws Exception {
        Worker minor = createVisibleWorker("minor@venus.test");
        minor.setBirthdate(java.time.LocalDate.now().minusYears(17));
        workerRepository.save(minor);

        mvc.perform(get("/admin/users").header("Authorization", bearer(admin)))
                .andExpect(jsonPath(issuesOf(minor)).value(contains("MINOR")));
    }
}
