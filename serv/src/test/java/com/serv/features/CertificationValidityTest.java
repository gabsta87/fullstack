package com.serv.features;

import com.serv.common.CertificationStatus;
import com.serv.database.entities.Admin;
import com.serv.database.entities.CertificationRequest;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.CertificationRequestRepository;
import com.serv.services.CertificationDatesMigration;
import com.serv.support.IntegrationTest;
import com.serv.support.TestImages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CertificationValidityTest extends IntegrationTest {

    @Autowired CertificationRequestRepository requestRepository;
    @Autowired CertificationDatesMigration migration;

    private ResultActions requestCertification(Worker w) throws Exception {
        return mvc.perform(get("/account/worker/request-certification").header("Authorization", bearer(w)));
    }

    private ResultActions sendPhoto(Worker w) throws Exception {
        return mvc.perform(multipart("/account/worker/certification-photo")
                .file(new MockMultipartFile("file", "id.jpg", "image/jpeg", TestImages.jpeg(300, 300)))
                .header("Authorization", bearer(w)));
    }

    private ResultActions review(Admin admin, Worker w, boolean approved) throws Exception {
        CertificationRequest req = tx.execute(s -> requestRepository.findByWorker(reload(w)).orElseThrow());
        return mvc.perform(post("/admin/profiles/verify-certification").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("requestId", req.getId(), "approved", approved, "comment", "test"))));
    }

    /** Gallery badge of a worker. */
    private ResultActions galleryBadge(Worker w, boolean expected) throws Exception {
        return mvc.perform(get("/workers")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + w.getId() + "')].isCertified").value(expected));
    }

    /** Certified through the real flow, then moved back in time as if approved {@code monthsAgo} months ago. */
    private Worker certifiedThroughFlow(Admin admin, String email, int monthsAgo) throws Exception {
        Worker w = createVisibleWorker(email);
        requestCertification(w).andExpect(status().isOk());
        sendPhoto(w).andExpect(status().isOk());
        review(admin, w, true).andExpect(status().isOk());
        Worker saved = reload(w);
        saved.certify(LocalDateTime.now().minusMonths(monthsAgo), 8, 2);
        return workerRepository.save(saved);
    }

    private void setSettings(Admin admin, String values) throws Exception {
        mvc.perform(put("/admin/settings").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"values\":" + values + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Approval fixes the end and renewal dates from the settings; a later change of the settings does not move them")
    void datesFixedAtApproval() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        setSettings(admin, "{\"CERTIFICATION_VALIDITY_MONTHS\":12,\"CERTIFICATION_RENEWAL_MONTHS\":3}");
        Worker w = createVisibleWorker("luna@venus.test");

        requestCertification(w).andExpect(status().isOk());
        sendPhoto(w).andExpect(status().isOk());
        LocalDateTime before = LocalDateTime.now();
        review(admin, w, true).andExpect(status().isOk());

        Worker saved = reload(w);
        assertThat(saved.isCertified()).isTrue();
        assertThat(saved.getCertificationExpiresAt()).isCloseTo(before.plusMonths(12), within());
        assertThat(saved.getCertificationRenewableFrom()).isCloseTo(before.plusMonths(9), within());

        setSettings(admin, "{\"CERTIFICATION_VALIDITY_MONTHS\":1,\"CERTIFICATION_RENEWAL_MONTHS\":0}");
        assertThat(reload(w).getCertificationExpiresAt()).isEqualTo(saved.getCertificationExpiresAt());

        // The advertiser gets both dates, to show them without computing anything
        mvc.perform(get("/account/worker/me").header("Authorization", bearer(w)))
                .andExpect(jsonPath("$.isCertified").value(true))
                .andExpect(jsonPath("$.certificationExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.certificationRenewableFrom").isNotEmpty());
    }

    private static org.assertj.core.data.TemporalUnitOffset within() {
        return new org.assertj.core.data.TemporalUnitWithinOffset(1, ChronoUnit.MINUTES);
    }

    @Test
    @DisplayName("A first request is stored with its code, so the page can show it; the admins see the same code; a renewal gets a new one")
    void codeShownAndKept() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        Worker w = createVisibleWorker("luna@venus.test");

        String body = requestCertification(w).andExpect(status().isOk())
                .andExpect(jsonPath("$.certificationRequest.status").value("PENDING_PHOTO"))
                .andReturn().getResponse().getContentAsString();
        String code = json.readTree(body).get("verificationCode").asString();
        assertThat(code).hasSize(5);
        assertThat(json.readTree(body).get("certificationRequest").get("verificationCode").asString()).isEqualTo(code);
        // Asking again keeps the same code (the advertiser may already have written it down)
        requestCertification(w).andExpect(jsonPath("$.certificationRequest.verificationCode").value(code));

        sendPhoto(w).andExpect(status().isOk());
        mvc.perform(get("/admin/certification-requests").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].verificationCode").value(code));
        review(admin, w, true).andExpect(status().isOk());

        // Renewal: a new code, on the page and for the admins
        Worker renewing = reload(w);
        renewing.certify(LocalDateTime.now().minusMonths(7), 8, 2);
        workerRepository.save(renewing);
        String renewal = json.readTree(requestCertification(w).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("certificationRequest").get("verificationCode").asString();
        assertThat(renewal).isNotEqualTo(code).hasSize(5);
        sendPhoto(w).andExpect(status().isOk());
        mvc.perform(get("/admin/certification-requests").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[0].verificationCode").value(renewal));
    }

    @Test
    @DisplayName("Before the renewal period a new request is refused; within it, the advertiser stays certified until the renewal is approved")
    void renewalWindow() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        Worker early = certifiedThroughFlow(admin, "early@venus.test", 1);
        requestCertification(early).andExpect(status().isConflict());
        sendPhoto(early).andExpect(status().isConflict());

        Worker w = certifiedThroughFlow(admin, "luna@venus.test", 7); // renewal opened a month ago
        // As stored (the database keeps microseconds; Linux clocks give nanoseconds)
        LocalDateTime oldEnd = reload(w).getCertificationExpiresAt();
        requestCertification(w).andExpect(status().isOk())
                .andExpect(jsonPath("$.certificationRequest.status").value("PENDING_PHOTO"))
                .andExpect(jsonPath("$.isCertified").value(true));
        sendPhoto(w).andExpect(status().isOk());
        galleryBadge(w, true);

        // A refused renewal leaves the current certification untouched
        review(admin, w, false).andExpect(status().isOk());
        assertThat(reload(w).isCertified()).isTrue();
        assertThat(reload(w).getCertificationExpiresAt()).isEqualTo(oldEnd);

        requestCertification(w).andExpect(status().isOk());
        sendPhoto(w).andExpect(status().isOk());
        review(admin, w, true).andExpect(status().isOk());
        assertThat(reload(w).getCertificationExpiresAt()).isAfter(oldEnd.plusMonths(6));
        CertificationStatus finalStatus = tx.execute(s -> requestRepository.findByWorker(reload(w)).orElseThrow().getStatus());
        assertThat(finalStatus).isEqualTo(CertificationStatus.CERTIFIED);
    }

    @Test
    @DisplayName("At its end date a certification stops counting by itself: badge gone, admins see it, it cannot be revoked, a new one can be requested")
    void expiresWithoutAnyJob() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        Worker w = certifiedThroughFlow(admin, "luna@venus.test", 9); // has a CERTIFIED request, but ended a month ago
        assertThat(w.isCertified()).isFalse();

        galleryBadge(w, false);
        mvc.perform(get("/admin/users").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$[?(@.id == '" + w.getId() + "')].certified").value(false))
                .andExpect(jsonPath("$[?(@.id == '" + w.getId() + "')].certificationExpiresAt").isNotEmpty());
        mvc.perform(post("/admin/profiles/revoke-certification").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("workerId", w.getId(), "reason", "x"))))
                .andExpect(status().isConflict());
        requestCertification(w).andExpect(status().isOk());
    }

    @Test
    @DisplayName("The end date is exclusive: certified one minute before, no longer one minute after")
    void boundaries() {
        Worker w = new Worker();
        LocalDateTime now = LocalDateTime.now();
        w.certify(now.minusMonths(8).plusMinutes(1), 8, 2);
        assertThat(w.isCertified()).isTrue();
        assertThat(w.canRequestCertification()).isTrue(); // within the renewal period
        w.certify(now.minusMonths(8).minusMinutes(1), 8, 2);
        assertThat(w.isCertified()).isFalse();
        w.certify(now.minusMonths(6).plusMinutes(1), 8, 2);
        assertThat(w.canRequestCertification()).isFalse(); // renewal opens in a minute
        w.certify(now, 8, 0); // no renewal period: only once expired
        assertThat(w.canRequestCertification()).isFalse();
        w.clearCertification();
        assertThat(w.isCertified()).isFalse();
        assertThat(w.canRequestCertification()).isTrue();
    }

    @Test
    @DisplayName("Certifications approved before end dates were stored get the 8-month rule they were approved under")
    void legacyCertificationsMigrated() throws Exception {
        Admin admin = createAdmin("admin@venus.test");
        setSettings(admin, "{\"CERTIFICATION_VALIDITY_MONTHS\":12}"); // must not apply to old certifications
        Worker w = workerRepository.findById(createVisibleWorker("luna@venus.test").getId()).orElseThrow();
        LocalDateTime approved = LocalDateTime.now().minusMonths(3).truncatedTo(ChronoUnit.SECONDS);
        w.setCertifiedAt(approved);
        workerRepository.save(w);
        assertThat(reload(w).isCertified()).isFalse();

        migration.run(null);
        migration.run(null); // idempotent

        Worker migrated = reload(w);
        assertThat(migrated.getCertificationExpiresAt()).isEqualTo(approved.plusMonths(8));
        assertThat(migrated.getCertificationRenewableFrom()).isEqualTo(approved.plusMonths(6));
        assertThat(migrated.isCertified()).isTrue();
    }
}
