package com.serv.configuration;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserArgumentResolverTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserArgumentResolver resolver;

    @Mock
    private MethodParameter methodParameter;

    @BeforeEach
    void setUp() {
        // Nettoyage préalable du contexte de sécurité
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("supportsParameter - Accepte uniquement les sous-types de VenusUser")
    void supportsParameter() {
        when(methodParameter.getParameterType()).thenAnswer(inv -> Client.class);
        assertThat(resolver.supportsParameter(methodParameter)).isTrue();

        when(methodParameter.getParameterType()).thenAnswer(inv -> String.class);
        assertThat(resolver.supportsParameter(methodParameter)).isFalse();
    }

    @Test
    @DisplayName("resolveArgument - Retourne l'utilisateur connecté si tout est valide")
    void resolveArgument_Success() {
        Client client = new Client(new Email("client@venus.com"), "Pass123!");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("client@venus.com")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("client@venus.com")).thenReturn(Optional.of(client));
        when(methodParameter.getParameterType()).thenAnswer(inv -> Client.class);

        Object resolvedUser = resolver.resolveArgument(methodParameter, null, null, null);

        assertThat(resolvedUser).isNotNull();
        assertThat(resolvedUser).isInstanceOf(Client.class);
    }

    @Test
    @DisplayName("resolveArgument - Lève 403 Forbidden si le type d'utilisateur ne correspond pas à la ressource demandée")
    void resolveArgument_TypeMismatchForbidden() {
        Client client = new Client(new Email("client@venus.com"), "Pass123!");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("client@venus.com")
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("client@venus.com")).thenReturn(Optional.of(client));
        // Le contrôleur attend un Worker mais l'utilisateur connecté est un Client
        when(methodParameter.getParameterType()).thenAnswer(inv -> Worker.class);

        assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}