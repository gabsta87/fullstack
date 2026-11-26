package com.serv.features;

import com.serv.configuration.WorkerChangeNotifier;
import com.serv.database.entities.Worker;
import com.serv.services.SseStreamService;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkerChangeNotifierTest extends IntegrationTest {

    @MockitoSpyBean SseStreamService sseStreamService;

    @Test
    @DisplayName("Admins are told which profile changed after a successful change by the advertiser, never on reads or failures")
    void adminsToldAfterOwnChange() throws Exception {
        Worker w = createVisibleWorker("luna@venus.test");

        mvc.perform(get("/account/worker/me").header("Authorization", bearer(w))).andExpect(status().isOk());
        mvc.perform(patch("/account/worker/availability").header("Authorization", bearer(w))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verify(sseStreamService, never()).broadcastAdminEvent(eq(WorkerChangeNotifier.EVENT), any());

        mvc.perform(patch("/account/worker/availability").header("Authorization", bearer(w))
                .contentType(MediaType.APPLICATION_JSON).content("{\"available\":false}")).andExpect(status().isOk());
        verify(sseStreamService, times(1)).broadcastAdminEvent(WorkerChangeNotifier.EVENT, w.getId().toString());
    }
}
