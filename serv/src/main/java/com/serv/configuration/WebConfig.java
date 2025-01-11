package com.serv.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST")// Allow necessary HTTP methods
//                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")// Allow necessary HTTP methods
                        .allowedHeaders("*")  // Allow headers for requests
//                        .allowedHeaders("Content-Type", "Authorization")  // Allow headers for requests
//                        .allowCredentials(false) // If app needs cookies/auth, enable this
                        .maxAge(3600) // Cache the response for 1 hour
                ;
            }
        };
    }
}