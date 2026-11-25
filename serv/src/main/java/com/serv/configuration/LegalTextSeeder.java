package com.serv.configuration;

import com.serv.database.entities.LegalText;
import com.serv.database.repositories.LegalTextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inserts the first version of the public legal texts ({@code resources/legal/<KEY>.<lang>.txt})
 * when the database has none. Later versions are added by the admins, never overwritten here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegalTextSeeder implements ApplicationRunner {

    static final List<String> LANGUAGES = List.of("fr", "en", "de", "es", "pt", "it", "ru", "ua", "ar");

    private final LegalTextRepository legalTextRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        seed(LegalText.ADULT_NOTICE);
    }

    void seed(String key) throws IOException {
        if (legalTextRepository.existsByName(key)) return;
        for (String language : LANGUAGES) {
            ClassPathResource file = new ClassPathResource("legal/" + key + "." + language + ".txt");
            LegalText text = new LegalText();
            text.setName(key);
            text.setLanguage(language);
            text.setContent(file.getContentAsString(StandardCharsets.UTF_8).strip());
            text.setVersion(1);
            text.setLastUpdate(LocalDateTime.now());
            legalTextRepository.save(text);
        }
        log.info("Legal text {} created in {} languages", key, LANGUAGES.size());
    }
}
