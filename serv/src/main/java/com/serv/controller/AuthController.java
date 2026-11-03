package com.serv.controller;

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

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository   userRepository;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }

        if(user.isBanned()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account has been banned.");
        }

        String jwtToken = jwtProvider.generateToken(user);

        return ResponseEntity.ok(new LoginResponse(jwtToken, VenusUserDTO.from(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
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
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account has been banned.");
                    }

                    return ResponseEntity.ok(VenusUserDTO.from(user));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/client")
    @Transactional
    public ResponseEntity<String> registerClient(@RequestBody Register req) {
        if (!Email.isValid(req.email()))
            return ResponseEntity.badRequest().body("Invalid email format.");

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email already taken.");

        Client client = new Client(new Email(req.email()), req.password());

        if (emailVerificationRequired) {
            client.setLocked(true);
            // Génère le token et envoie l'e-mail de validation
            String activationUrl = verificationTokenService.createTokenAndSendEmail(client);
            System.out.println("Activation URL for client: " + activationUrl);
        } else {
            client.setLocked(false);
        }

        clientRepository.save(client);

        String message = emailVerificationRequired
                ? "Registration successful. Please check your email to activate your account."
                : "Registration successful.";

        return ResponseEntity.ok(message);
    }

    @PostMapping("/register/worker")
    @Transactional
    public ResponseEntity<String> registerWorker(@RequestBody Register req) {
        if (!Email.isValid(req.email()))
            return ResponseEntity.badRequest().body("Invalid email format.");

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email already taken.");

        Worker worker = new Worker(new Email(req.email()), req.password());
        worker.setBanned(false);
        worker.setExpired(false);

        if (emailVerificationRequired) {
            worker.setLocked(true);
            // Génère le token et envoie l'e-mail de validation
            String activationUrl = verificationTokenService.createTokenAndSendEmail(worker);
            System.out.println("Activation URL for worker: " + activationUrl);
        } else {
            worker.setLocked(false);
        }

        workerRepository.save(worker);

        String message = emailVerificationRequired
                ? "Registration successful. Please check your email to activate your account."
                : "Registration successful.";

        return ResponseEntity.ok(message);
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        // 1. Rechercher le token en base
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        // 2. Vérifier si le token a expiré
        if (verificationToken.isExpired()) {
            verificationTokenRepository.delete(verificationToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Link has expired.");
        }

        // 3. Récupérer l'utilisateur associé et le déverrouiller
        VenusUser user = verificationToken.getUser();
        user.setLocked(false);
        userRepository.save(user);

        // 4. Nettoyer TOUS les tokens de cet utilisateur (ce qui supprime aussi celui qui vient d'être utilisé)
        verificationTokenRepository.deleteByUser(user);

        return ResponseEntity.ok("Account activated successfully. You can now log in.");
    }

    // ── Password reset ────────────────────────────────────────────────────────
    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("email") String email) {
        Optional<VenusUser> userOpt = userRepository.findByEmail(email);

        // Retourne toujours 200 pour éviter de fuiter la présence d'un email
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
        }

        VenusUser user = userOpt.get();

        passwordResetService.createTokenAndSendEmail(user, false);

        return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password/confirm")
    @Transactional // Préférable d'ajouter @Transactional ici car on modifie et supprime en BDD
    public ResponseEntity<String> confirmReset(@RequestParam("resetToken") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(token);

        System.out.println("Confirming password reset for token " + token);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken); // clean up expired token
            return ResponseEntity.badRequest().body("Token expired.");
        }

        VenusUser user = resetToken.getUser();

        // 1. Mise à jour du mot de passe
        user.setPassword(newPassword); // N'oublie pas d'encoder avec BCrypt ici si applicable !

        // 2. Déverrouillage automatique si le compte Admin était bloqué/en attente
        if (user.isLocked() && user.isAdmin()) {
            user.setLocked(false);
        }

        // 3. Sauvegarde et nettoyage du token
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset successful.");
    }

}