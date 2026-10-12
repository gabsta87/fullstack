package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Report;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.ReportRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.ClientDTO;
import com.serv.dto.GalleryFiltersDTO;
import com.serv.dto.ReportDTO;
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
    private final ReportRepository reportRepository;

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

    @PostMapping("/report-account")
    @Transactional
    public ResponseEntity<?> reportWorker(@RequestBody Requests.ReportRequest body, Client clientArg){
        System.out.println("Report request received");
        Client c = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable"));

        Worker w = workerRepository.findById(UUID.fromString(body.workerId())).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker introuvable")
        );

        System.out.println("Report worker "+w.getUsername()+" by client "+c.getUsername()+" with reason "+body.reason());
        Report r;
        if(reportRepository.findByWorkerAndAuthor(w, c).isEmpty()){
            r = new Report(w, c, body.reason());
            reportRepository.save(r);
        }else{
            r = reportRepository.findByWorkerAndAuthor(w, c).get();
            r.setReason(body.reason());
        }

        return ResponseEntity.ok(ReportDTO.from(r));
    }

}