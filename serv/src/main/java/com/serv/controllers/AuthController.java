package com.serv.controllers;

import com.serv.common.PasswordPolicy;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.VenusUserDTO;
import com.serv.requests.Login;
import com.serv.requests.LoginResponse;
import com.serv.requests.Register;
import com.serv.services.AbuseProtectionService;
import com.serv.services.AuthService;
import com.serv.services.EmailVerificationService;
import com.serv.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private static final String RESET_REQUESTED_MESSAGE = "If that address is registered, a reset link has been sent.";

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService verificationTokenService;
    private final AbuseProtectionService abuseProtection;

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @Value("${app.registration.email-verification-required:true}")
    private boolean emailVerificationRequired;

    // ── Login / Logout ────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login loginRequest) {
        String email = loginRequest.email();

        if (abuseProtection.isLoginBlocked(email)) {
            log.warn("SECURITY login refused, too many failures for account {}", accountRef(email));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed attempts. Please try again later."));
        }

        VenusUser user = authService.verifyCredentials(email, loginRequest.password());

        if (user == null) {
            abuseProtection.recordLoginFailure(email);
            log.info("Login failed for account {}", accountRef(email));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }

        abuseProtection.recordLoginSuccess(email);

        if (user.isBanned()) {
            log.info("SECURITY login refused for banned account {}", user.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Your account has been banned."));
        }

        log.info("Login ok for account {} ({})", user.getId(), user.getRole());
        String jwtToken = jwtProvider.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(jwtToken, VenusUserDTO.from(user)));
    }

    /** Tokens are stateless: the client simply discards its token. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    /** Only reachable with a valid token for a non-banned account (enforced by the security filter). */
    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(VenusUser user) {
        return ResponseEntity.ok(VenusUserDTO.from(user));
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/client")
    @Transactional
    public ResponseEntity<?> registerClient(@Valid @RequestBody Register req) {
        return register(req, email -> clientRepository.save(new Client(email, req.password())));
    }

    @PostMapping("/register/worker")
    @Transactional
    public ResponseEntity<?> registerWorker(@Valid @RequestBody Register req) {
        return register(req, email -> {
            Worker worker = new Worker(email, req.password());
            worker.setBanned(false);
            worker.setExpired(false);
            return workerRepository.save(worker);
        });
    }

    private ResponseEntity<?> register(Register req, Function<Email, VenusUser> creator) {
        if (!Email.isValid(req.email()))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format."));

        if (!PasswordPolicy.isValid(req.password()))
            return ResponseEntity.badRequest().body(Map.of("error", PasswordPolicy.MESSAGE));

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already taken."));

        VenusUser user = creator.apply(new Email(req.email()));
        log.info("Account {} registered ({})", user.getId(), user.getRole());

        if (emailVerificationRequired) {
            user.setLocked(true);
            verificationTokenService.createTokenAndSendEmail(user);
        } else {
            user.setLocked(false);
        }

        String message = emailVerificationRequired
                ? "Registration successful. Please check your email to activate your account."
                : "Registration successful.";

        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        if (token.length() > 64) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }

        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }
        VerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link has expired.");
        }

        VenusUser user = verificationToken.getUser();
        log.info("Email verified for account {}", user.getId());
        user.setLocked(false);
        userRepository.save(user);

        verificationTokenRepository.deleteByUser(user);

        return ResponseEntity.ok("Account activated successfully. You can now log in.");
    }

    /** Account id for logs (never the email address, which is personal data). */
    private String accountRef(String email) {
        return userRepository.findByEmail(email).map(u -> u.getId().toString()).orElse("<unknown>");
    }

    // ── Password reset ────────────────────────────────────────────────────────

    /** Always answers the same way so the endpoint cannot be used to discover registered emails. */
    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("email") String email) {
        if (!Email.isValid(email)) {
            return ResponseEntity.ok(RESET_REQUESTED_MESSAGE);
        }
        if (!abuseProtection.tryPasswordResetRequest(email)) {
            log.warn("SECURITY password reset throttled for account {}", accountRef(email));
            return ResponseEntity.ok(RESET_REQUESTED_MESSAGE);
        }

        userRepository.findByEmail(email)
                .filter(user -> !user.isBanned())
                .ifPresent(user -> {
                    log.info("Password reset requested for account {}", user.getId());
                    passwordResetService.createTokenAndSendEmail(user, false);
                });

        return ResponseEntity.ok(RESET_REQUESTED_MESSAGE);
    }

    @PostMapping("/reset-password/confirm")
    @Transactional
    public ResponseEntity<String> confirmReset(@RequestParam("resetToken") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) {
            return ResponseEntity.badRequest().body(PasswordPolicy.MESSAGE);
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(token);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            return ResponseEntity.badRequest().body("Token expired.");
        }

        VenusUser user = resetToken.getUser();

        // Hashes the password and revokes every token issued before the reset.
        user.setPassword(newPassword);

        // Admin invitations are activated by setting the first password.
        if (user.isLocked() && user.isAdmin()) {
            user.setLocked(false);
        }

        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        log.info("Password reset completed for account {}: previous sessions revoked", user.getId());
        abuseProtection.recordLoginSuccess(user.getEmail().getValue());

        return ResponseEntity.ok("Password reset successful.");
    }
}
