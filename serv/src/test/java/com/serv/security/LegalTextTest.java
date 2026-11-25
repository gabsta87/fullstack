package com.serv.security;

import com.serv.configuration.LegalTextSeeder;
import com.serv.database.entities.Admin;
import com.serv.database.entities.LegalText;
import com.serv.database.repositories.LegalTextRepository;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LegalTextTest extends IntegrationTest {

    @Autowired private LegalTextSeeder seeder;
    @Autowired private LegalTextRepository legalTextRepository;

    private Admin admin;

    @BeforeEach
    void setUp() throws Exception {
        seeder.run(null);
        admin = createAdmin("admin@venus.test");
    }

    private org.springframework.test.web.servlet.ResultActions publish(Map<String, ?> body) throws Exception {
        return mvc.perform(post("/admin/legal").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    @Test
    @DisplayName("The adult notice exists in the 9 site languages and is readable anonymously")
    void seededAndPublic() throws Exception {
        assertThat(legalTextRepository.count()).isEqualTo(9);
        mvc.perform(get("/common/legal/ADULT_NOTICE").param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("fr"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.content").value(containsString("18 ans")));
    }

    @Test
    @DisplayName("Seeding twice never duplicates nor overwrites the texts")
    void seedIsIdempotent() throws Exception {
        seeder.run(null);
        assertThat(legalTextRepository.count()).isEqualTo(9);
    }

    @Test
    @DisplayName("An admin publishes a new version; the old one is kept as proof")
    void newVersion() throws Exception {
        publish(Map.of("key", LegalText.ADULT_NOTICE, "language", "de", "content", "Neue Fassung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(get("/common/legal/ADULT_NOTICE").param("lang", "de"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.content").value("Neue Fassung"));
        assertThat(legalTextRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("A text missing in a language falls back to English; an unknown text is 404")
    void fallbackAndNotFound() throws Exception {
        publish(Map.of("key", "TERMS", "language", "en", "content", "Terms")).andExpect(status().isOk());
        mvc.perform(get("/common/legal/TERMS").param("lang", "it"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("en"));
        mvc.perform(get("/common/legal/PRIVACY")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "EN", "fr-CH", "../etc", "fr'--"})
    @DisplayName("Invalid languages are rejected with 400")
    void invalidLanguage(String lang) throws Exception {
        mvc.perform(get("/common/legal/ADULT_NOTICE").param("lang", lang)).andExpect(status().isBadRequest());
        publish(Map.of("key", LegalText.ADULT_NOTICE, "language", lang, "content", "x")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"adult_notice", "A-B", "ADULT NOTICE", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("Invalid keys are rejected with 400")
    void invalidKey(String key) throws Exception {
        mvc.perform(get("/common/legal/" + key)).andExpect(status().isBadRequest());
        publish(Map.of("key", key, "language", "fr", "content", "x")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Empty or oversized content is rejected; clients cannot publish")
    void contentAndRights() throws Exception {
        publish(Map.of("key", "TERMS", "language", "fr", "content", " ")).andExpect(status().isBadRequest());
        publish(Map.of("key", "TERMS", "language", "fr", "content", "a".repeat(200_001))).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/legal").header("Authorization", bearer(createClient("c@venus.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("key", "TERMS", "language", "fr", "content", "x"))))
                .andExpect(status().isForbidden());
    }
}
