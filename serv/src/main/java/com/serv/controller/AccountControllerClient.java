package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.ClientDTO;
import com.serv.dto.GalleryFiltersDTO;
import com.serv.service.SseStreamService;
import com.serv.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/account/client")
@RequiredArgsConstructor
public class AccountControllerClient {

    private final WorkerRepository workerRepository;
    private final WorkerService galleryService;
    private final ClientRepository clientRepository;
    private final SseStreamService sseStreamService;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        Client user = jwtClient(jwt);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        return clientRepository.findById(user.getId())
                .map(client -> ResponseEntity.ok(ClientDTO.from(client)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateClientSettings(@RequestBody Requests.AccountDataRequest req,
                                                  @AuthenticationPrincipal Jwt jwt) {
        // 🎯 FIX : Utilisation du helper pour éviter le crash de Cast sauvage si c'est un Worker
        Client client = jwtClient(jwt);
        if (client == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        if (req.username() != null) client.setUsername(req.username());
        if (req.email() != null) client.setEmail(new Email(req.email()));

        // 🎯 FIX : On utilise le clientRepository pour obtenir directement un Client après sauvegarde
        Client saved = clientRepository.save(client);
        ClientDTO dto = ClientDTO.from(saved);

        sseStreamService.emitEvent(saved.getId(), "account-update", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, @AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);

        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Optional<Worker> foundWorker = workerRepository.findById(workerId);
        if (foundWorker.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Worker not found.");

        Worker worker = foundWorker.get();

        boolean alreadyFavorite = client.getFavorites().stream()
                .anyMatch(w -> w.getId().equals(workerId));

        if (!alreadyFavorite) {
            client.getFavorites().add(worker);
            // 🎯 FIX : Utilisation de clientRepository
            client = clientRepository.save(client);

            sseStreamService.emitEvent(client.getId(), "account-update", ClientDTO.from(client));
        }

        return ResponseEntity.ok(ClientDTO.from(client));
    }

    @DeleteMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, @AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));

        Client patchedUser = clientRepository.save(client);

        ClientDTO dto = ClientDTO.from(patchedUser);
        sseStreamService.emitEvent(patchedUser.getId(), "account-update", dto);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/filters")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFilters(@AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        return ResponseEntity.ok(GalleryFiltersDTO.from(client));
    }


    private Client jwtClient(Jwt jwt) {
        if (jwt == null) return null;

        String userIdStr = jwt.getClaimAsString("userId");
        if (userIdStr == null) return null;

        return clientRepository.findById(UUID.fromString(userIdStr)).orElse(null);
    }
}
