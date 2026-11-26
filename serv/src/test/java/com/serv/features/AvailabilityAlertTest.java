package com.serv.features;

import com.serv.database.entities.Client;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.AvailabilityAlertRepository;
import com.serv.support.IntegrationTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityAlertTest extends IntegrationTest {

    @Autowired AvailabilityAlertRepository alertRepository;

    private Worker unavailableWorker(String email) {
        Worker w = workerRepository.findById(createVisibleWorker(email).getId()).orElseThrow();
        w.setAvailable(false);
        return workerRepository.save(w);
    }

    private void subscribe(Client c, Worker w, int expectedStatus) throws Exception {
        mvc.perform(put("/account/client/availability-alerts/" + w.getId()).header("Authorization", bearer(c))
                .header("Accept-Language", "fr")).andExpect(status().is(expectedStatus));
    }

    private void setAvailable(Worker w, boolean available) throws Exception {
        mvc.perform(patch("/account/worker/availability").header("Authorization", bearer(w))
                .contentType(MediaType.APPLICATION_JSON).content("{\"available\":" + available + "}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A visitor waits for an unavailable profile; one email in their language when it is back, then the alert ends")
    void notifiedOnceWhenBack() throws Exception {
        Client c = createClient("client@venus.test");
        Worker w = unavailableWorker("luna@venus.test");

        subscribe(c, w, 204);
        subscribe(c, w, 204); // idempotent
        mvc.perform(get("/account/client/availability-alerts").header("Authorization", bearer(c)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(w.getId().toString()));

        setAvailable(w, true);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo(w.getUsername() + " est de retour sur Venus");
        assertThat(sent.getValue().getAllRecipients()[0].toString()).isEqualTo("client@venus.test");
        assertThat(alertRepository.count()).isZero();

        // Next return: nobody is waiting any more
        setAvailable(w, false);
        setAvailable(w, true);
        verify(mailSender, after(500).times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("No email after unsubscribing, nor when the profile comes back while not public")
    void noEmail() throws Exception {
        Client c = createClient("client@venus.test");
        Worker w = unavailableWorker("luna@venus.test");
        subscribe(c, w, 204);
        mvc.perform(delete("/account/client/availability-alerts/" + w.getId()).header("Authorization", bearer(c)))
                .andExpect(status().isNoContent());
        setAvailable(w, true);

        Worker hidden = unavailableWorker("mira@venus.test");
        subscribe(c, hidden, 204);
        Worker fresh = workerRepository.findById(hidden.getId()).orElseThrow();
        fresh.setHidden(true);
        workerRepository.save(fresh);
        setAvailable(hidden, true);

        verify(mailSender, after(500).never()).send(any(MimeMessage.class));
        assertThat(alertRepository.count()).isEqualTo(1); // kept for a later, public return
    }

    @Test
    @DisplayName("Alerts only target public, unavailable profiles")
    void invalidTargets() throws Exception {
        Client c = createClient("client@venus.test");
        subscribe(c, createVisibleWorker("available@venus.test"), 409);
        subscribe(c, createWorker("incomplete@venus.test"), 404);
        Worker banned = unavailableWorker("banned@venus.test");
        Worker fresh = workerRepository.findById(banned.getId()).orElseThrow();
        fresh.setBanned(true);
        workerRepository.save(fresh);
        subscribe(c, banned, 404);
        mvc.perform(put("/account/client/availability-alerts/" + UUID.randomUUID()).header("Authorization", bearer(c)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/account/client/availability-alerts/not-a-uuid").header("Authorization", bearer(c)))
                .andExpect(status().isBadRequest());
        assertThat(alertRepository.count()).isZero();
    }

    @Test
    @DisplayName("Each visitor only sees and removes their own alerts")
    void ownAlertsOnly() throws Exception {
        Client owner = createClient("owner@venus.test");
        Client other = createClient("other@venus.test");
        Worker w = unavailableWorker("luna@venus.test");
        subscribe(owner, w, 204);

        mvc.perform(get("/account/client/availability-alerts").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/account/client/availability-alerts/" + w.getId()).header("Authorization", bearer(other)))
                .andExpect(status().isNoContent());
        assertThat(alertRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deleting an account deletes its alerts")
    void deletedWithAccounts() throws Exception {
        Client c = createClient("client@venus.test");
        Worker w = unavailableWorker("luna@venus.test");
        subscribe(c, w, 204);
        mvc.perform(delete("/admin/users/" + w.getId()).header("Authorization", bearer(createAdmin("admin@venus.test"))))
                .andExpect(status().isOk());
        assertThat(alertRepository.count()).isZero();
    }
}
