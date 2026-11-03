package com.serv.controller;

import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.UserRepository;
import com.serv.dto.*;
import com.serv.requests.AccountData;
import com.serv.service.EmailVerificationService;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.springframework.util.ClassUtils.isPresent;

@RestController
@RequestMapping("/account")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository       userRepository;
    private final SseStreamService     sseStreamService;
    private final EmailVerificationService emailVerificationService;
    private final JwtProvider          jwtProvider;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token) {
        try {
            String email = jwtProvider.extractEmail(token);

            VenusUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user."));

            return sseStreamService.createStream(user.getId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.", e);
        }
    }

    /**
     * PATCH /account/data
     * Update username, email, or password — works for all roles.
     * Requires old password for email or password changes.
     */
    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody AccountData req,
                                            VenusUser user) {

        // 1. Déterminer si l'ancien mot de passe est requis
        boolean isPasswordChanging = req.password() != null && !req.password().isBlank();
        boolean isEmailChanging = req.email() != null && !req.email().isBlank() && !req.email().equals(user.getEmail().toString());

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

        // 2. Mise à jour du username (si présent)
        if (req.username() != null) {
            user.setUsername(req.username());
        }

        boolean emailChanged = false;

        // 3. Mise à jour de l'email (si présent et différent)
        if (isEmailChanging) {
            if (!Email.isValid(req.email())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format."));
            }
            if (userRepository.findByEmail(req.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already taken."));
            }

            user.setEmail(new Email(req.email()));
            user.setLocked(true); // Sécurité : on reverrouille jusqu'à validation du nouvel e-mail
            emailChanged = true;
        }

        // 4. Mise à jour du mot de passe (si présent)
        if (isPasswordChanging) {
            user.setPassword(req.password()); // Ton entité gère le hachage en interne ou via setter
        }

        // 5. Sauvegarde en base
        VenusUser patchedUser = userRepository.save(user);

        // 6. Si l'email a changé, on déclenche l'envoi du token de vérification
        if (emailChanged) {
            emailVerificationService.createTokenAndSendEmail(patchedUser);
        }

        // 7. Retour du DTO adapté au rôle (ton switch d'origine)
        Object dtoToSend = switch (patchedUser) {
            case Client client -> ClientDTO.from(client);
            case Worker worker -> WorkerFullProfileDTO.from(worker);
            case Admin admin   -> AdminDTO.from(admin);
            default            -> VenusUserDTO.from(patchedUser);
        };

        return ResponseEntity.ok(dtoToSend);
    }
}