package com.serv.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("dev") // Active ONLY with the dev profile
public class DevMediaWebConfig implements WebMvcConfigurer {

    @Value("${media.upload.base}")
    private String uploadBase;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Make sure the path ends with a slash
        String location = uploadBase.endsWith("/") ? uploadBase : uploadBase + "/";

        // An absolute directory needs the file: prefix
        if (!location.startsWith("file:")) {
            location = "file:" + location;
        }

        // Maps /media/** to the local media directory
        registry.addResourceHandler("/media/**")
                .addResourceLocations(location);
    }
}