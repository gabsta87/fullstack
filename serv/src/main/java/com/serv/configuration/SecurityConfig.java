package com.serv.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .formLogin(httpForm -> {
                httpForm.loginPage("/login").permitAll();
            })
//            .formLogin(AbstractHttpConfigurer::disable) // Disable form-based login
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/account", "/favorites").authenticated()
                    .anyRequest().anonymous()
//                    .requestMatchers("/auth/register", "/auth/login", "/auth/forgot-password").permitAll() // Public endpoints for register/login
            )
            .httpBasic(AbstractHttpConfigurer::disable); // Disable HTTP Basic authentication

        return http.build();
    }
}
