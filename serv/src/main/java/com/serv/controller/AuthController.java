package com.serv.controller;

import com.serv.database.Client;
import com.serv.database.Email;
import com.serv.database.PasswordResetToken;
import com.serv.database.User;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public AuthController(UserRepository userRepository, MailService mailService, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /** REGISTER NEW USER */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Requests.RegisterRequest request) {
        if (userRepository.findByUsername(request.getPseudo()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken.");
        }

        Client client = new Client();
        client.setUsername(request.getPseudo());
        client.setEmail(new Email(request.getEmail()));
        client.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(client);
        return ResponseEntity.ok("User registered successfully!");
    }

    /** LOGIN */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Requests.LoginRequest request, HttpSession session) {
        Optional<User> clientOpt = userRepository.findByUsername(request.getPseudo());

        if (clientOpt.isPresent() && passwordEncoder.matches(request.getPassword(), clientOpt.get().getPasswordHash())) {
            session.setAttribute("user", clientOpt.get());
            return ResponseEntity.ok("Login successful!");
        }
        return ResponseEntity.status(401).body("Invalid credentials.");
    }

    /** LOGOUT */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("Logged out successfully.");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<String> resetPassword(HttpServletRequest request,
                                         @RequestParam("email") Email userEmail)  {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");

        }
        User user = userOpt.get();

        UUID token = UUID.randomUUID();
        passwordResetTokenRepository.save(new PasswordResetToken(token, user));

        String resetUrl = String.format("%s://%s:%d/resetPassword?token=%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                token
        );
        mailService.sendFormattedMessage(user.getEmail(),"Password reset (link valid for 1 day)",resetUrl);
        return ResponseEntity.ok("Reset link sent to mail");
    }

    @PostMapping("/resetPassword/confirm")
    public ResponseEntity<String> confirmReset(@RequestParam("token") UUID token,
                                               @RequestParam("newPassword") String newPassword) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findById(token);

        // Token exists and has not expired
        if (resetTokenOpt.isPresent()) {
            if(resetTokenOpt.get().getExpiryDate().isAfter(LocalDateTime.now())){

                User user = resetTokenOpt.get().getUser();
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                passwordResetTokenRepository.delete(resetTokenOpt.get());

                return ResponseEntity.ok("Password reset successful.");
            }else{
                return ResponseEntity.badRequest().body("Outdated token.");
            }
        } else {
            return ResponseEntity.badRequest().body("Invalid token.");
        }
    }

}
