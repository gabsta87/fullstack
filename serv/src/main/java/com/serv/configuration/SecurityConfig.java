package com.serv.configuration;

import com.serv.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthService authService;

    // 1. REMISE EN PLACE DES BEANS INITIALEMENT PRÉSENTS
    @Bean
    public UserDetailsService getUserDetailsService() {
        return authService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider getAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)

                // On bascule l'application en mode Stateless (Zéro session serveur)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // On configure le décodage du JWT et la gestion des erreurs de jeton
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))

                        .bearerTokenResolver(request -> {
                            // On regarde d'abord dans le header classique
                            String authorizationHeader = request.getHeader("Authorization");
                            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                                return authorizationHeader.substring(7);
                            }
                            // Si absent (comme pour le SSE), on regarde le paramètre "?token=..."
                            return request.getParameter("token");
                        })

                        // Si le Token est manquant, expiré ou corrompu, on renvoie le JSON personnalisé
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Token invalide ou expiré\"}");
                            response.getWriter().flush();
                        })
                )

                // Règles d'accès strictes à tes endpoints
                .authorizeHttpRequests(auth -> auth
                        // 🎯 1. Les exceptions spécifiques en PREMIER
                        // On autorise l'accès public technique à l'endpoint du stream pour que notre contrôleur lise le token de l'URL
                        .requestMatchers("/account/stream").permitAll()
                        .requestMatchers("/error", "/auth/**", "/session-check").permitAll()
                        .requestMatchers("/public/**", "/gallery/**", "/workers/**").permitAll()

                        // 🎯 2. Les règles restrictives globales en DEUXIÈME
                        // Tout le reste de la gestion de compte et des favoris nécessite d'être loggé (Header Bearer standard)
                        .requestMatchers("/account/**", "/favorites/**").authenticated()

                        // 🎯 3. Le gardien final en DERNIER (Une seule fois !)
                        // Par sécurité, toute route non listée au-dessus nécessite d'être authentifiée
                        .anyRequest().authenticated()
                )

                // EntryPoint global (sécurité additionnelle pour les autres rejets)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Accès refusé\"}");
                            response.getWriter().flush();
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache la réponse Preflight pendant 1 heure

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");

        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return jwtAuthenticationConverter;
    }
}