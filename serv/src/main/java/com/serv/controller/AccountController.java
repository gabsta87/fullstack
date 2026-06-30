package com.serv.controller;

import com.serv.common.Language;
import com.serv.common.Requests;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.UserRepository;
import com.serv.dto.ClientDTO;
import com.serv.dto.VenusUserDTO;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

@RestController
@RequestMapping("/account")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository       userRepository;
    private final SseStreamService     sseStreamService;
    private final JwtProvider          jwtProvider;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("token") String token) {
        try {
            // 1. Extraction de l'email contenu dans le sujet du JWT
            String email = jwtProvider.extractEmail(token);

            // 2. Recherche de l'utilisateur en BDD grâce à cet email
            VenusUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));

            // 3. On passe l'UUID technique au SseStreamService pour l'enregistrement du flux
            return sseStreamService.createStream(user.getId());

        } catch (Exception e) {
            // 🔥 TRÈS IMPORTANT : Si le token est expiré ou invalide, on renvoie un 401.
            // Cela va déclencher le bloc "EventSource.CLOSED" côté Angular pour déconnecter proprement l'utilisateur.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide ou expiré.", e);
        }
    }

    /**
     * PATCH /account/data
     * Update username, email, or password — works for both roles.
     */
    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody Requests.AccountDataRequest req,
                                            @AuthenticationPrincipal Jwt jwt) {
        VenusUser user = jwtUser(jwt);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 1. Mise à jour des champs communs
        if (req.username() != null) user.setUsername(req.username());
        if (req.email() != null) user.setEmail(new Email(req.email()));
        if (req.password() != null) user.setPassword(req.password());

        VenusUser patchedUser = userRepository.save(user);

        // 2. Détection dynamique du type pour envoyer le BON DTO complet 🎯
        Object dtoToSend;
        if (patchedUser instanceof Client client) {
            dtoToSend = ClientDTO.from(client); // Contient les favoris !
        } else if (patchedUser instanceof Worker worker) {
            dtoToSend = WorkerFullProfileDTO.from(worker); // Contient la galerie, les services, etc.
        } else {
            dtoToSend = VenusUserDTO.from(patchedUser); // Cas de secours
        }

        // 3. On émet le DTO spécifique dans le SSE
        sseStreamService.emitEvent(patchedUser.getId(), "account-update", dtoToSend);

        return ResponseEntity.ok(dtoToSend);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VenusUser jwtUser(Jwt jwt) {
        if (jwt == null) return null;
        // 🎯 jwt.getSubject() contient désormais l'email
        return userRepository.findByEmail(jwt.getSubject()).orElse(null);
    }
}