package com.serv.configuration;

import com.serv.database.entities.VenusUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateToken(VenusUser user) {
        Instant now = Instant.now();

        // 🎯 1. On crée explicitement le Header en spécifiant l'algorithme HMAC HS256
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        // 2. Tes claims habituels
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("venus-api")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(user.getEmail().toString())
                .claim("userId", user.getId().toString())
                .claim("role", "ROLE_" + user.getRole().toString())
                .build();

        // 🎯 3. On passe le HEADER ET les CLAIMS dans les paramètres de l'encodeur
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String extractEmail(String token) {
        // .decode(token) va automatiquement :
        // - Vérifier la signature HMAC avec ta clé secrète
        // - Vérifier que le token n'est pas expiré (expiresAt)
        // Si le token est invalide ou expiré, cela lève une JwtException
        org.springframework.security.oauth2.jwt.Jwt jwt = this.jwtDecoder.decode(token);

        return jwt.getSubject(); // Retourne l'email stocké dans le subject
    }
}