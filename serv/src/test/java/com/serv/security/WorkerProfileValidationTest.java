package com.serv.security;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkerProfileValidationTest extends IntegrationTest {

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = createWorker("worker@venus.test");
    }

    private ResultActions patchProfile(String body) throws Exception {
        return mvc.perform(patch("/account/worker/profile").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions patchProfile(Map<String, ?> body) throws Exception {
        return patchProfile(toJson(body));
    }

    private Worker fresh() {
        return workerRepository.findById(worker.getId()).orElseThrow();
    }

    @Test
    @DisplayName("Valid fields are saved and trimmed")
    void validUpdate() throws Exception {
        patchProfile(Map.of("username", "  Luna  ", "phone", "+41791234567", "bodyType", "athletic",
                "birthdate", "1995-06-15", "description", "Hello")).andExpect(status().isOk());
        Worker w = fresh();
        assertThat(w.getUsername()).isEqualTo("Luna");
        assertThat(w.getPhone()).isEqualTo("+41791234567");
        assertThat(w.getBodyType().name()).isEqualTo("ATHLETIC");
        assertThat(w.getAge()).isGreaterThanOrEqualTo(18);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bodyType", "eyeColor", "hairColor"})
    @DisplayName("Unknown enum values give 400 instead of a server error")
    void invalidEnums(String field) throws Exception {
        patchProfile(Map.of(field, "PURPLE_UNICORN")).andExpect(status().isBadRequest());
        patchProfile(Map.of(field, "'; DROP TABLE users; --")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "phone", "+41 79 123", "0041-79-123", "+123456789012345678901", "<script>"})
    @DisplayName("Invalid phone numbers are rejected")
    void invalidPhone(String phone) throws Exception {
        patchProfile(Map.of("phone", phone)).andExpect(status().isBadRequest());
        assertThat(fresh().getPhone()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000-02-30", "2000-13-01", "31/02/2000", "1990/01/01", "yesterday", "0000-01-01", "1800-01-01"})
    @DisplayName("Impossible or malformed birthdates are rejected (no lenient date rolling)")
    void invalidBirthdate(String date) throws Exception {
        patchProfile(Map.of("birthdate", date)).andExpect(status().isBadRequest());
        assertThat(fresh().getBirthdate()).isNull();
    }

    @Test
    @DisplayName("A birthdate in the future is rejected instead of banning the account")
    void futureBirthdate() throws Exception {
        patchProfile(Map.of("birthdate", java.time.LocalDate.now().plusDays(1).toString())).andExpect(status().isBadRequest());
        assertThat(fresh().isBanned()).isFalse();
    }

    @Test
    @DisplayName("A minor's profile is deactivated and reported")
    void minorDeactivated() throws Exception {
        String birthdate = java.time.LocalDate.now().minusYears(16).toString();
        patchProfile(Map.of("birthdate", birthdate)).andExpect(status().isOk());
        Worker w = fresh();
        assertThat(w.isBanned()).isTrue();
        assertThat(w.isLocked()).isTrue();
        assertThat(w.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("Oversized text fields are rejected")
    void oversizedText() throws Exception {
        patchProfile(Map.of("username", "x".repeat(51))).andExpect(status().isBadRequest());
        patchProfile(Map.of("shortDescription", "x".repeat(46))).andExpect(status().isBadRequest());
        patchProfile(Map.of("description", "x".repeat(5001))).andExpect(status().isBadRequest());
        assertThat(fresh().getUsername()).isNull();
    }

    @Test
    @DisplayName("An invalid field rejects the whole update (no half-applied changes)")
    void atomicUpdate() throws Exception {
        patchProfile(Map.of("username", "Luna", "bodyType", "NOPE")).andExpect(status().isBadRequest());
        assertThat(fresh().getUsername()).isNull();
    }

    @Test
    @DisplayName("Unknown geographic zones and services are rejected")
    void unknownReferences() throws Exception {
        patchProfile(Map.of("geographicZoneId", 99999)).andExpect(status().isBadRequest());
        patchProfile(Map.of("services", java.util.List.of("does-not-exist"))).andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/updateservices").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("[99999]")).andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/updateservices").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("[null]")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A worker cannot use another worker's photo as main photo")
    void foreignMainPhoto() throws Exception {
        Worker other = createVisibleWorker("other@venus.test");
        Photo foreign = photoRepository.findByWorker(other).get(0);

        patchProfile(Map.of("mainPhotoId", foreign.getId().toString())).andExpect(status().isNotFound());
        mvc.perform(patch("/account/worker/photos/" + foreign.getId() + "/main").header("Authorization", bearer(worker)))
                .andExpect(status().isNotFound());
        assertThat(fresh().getMainPhoto()).isNull();
    }

    @Test
    @DisplayName("Malformed ids give 400, never 500")
    void malformedIds() throws Exception {
        patchProfile(Map.of("mainPhotoId", "not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/photos/not-a-uuid/main").header("Authorization", bearer(worker)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/photos/reorder").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("[\"not-a-uuid\"]")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A worker cannot reorder another worker's photos")
    void foreignReorder() throws Exception {
        Worker other = createVisibleWorker("other@venus.test");
        Photo foreign = photoRepository.findByWorker(other).get(0);
        mvc.perform(patch("/account/worker/photos/reorder").header("Authorization", bearer(worker))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(java.util.List.of(foreign.getId().toString()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Fields a worker must not control are ignored (mass assignment)")
    void massAssignment() throws Exception {
        patchProfile("{\"username\":\"Luna\",\"isBanned\":true,\"banned\":false,\"certifiedAt\":\"2030-01-01T00:00:00\","
                + "\"galleryPositionPriority\":999999,\"remainingDaysCredit\":9999,\"role\":\"ADMIN\",\"tokenVersion\":5}")
                .andExpect(status().isOk());
        Worker w = fresh();
        assertThat(w.getCertifiedAt()).isNull();
        assertThat(w.getGalleryPositionPriority()).isZero();
        assertThat(w.getRemainingDaysCredit()).isNull();
        assertThat(w.getTokenVersion()).isZero();
    }

    @Test
    @DisplayName("Availability and visibility require an explicit boolean")
    void togglesRequireValue() throws Exception {
        mvc.perform(patch("/account/worker/availability").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/visibility").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("{\"hidden\":\"yes\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/account/worker/visibility").header("Authorization", bearer(worker))
                .contentType(MediaType.APPLICATION_JSON).content("{\"hidden\":true}")).andExpect(status().isOk());
        assertThat(fresh().isHidden()).isTrue();
    }

    @Test
    @DisplayName("Spoken languages are validated (level 1-4, known language)")
    void spokenLanguages() throws Exception {
        String url = "/account/worker/spoken-languages";
        mvc.perform(post(url).header("Authorization", bearer(worker)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"languages\":[{\"language\":\"FR\",\"level\":9}]}")).andExpect(status().isBadRequest());
        mvc.perform(post(url).header("Authorization", bearer(worker)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"languages\":[{\"language\":\"KLINGON\",\"level\":2}]}")).andExpect(status().isBadRequest());
        mvc.perform(post(url).header("Authorization", bearer(worker)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"languages\":null}")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A certified worker cannot open a new certification request")
    void certifiedCannotRequestAgain() throws Exception {
        Worker w = fresh();
        w.setCertifiedAt(java.time.LocalDateTime.now());
        workerRepository.save(w);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/account/worker/request-certification")
                .header("Authorization", bearer(worker))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("No alert is sent to the authorities for a rejected (invalid) birthdate")
    void noAlertOnInvalidDate() throws Exception {
        patchProfile(Map.of("birthdate", "2999-01-01")).andExpect(status().isBadRequest());
        org.mockito.Mockito.verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}
