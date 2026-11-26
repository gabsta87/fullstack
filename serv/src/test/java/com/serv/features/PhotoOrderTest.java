package com.serv.features;

import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import com.serv.support.TestImages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PhotoOrderTest extends IntegrationTest {

    private List<String> photoIds(String url, Worker auth) throws Exception {
        var req = get(url);
        if (auth != null) req = req.header("Authorization", bearer(auth));
        JsonNode body = json.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<String> ids = new ArrayList<>();
        body.get("photos").forEach(p -> ids.add(p.get("id").asString()));
        return ids;
    }

    @Test
    @DisplayName("The order chosen in profile management is the order of the public profile")
    void publicProfileFollowsTheChosenOrder() throws Exception {
        Worker w = createVisibleWorker("luna@venus.test");
        for (int i = 0; i < 3; i++) {
            mvc.perform(multipart("/account/worker/media").header("Authorization", bearer(w))
                    .file(new MockMultipartFile("files", "p" + i + ".jpg", "image/jpeg", TestImages.jpeg(200 + i, 200))))
                    .andExpect(status().isOk());
        }
        List<String> order = photoIds("/account/worker/me", w);
        assertThat(order).hasSizeGreaterThanOrEqualTo(3);
        Collections.reverse(order);

        mvc.perform(patch("/account/worker/photos/reorder").header("Authorization", bearer(w))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(order))).andExpect(status().isOk());

        assertThat(photoIds("/workers/" + w.getId(), null)).containsExactlyElementsOf(order);
        assertThat(photoIds("/account/worker/me", w)).containsExactlyElementsOf(order);
    }
}
