package com.serv.controller;

import com.serv.common.Requests;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.VenusUserDTO;
import com.serv.service.AuthService;
import com.serv.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository   userRepository;
    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final MailService      mailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthService authService;
    private final JwtProvider jwtProvider;

    // ── Login / Logout ────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Requests.LoginRequest loginRequest) {
        VenusUser user = authService.verifyCredentials(loginRequest.email(), loginRequest.password());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }

        String jwtToken = jwtProvider.generateToken(user);

        return ResponseEntity.ok(new Requests.LoginResponse(jwtToken, VenusUserDTO.from(user)));
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
                .map(user -> ResponseEntity.ok(VenusUserDTO.from(user)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/client")
    public ResponseEntity<String> registerClient(@RequestBody Requests.RegisterRequest req) {
        if(! Email.isValid(req.email()))
            return ResponseEntity.badRequest().body("Invalid email format.");

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email already taken.");

        // TODO check email

        // TODO check password

        Client client = new Client(req.email(), new Email(req.email()), req.password());
        client.setLocked(true);
        clientRepository.save(client);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/worker")
    public ResponseEntity<String> registerWorker(@RequestBody Requests.RegisterRequest req) {
        if(! Email.isValid(req.email()))
            return ResponseEntity.badRequest().body("Invalid email format.");

        if (userRepository.findByEmail(req.email()).isPresent())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email already taken.");

        // TODO check email

        // TODO check password

        Worker worker = new Worker(req.email(), new Email(req.email()), req.password());
        worker.setDisabled(true);
        worker.setLocked(true);
        worker.setExpired(true);
        workerRepository.save(worker);
        return ResponseEntity.ok().build();
    }

    // ── Password reset ────────────────────────────────────────────────────────

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(HttpServletRequest request,
                                                @RequestParam("email") String email) {
        Optional<VenusUser> userOpt = userRepository.findByEmail(email);

        // Always return 200 — don't leak whether the email is registered
        if (userOpt.isEmpty())
            return ResponseEntity.ok("If that address is registered, a reset link has been sent.");

        VenusUser user = userOpt.get();
        UUID token = UUID.randomUUID();
        passwordResetTokenRepository.save(new PasswordResetToken(token, user));

        String resetUrl = String.format("%s://%s:%d/resetPassword?token=%s",
                request.getScheme(), request.getServerName(), request.getServerPort(), token);

        mailService.sendFormattedMessage(user.getEmail(),
                "Réinitialisation du mot de passe (lien valable 24h)", resetUrl);

        return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
    }

    @PostMapping("/resetPassword/confirm")
    public ResponseEntity<String> confirmReset(@RequestParam("token") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(token);

        if (tokenOpt.isEmpty())
            return ResponseEntity.badRequest().body("Invalid token.");

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken); // clean up expired token
            return ResponseEntity.badRequest().body("Token expired.");
        }

        resetToken.getUser().setPassword(newPassword);
        userRepository.save(resetToken.getUser());
        passwordResetTokenRepository.delete(resetToken);
        return ResponseEntity.ok("Password reset successful.");
    }

}