package com.serv.controller;

import com.serv.database.entities.*;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.VenusUserDTO;
import com.serv.service.MailService;
import com.serv.service.SseStreamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final SseStreamService sseStreamService;

    // ── Login / Logout ────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Requests.LoginRequest request,
                                        HttpSession session) {
        Optional<VenusUser> userOpt = userRepository.findByUsername(request.getPseudo());

        if (userOpt.isPresent() && userOpt.get().checkPassword(request.getPassword())) {
            session.setAttribute("user", userOpt.get());
            return ResponseEntity.ok(VenusUserDTO.from(userOpt.get()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid credentials.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/session-check")
    public ResponseEntity<?> sessionCheck(HttpSession session) {
        Object user = session.getAttribute("user");

        if (user == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/client")
    public ResponseEntity<String> registerClient(@RequestBody Requests.RegisterRequest req) {
        try {
            if (userRepository.findByUsername(req.getUsername()).isPresent())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Username already taken.");

            // TODO check email

            // TODO check password

            Client client = new Client(req.getUsername(), new Email(req.getEmail()), req.getPassword());
            client.setLocked(true);
            clientRepository.save(client);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/worker")
    public ResponseEntity<String> registerWorker(@RequestBody Requests.RegisterRequest req) {
        try {
            if (userRepository.findByUsername(req.getUsername()).isPresent())
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Username already taken.");

            // TODO check email

            // TODO check password

            Worker worker = new Worker(req.getUsername(), new Email(req.getEmail()), req.getPassword());
            worker.setDisabled(true);
            worker.setLocked(true);
            worker.setExpired(true);
            workerRepository.save(worker);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Password reset ────────────────────────────────────────────────────────

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(HttpServletRequest request,
                                                @RequestParam("email") String email) {
        Optional<VenusUser> userOpt = userRepository.findByEmail(new Email(email));

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