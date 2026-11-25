package com.serv.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final VenusJwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Stateless bearer-token API: no cookie-based authentication, so CSRF does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny()))

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token."))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied."))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Payments are not part of the free version of the site.
                        .requestMatchers("/payment/**").denyAll()

                        // Public endpoints
                        .requestMatchers("/auth/session-check").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/common/**", "/workers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/media/**").permitAll()

                        // Role-restricted areas
                        .requestMatchers("/admin/**").hasAnyRole(ADMIN, SUPER_ADMIN)
                        .requestMatchers("/account/admin/**").hasAnyRole(ADMIN, SUPER_ADMIN)
                        .requestMatchers("/account/worker/**").hasRole("WORKER")
                        .requestMatchers("/account/client/**").hasRole("CLIENT")
                        .requestMatchers("/account/**").authenticated()

                        // Anything not listed above is closed.
                        .anyRequest().denyAll()
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required."))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied."))
                );

        return http.build();
    }

    /**
     * Reads the token from the Authorization header. The {@code ?token=} query parameter is only
     * accepted for Server-Sent Events streams, because EventSource cannot send headers; everywhere
     * else it would leak tokens into logs and browser history.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return (HttpServletRequest request) -> {
            String header = request.getHeader("Authorization");
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = header.substring(7).trim();
                return token.isEmpty() ? null : token;
            }
            if (isStreamRequest(request)) {
                String token = request.getParameter("token");
                return (token == null || token.isBlank()) ? null : token;
            }
            return null;
        };
    }

    private static boolean isStreamRequest(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) return false;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/account/stream") || path.equals("/admin/stream");
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://nas:8888",
                "http://venustest.synology.me",
                "https://venustest.synology.me"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(com.serv.controllers.AccountController.NEW_TOKEN_HEADER));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
