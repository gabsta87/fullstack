package com.serv.controllers;

import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.VenusUserDTO;
import com.serv.requests.Login;
import com.serv.requests.LoginResponse;
import com.serv.requests.Register;
import com.serv.service.AuthService;
import com.serv.service.EmailVerificationService;
import com.serv.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService verificationTokenService;

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @Value("${app.registration.email-verification-required:true}")
    private boolean emailVerificationRequired;

    // ── Login / Logout ────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login loginRequest) {
        VenusUser user = authService.verifyCredentials(loginRequest.email(), loginRequest.password());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials."));
        }

        if (user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Your account has been banned."));
        }

        String jwtToken = jwtProvider.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(jwtToken, VenusUserDTO.from(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = jwt.getSubject();

        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.isBanned()) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Your account has been banned."));
                    }
                    // On autorise le session-check pour les locked afin que le front-end sache qu'il est connecté mais restreint

                    return ResponseEntity.ok(VenusUserDTO.from(user));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/client")
    @Transactional
    public ResponseEntity<?> registerClient(@RequestBody Register req) {
        if (!Email.isValid(req.email()))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format."));

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already taken."));

        // Le mot de passe sera automatiquement haché par l'entité Client/VenusUser
        Client client = new Client(new Email(req.email()), req.password());

        if (emailVerificationRequired) {
            client.setLocked(true);
            String activationUrl = verificationTokenService.createTokenAndSendEmail(client);
            System.out.println("Activation URL for client: " + activationUrl);
        } else {
            client.setLocked(false);
        }

        clientRepository.save(client);

        String message = emailVerificationRequired
                ? "Registration successful. Please check your email to activate your account."
                : "Registration successful.";

        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/register/worker")
    @Transactional
    public ResponseEntity<?> registerWorker(@RequestBody Register req) {
        if (!Email.isValid(req.email()))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format."));

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already taken."));

        // Le mot de passe sera automatiquement haché par l'entité Worker/VenusUser
        Worker worker = new Worker(new Email(req.email()), req.password());
        worker.setBanned(false);
        worker.setExpired(false);

        if (emailVerificationRequired) {
            worker.setLocked(true);
            String activationUrl = verificationTokenService.createTokenAndSendEmail(worker);
            System.out.println("Activation URL for worker: " + activationUrl);
        } else {
            worker.setLocked(false);
        }

        workerRepository.save(worker);

        String message = emailVerificationRequired
                ? "Registration successful. Please check your email to activate your account."
                : "Registration successful.";

        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link has expired.");
        }

        VenusUser user = verificationToken.getUser();
        user.setLocked(false);
        userRepository.save(user);

        verificationTokenRepository.deleteByUser(user);

        return ResponseEntity.ok("Account activated successfully. You can now log in.");
    }

    // ── Password reset ────────────────────────────────────────────────────────
    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("email") String email) {
        Optional<VenusUser> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
        }

        passwordResetService.createTokenAndSendEmail(userOpt.get(), false);

        return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password/confirm")
    @Transactional
    public ResponseEntity<String> confirmReset(@RequestParam("resetToken") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
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

        // Le setter user.setPassword() appelle automatiquement ENCODER.encode()
        user.setPassword(newPassword);

        if (user.isLocked() && user.isAdmin()) {
            user.setLocked(false);
        }

        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset successful.");
    }
}