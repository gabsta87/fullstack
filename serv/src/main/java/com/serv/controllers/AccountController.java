package com.serv.controllers;

import com.serv.common.PasswordPolicy;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.UserRepository;
import com.serv.dto.*;
import com.serv.requests.AccountData;
import com.serv.services.EmailVerificationService;
import com.serv.services.SseStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@lombok.extern.slf4j.Slf4j
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository           userRepository;
    private final SseStreamService         sseStreamService;
    private final EmailVerificationService emailVerificationService;
    private final JwtProvider              jwtProvider;
    private final com.serv.services.AccountNotificationService accountNotificationService;

    /** Response header carrying a fresh token after a change that revoked the previous ones. */
    public static final String NEW_TOKEN_HEADER = "X-Auth-Token";

    /**
     * Server-Sent Events stream of the connected user.
     * EventSource cannot send headers, so the token is read from {@code ?token=} by the security
     * filter, which applies the same checks as any other request (signature, ban, revocation).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(VenusUser user) {
        return sseStreamService.createStream(user.getId());
    }

    /**
     * PATCH /account/data
     * Update username, email, or password — works for all roles.
     * Requires the current password for email or password changes.
     */
    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateSettings(@Valid @RequestBody AccountData req,
                                            VenusUser user) {

        boolean isPasswordChanging = req.password() != null && !req.password().isEmpty();
        boolean isEmailChanging = req.email() != null && !req.email().isBlank()
                && !Email.normalize(req.email()).equals(user.getEmail().getValue());

        if (isPasswordChanging || isEmailChanging) {
            if (req.oldPassword() == null || req.oldPassword().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Old password is required for email or password change."));
            }

            if (!user.checkPassword(req.oldPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Wrong old password. Please try again."));
            }
        }

        if (isPasswordChanging && !PasswordPolicy.isValid(req.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", PasswordPolicy.MESSAGE));
        }

        if (isEmailChanging) {
            if (!Email.isValid(req.email())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format."));
            }
            if (userRepository.findByEmail(req.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already taken."));
            }
        }

        // A visitor signs reviews with this name: it must not be another account's (anyone could pose as them)
        String newUsername = req.username() == null ? null : req.username().trim();
        if (newUsername != null && !newUsername.isEmpty()) {
            if (newUsername.codePoints().anyMatch(Character::isISOControl)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid username."));
            }
            if (user instanceof Client && !newUsername.equalsIgnoreCase(user.getUsername())
                    && userRepository.existsByUsernameIgnoreCaseAndIdNot(newUsername, user.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken."));
            }
        }

        // All checks passed: apply the changes.
        if (req.username() != null) {
            String username = req.username().trim();
            if (username.isEmpty()) {
                // A visitor keeps a pseudonym: their comments must never be signed with nothing (or their email).
                user.setUsername(user instanceof Client ? Client.defaultPseudonym() : null);
            } else {
                user.setUsername(username);
            }
        }

        Email previousEmail = user.getEmail();
        if (isEmailChanging) {
            user.setEmail(new Email(req.email()));
            user.setLocked(true); // locked again until the new address is verified
            user.revokeTokens();
        }

        if (isPasswordChanging) {
            user.setPassword(req.password()); // also revokes existing tokens
        }

        VenusUser patchedUser = userRepository.save(user);
        if (isPasswordChanging) log.info("Password changed for account {}: other sessions revoked", user.getId());
        if (isEmailChanging) log.info("Email changed for account {}: verification required", user.getId());

        if (isEmailChanging) {
            emailVerificationService.createTokenAndSendEmail(patchedUser);
            accountNotificationService.emailChanged(previousEmail);
        }
        if (isPasswordChanging) {
            accountNotificationService.passwordChanged(patchedUser.getEmail());
        }

        Object dtoToSend = switch (patchedUser) {
            case Client client -> ClientDTO.from(client);
            case Worker worker -> WorkerFullProfileDTO.from(worker);
            case Admin admin   -> AdminDTO.from(admin);
            default            -> VenusUserDTO.from(patchedUser);
        };

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (isEmailChanging || isPasswordChanging) {
            response.header(NEW_TOKEN_HEADER, jwtProvider.generateToken(patchedUser));
        }
        return response.body(dtoToSend);
    }
}
