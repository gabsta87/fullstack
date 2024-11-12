package com.serv.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")  // Allow your Angular app
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")// Allow necessary HTTP methods
//                        .allowedHeaders("Content-Type", "Authorization")  // Allow headers for your requests
                        .allowedHeaders("*")  // Allow headers for your requests
                        .allowCredentials(true) // If your app needs cookies/auth, enable this
                        .maxAge(3600); // Cache the response for 1 hour (optional)
            }
        };
    }
}