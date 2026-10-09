package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.ClientDTO;
import com.serv.dto.GalleryFiltersDTO;
import com.serv.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account/client")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class AccountControllerClient {

    private final WorkerRepository workerRepository;
    private final WorkerService galleryService;
    private final ClientRepository clientRepository;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ClientDTO> getMe(Client user) {
        Client managedClient = clientRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
        return ResponseEntity.ok(ClientDTO.from(managedClient));
    }

    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateClientSettings(@RequestBody Requests.AccountDataRequest req, Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));

        if (req.username() != null) client.setUsername(req.username());
        if (req.email() != null) client.setEmail(new Email(req.email()));

        Client saved = clientRepository.save(client);
        return ResponseEntity.ok(ClientDTO.from(saved)); // 👈 Réponse HTTP directe, propre et synchrone
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, Client clientArg) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));

        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));

        boolean alreadyFavorite = client.getFavorites().stream()
                .anyMatch(w -> w.getId().equals(workerId));

        if (!alreadyFavorite) {
            client.getFavorites().add(worker);
            client = clientRepository.save(client);
        }

        return ResponseEntity.ok(ClientDTO.from(client));
    }

    @DeleteMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));
        Client patchedUser = clientRepository.save(client);

        return ResponseEntity.ok(ClientDTO.from(patchedUser));
    }

    @GetMapping("/filters")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFilters(Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));
        return ResponseEntity.ok(GalleryFiltersDTO.from(client));
    }
}