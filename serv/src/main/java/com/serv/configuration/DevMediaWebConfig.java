package com.serv.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("dev") // Cette configuration s'active UNIQUEMENT en mode dev
public class DevMediaWebConfig implements WebMvcConfigurer {

    @Value("${media.upload.base}")
    private String uploadBase;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // On s'assure que le chemin se termine par un slash
        String location = uploadBase.endsWith("/") ? uploadBase : uploadBase + "/";

        // Sous macOS/Linux, pour un dossier absolu, Spring a besoin du préfixe file:
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }

        // Associe l'URL /media/** au dossier physique de votre Mac
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location);
    }
}