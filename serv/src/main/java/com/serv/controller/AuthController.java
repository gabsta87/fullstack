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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
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

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

        Client client = new Client(new Email(req.email()), req.password());
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

        Worker worker = new Worker(new Email(req.email()), req.password());
        worker.setDisabled(true);
        worker.setLocked(true);
        worker.setExpired(true);
        workerRepository.save(worker);
        return ResponseEntity.ok().build();
    }

    // ── Password reset ────────────────────────────────────────────────────────

    @Transactional
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(HttpServletRequest request,
                                                @RequestParam("email") String email) {
        Optional<VenusUser> userOpt = userRepository.findByEmail(email);

        // Always return 200 — don't leak whether the email is registered
        if (userOpt.isEmpty())
            return ResponseEntity.ok("If that address is registered, a reset link has been sent.");

        System.out.println("sending password reset email to " + email);
        System.out.println("Request from : "+request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort());
        System.out.println("Request URL : "+request.getRequestURL());

        VenusUser user = userOpt.get();

        passwordResetTokenRepository.deleteAllByUserId(user.getId());
        passwordResetTokenRepository.flush();

        UUID token = UUID.randomUUID();

        PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(myToken);

        String resetUrl = String.format("%s/reset-password?resetToken=%s", frontendUrl, token);

        String htmlContent = "<html><body>"
                + "<p>Bonjour,</p>"
                + "<p>Vous avez demandé la réinitialisation de votre mot de passe. "
                + "Veuillez cliquer sur le lien ci-dessous pour configurer un nouveau mot de passe (valable 24h) :</p>"
                + "<p style='margin: 20px 0;'>"
                + "  <a href='" + resetUrl + "' style='background-color: #3880ff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block;'>"
                + "    Réinitialiser mon mot de passe"
                + "  </a>"
                + "</p>"
                + "<p>Si le bouton ne fonctionne pas, vous pouvez copier-coller ce lien dans votre navigateur :<br>"
                + "<a href='" + resetUrl + "'>" + resetUrl + "</a></p>"
                + "<p>Cordialement,<br>L'équipe Venus</p>"
                + "</body></html>";     

        mailService.sendHtmlMessage(user.getEmail(),
                "Réinitialisation du mot de passe", htmlContent);

        return ResponseEntity.ok("If that address is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<String> confirmReset(@RequestParam("resetToken") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(token);

        System.out.println("Confirming password reset for token " + token);

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