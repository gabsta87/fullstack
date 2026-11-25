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

    public static final String CLAIM_USER_ID = "userId";
    /** Incremented on the user to revoke every token issued before (password change, ban...). */
    public static final String CLAIM_TOKEN_VERSION = "tv";

    private final JwtEncoder jwtEncoder;

    /** Token lifetime (app.jwt.expiration, ISO-8601 or Spring duration such as 30d). */
    @org.springframework.beans.factory.annotation.Value("${app.jwt.expiration:30d}")
    private java.time.Duration expiration = java.time.Duration.ofDays(30);
    private final JwtDecoder jwtDecoder;

    public String generateToken(VenusUser user) {
        Instant now = Instant.now();

        // 1. Explicit header with the HMAC HS256 algorithm
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        // 2. Tes claims habituels
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("venus-api")
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .subject(user.getEmail().toString())
                .claim(CLAIM_USER_ID, user.getId().toString())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .build();

        // 3. Encode header and claims
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String extractEmail(String token) {
        // .decode(token) va automatiquement :
        // - checks the HMAC signature with the secret key
        // - checks that the token has not expired (expiresAt)
        // An invalid or expired token throws a JwtException
        org.springframework.security.oauth2.jwt.Jwt jwt = this.jwtDecoder.decode(token);

        return jwt.getSubject(); // The email is stored in the subject
    }
}