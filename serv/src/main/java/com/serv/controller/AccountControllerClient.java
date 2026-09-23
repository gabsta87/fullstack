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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private final WorkerRepository workerRepository;
    @Autowired
    private final WorkerService galleryService;
    @Autowired
    private final ClientRepository clientRepository;
    @Autowired
    private final SseStreamService sseStreamService;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(Client user) {
        return clientRepository.findById(user.getId())
                .map(client -> ResponseEntity.ok(ClientDTO.from(client)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateClientSettings(@RequestBody Requests.AccountDataRequest req,
                                                  Client client) {

        if (req.username() != null) client.setUsername(req.username());
        if (req.email() != null) client.setEmail(new Email(req.email()));

        Client saved = clientRepository.save(client);
        ClientDTO dto = ClientDTO.from(saved);

        sseStreamService.emitEvent(saved.getId(), "account-update", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(Client client) {
        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, Client client) {

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
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, Client client) {

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));

        Client patchedUser = clientRepository.save(client);

        ClientDTO dto = ClientDTO.from(patchedUser);
        sseStreamService.emitEvent(patchedUser.getId(), "account-update", dto);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/filters")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFilters(Client client) {
        return ResponseEntity.ok(GalleryFiltersDTO.from(client));
    }
}
