package com.serv.configuration;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // Clé secrète de test (doit faire au moins 256 bits pour HMAC-SHA256)
        String secret = "MaCleSecreteSuperSecuriseeDePlusDe32Caracteres!";
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(new com.nimbusds.jose.jwk.OctetSequenceKey.Builder(secretKey)
                        .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                        .build())
        ));

        JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).build();

        jwtProvider = new JwtProvider(jwtEncoder, jwtDecoder);
    }

    @Test
    @DisplayName("generateToken & extractEmail - Génère un token valide et extrait correctement l'email")
    void generateAndExtractToken_Success() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setEmail(new Email("user@venus.com"));

        String token = jwtProvider.generateToken(client);

        assertThat(token).isNotNull().isNotEmpty();

        String extractedEmail = jwtProvider.extractEmail(token);
        assertThat(extractedEmail).isEqualTo("user@venus.com");
    }

    @Test
    @DisplayName("extractEmail - Lève une exception si le token est corrompu ou invalide")
    void extractEmail_InvalidToken() {
        String invalidToken = "token.invalide.corrompu";

        assertThatThrownBy(() -> jwtProvider.extractEmail(invalidToken))
                .isInstanceOf(JwtException.class);
    }
}
