package com.serv.configuration;

import com.serv.common.UserRole;
import com.serv.database.entities.VenusUser;
import com.serv.database.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a cryptographically valid JWT into an authenticated principal, but only after checking
 * the current state of the account in the database:
 * <ul>
 *   <li>the account still exists,</li>
 *   <li>it is not banned,</li>
 *   <li>the token has not been revoked (token version matches).</li>
 * </ul>
 * Roles are never trusted from the token: they come from the stored account.
 */
@lombok.extern.slf4j.Slf4j
@Component
@RequiredArgsConstructor
public class VenusJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        VenusUser user = findUser(jwt)
                .orElseThrow(() -> new InvalidBearerTokenException("Unknown account."));

        if (user.isBanned()) {
            log.info("SECURITY token of banned account {} refused", user.getId());
            throw new InvalidBearerTokenException("Account banned.");
        }

        if (tokenVersionOf(jwt) != user.getTokenVersion()) {
            log.info("SECURITY revoked token of account {} refused", user.getId());
            throw new InvalidBearerTokenException("Token revoked.");
        }

        return new JwtAuthenticationToken(jwt, authoritiesOf(user.getRole()), user.getEmail().getValue());
    }

    private Optional<VenusUser> findUser(Jwt jwt) {
        String userId = jwt.getClaimAsString(JwtProvider.CLAIM_USER_ID);
        if (userId == null) {
            return Optional.empty();
        }
        try {
            Optional<VenusUser> user = userRepository.findById(UUID.fromString(userId));
            // The subject must still match: an email change invalidates older tokens.
            return user.filter(u -> u.getEmail() != null && u.getEmail().getValue().equals(jwt.getSubject()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static int tokenVersionOf(Jwt jwt) {
        Object claim = jwt.getClaims().get(JwtProvider.CLAIM_TOKEN_VERSION);
        if (claim instanceof Number n) {
            return n.intValue();
        }
        return claim == null ? 0 : -1;
    }

    static List<GrantedAuthority> authoritiesOf(UserRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role == UserRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }
}
