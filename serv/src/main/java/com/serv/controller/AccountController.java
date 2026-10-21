package com.serv.controller;

import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.UserRepository;
import com.serv.dto.*;
import com.serv.requests.AccountData;
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
     */
    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody AccountData req,
                                            VenusUser user) {
        if (req.username() != null) user.setUsername(req.username());
        if (req.email() != null) user.setEmail(new Email(req.email()));

        if (req.password() != null && !req.password().isBlank()) {
            if (req.oldPassword() == null || req.oldPassword().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Old password is required for password change."));
            }

            if (!user.checkPassword(req.oldPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Wrong old password. Please try again."));
            }

            user.setPassword(req.password());
        }

        VenusUser patchedUser = userRepository.save(user);

        Object dtoToSend = switch (patchedUser) {
            case Client client -> ClientDTO.from(client);
            case Worker worker -> WorkerFullProfileDTO.from(worker);
            case Admin admin   -> AdminDTO.from(admin);
            default            -> VenusUserDTO.from(patchedUser);
        };

        return ResponseEntity.ok(dtoToSend);
    }
}