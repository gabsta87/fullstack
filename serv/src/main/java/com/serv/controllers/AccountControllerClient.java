package com.serv.controllers;

import com.serv.common.ReportStatus;
import com.serv.database.entities.*;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.CommentRepository;
import com.serv.database.repositories.ReportRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.ClientDTO;
import com.serv.dto.CommentDTO;
import com.serv.dto.GalleryFiltersDTO;
import com.serv.dto.ReportDTO;
import com.serv.requests.CommentRequest;
import com.serv.services.WorkerService;
import jakarta.validation.Valid;
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
    private final CommentRepository commentRepository;
    private final com.serv.services.AvailabilityAlertService availabilityAlertService;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ClientDTO> getMe(Client user) {
        Client managedClient = clientRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        return ResponseEntity.ok(ClientDTO.from(managedClient));
    }

    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, Client clientArg) {
        Worker worker = workerRepository.findById(workerId)
                .filter(Worker::isValid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found."));

        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));
        Client patchedUser = clientRepository.save(client);

        return ResponseEntity.ok(ClientDTO.from(patchedUser));
    }

    @GetMapping("/filters")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFilters(Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        return ResponseEntity.ok(GalleryFiltersDTO.from(client));
    }

    @PostMapping("/report-account")
    @Transactional
    public ResponseEntity<?> reportWorker(@Valid @RequestBody com.serv.requests.Report body, Client clientArg){
        Client c = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Worker w = workerRepository.findById(body.workerId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Targeted announcer not found")
        );

        Report r = reportRepository.findByWorkerAndAuthor(w, c).orElse(null);
        if(r == null){
            r = new Report(w, c, body.reason().trim());
            reportRepository.save(r);
        }else if(r.getStatus() == ReportStatus.PENDING){
            r.setReason(body.reason().trim());
        }else{
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already reported");
        }

        return ResponseEntity.ok(ReportDTO.from(r));
    }

    // "Notify me when back" alerts

    /** Ids of the profiles this visitor is waiting for. */
    @GetMapping("/availability-alerts")
    public List<UUID> availabilityAlerts(Client client) {
        return availabilityAlertService.watchedWorkerIds(client);
    }

    /** Only for a public, currently unavailable profile (409 if already available). Idempotent. */
    @PutMapping("/availability-alerts/{workerId}")
    public ResponseEntity<Void> addAvailabilityAlert(@PathVariable UUID workerId, Client clientArg) {
        Client client = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        Worker worker = workerRepository.findById(workerId)
                .filter(Worker::isValid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Targeted announcer not found"));
        availabilityAlertService.subscribe(client, worker);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/availability-alerts/{workerId}")
    public ResponseEntity<Void> removeAvailabilityAlert(@PathVariable UUID workerId, Client client) {
        availabilityAlertService.unsubscribe(client, workerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comment")
    @Transactional
    public ResponseEntity<?> writeComment(@Valid @RequestBody CommentRequest body, Client clientArg) {
        Client c = clientRepository.findById(clientArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // Comments can only target profiles that are publicly visible.
        Worker w = workerRepository.findById(body.workerId())
                .filter(Worker::isValid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Targeted announcer not found"));

        String content = body.content().trim();
        c.ensureUsername(); // a comment is always signed with a pseudonym

        Comment comm = commentRepository.findByAuthorAndWorker(c, w).orElse(null);

        if(comm == null){
            // New comment
            comm = new Comment(c, w, content);
            commentRepository.save(comm);
        }else{
            // Edit comment
            comm.setContent(content);
            comm.refreshDate();
            commentRepository.save(comm);
        }
        return ResponseEntity.ok(CommentDTO.from(comm));
    }

    @DeleteMapping("/comment/{commentId}")
    @Transactional
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, Client clientArg) {
        Comment comm = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (comm.getAuthor() == null || !comm.getAuthor().getId().equals(clientArg.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        clientArg.removeComment(comm);

        if (comm.getWorker() != null) {
            comm.getWorker().removeComment(comm);
        }

        commentRepository.delete(comm);

        return ResponseEntity.ok(ClientDTO.from(clientArg));
    }

}