package com.serv.controller;

import com.serv.database.Email;
import com.serv.database.PasswordResetToken;
import com.serv.database.VenusUser;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MailService mailService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /** LOGIN */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Requests.LoginRequest request, HttpSession session) {
        Optional<VenusUser> clientOpt = userRepository.findByUsername(request.getPseudo());

        if (clientOpt.isPresent() && clientOpt.get().checkPassword(request.getPassword())) {
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
        Optional<VenusUser> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");

        }
        VenusUser user = userOpt.get();

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

                VenusUser user = resetTokenOpt.get().getUser();
                user.setPassword(newPassword);
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
