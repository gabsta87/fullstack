package com.serv.controller;

import com.serv.common.Language;
import com.serv.database.entities.*;
import com.serv.common.BodyType;
import com.serv.database.repositories.*;
import com.serv.dto.ClientDTO;
import com.serv.dto.VenusUserDTO;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.SseStreamService;
import com.serv.service.WorkerService;
import com.serv.service.MediaStorageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository       userRepository;
    private final WorkerRepository     workerRepository;
    private final ClientRepository     clientRepository;
    private final PhotoRepository      photoRepository;
    private final WorkerService galleryService;
    private final MediaStorageService  mediaStorageService;
    private final ServiceRepository    serviceRepository;
    private final SseStreamService     sseStreamService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VenusUser sessionUser(HttpSession session) {
        return (VenusUser) session.getAttribute("user");
    }

    private Worker sessionWorker(HttpSession session) {
        VenusUser u = sessionUser(session);
        return (u instanceof Worker w) ? w : null;
    }

    private Client sessionClient(HttpSession session) {
        VenusUser u = sessionUser(session);
        return (u instanceof Client c) ? c : null;
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(HttpSession session) {
        VenusUser user = sessionUser(session);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        // Utilisez l'id de l'utilisateur pour recharger une instance "fraîche" depuis la base
        // Cela permet à Hibernate de gérer l'objet dans la transaction actuelle

        switch (user.getRole()) {
            case WORKER:
                // On cherche par id dans le repository des Workers
                return workerRepository.findByIdWithPhotos(user.getId())
                        .map(worker -> ResponseEntity.ok(WorkerFullProfileDTO.from(worker)))
                        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

            case CLIENT:
                // Même logique pour les clients si besoin
                return clientRepository.findById(user.getId())
                        .map(client -> ResponseEntity.ok(ClientDTO.from(client)))
                        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

            case ADMIN:
                // ...
                break;
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/stream")
    public SseEmitter stream(HttpSession session) {
        VenusUser user = (VenusUser) session.getAttribute("user");

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in.");
        }
        return sseStreamService.createStream(user.getId());
    }

    @PatchMapping("/language")
    @Transactional
    public ResponseEntity<?> setLanguage(@RequestBody String language, HttpSession session) {
        VenusUser user = sessionUser(session);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");
        Language newLanguage;

        try{
            newLanguage = Language.valueOf(language);
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body("Invalid language code.");
        }

        user.setLanguage(newLanguage);
        userRepository.save(user);

        VenusUser patchedUser = userRepository.save(user);

        session.setAttribute("user", patchedUser);

        sseStreamService.emitEvent(patchedUser.getId(), "account-update", VenusUserDTO.from(patchedUser));

        return ResponseEntity.ok(patchedUser);
    }

    /**
     * PATCH /account/settings
     * Update username, email, or password — works for both roles.
     */
    @PatchMapping("/settings")
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> body,
                                            HttpSession session) {
        VenusUser user = sessionUser(session);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (body.containsKey("username") && !body.get("username").isBlank())
            user.setUsername(body.get("username"));
        if (body.containsKey("email") && !body.get("email").isBlank())
            user.setEmail(new Email(body.get("email")));
        if (body.containsKey("password") && !body.get("password").isBlank())
            user.setPassword(body.get("password"));

        VenusUser patchedUser = userRepository.save(user);

        session.setAttribute("user", patchedUser);

        sseStreamService.emitEvent(patchedUser.getId(), "account-update", VenusUserDTO.from(patchedUser));

        return ResponseEntity.ok(patchedUser);
    }

    // ── CLIENT ────────────────────────────────────────────────────────────────

    /**
     * GET /account/favorites
     * Returns favorite workers as gallery DTOs — reuses the same worker-card component.
     */
    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGallery(ids));
    }

    /** POST /account/favorites/{workerId} */
    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Optional<Worker> foundWorker = workerRepository.findById(workerId);

        if (foundWorker.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Worker not found.");

        client.getFavorites().add(foundWorker.get());
        userRepository.save(client);
        session.setAttribute("user", client);
        return ResponseEntity.ok().build();
    }

    /** DELETE /account/favorites/{workerId} */
    @DeleteMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));
        userRepository.save(client);
        session.setAttribute("user", client);
        return ResponseEntity.ok().build();
    }

    // ── WORKER ────────────────────────────────────────────────────────────────

    /** PATCH /account/worker/availability */
    @PatchMapping("/worker/availability")
    @Transactional
    public ResponseEntity<?> setAvailability(@RequestBody Map<String, Boolean> body,
                                             HttpSession session) {
        System.out.println("setAvailability: " + body + "");
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        worker.setAvailable(body.getOrDefault("available", false));
        Worker savedWorker = workerRepository.save(worker);
        session.setAttribute("user", savedWorker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);
        return ResponseEntity.ok().body(dto);
    }

    /** PATCH /account/worker/profile */
    @PatchMapping("/worker/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody WorkerProfileUpdateRequest req,
                                           HttpSession session) {
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        if (req.description() != null) worker.setDescription(req.description());
        if (req.location()    != null) worker.setLocation(req.location);
        if (req.region()      != null) worker.setRegion(req.region());
        if (req.bodyType()    != null) worker.setBodyType(BodyType.valueOf(req.bodyType()));
        if (req.eyeColor()    != null) worker.setEyeColor(req.eyeColor());
        if (req.hairColor()   != null) worker.setHairColor(req.hairColor());
        if (req.phone()       != null) worker.setPhone(req.phone());

        if (req.mainPhotoId() != null) {
            Optional<Photo> newPhoto = photoRepository.findById(UUID.fromString(req.mainPhotoId()));
            if (newPhoto.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Photo not found.");
            worker.setMainPhoto(newPhoto.get());
        }

        if (req.services()    != null) {
            worker.setServices(req.services().stream()
                    .map(serviceRepository::findByName)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        }

        Worker savedWorker = workerRepository.save(worker);
        session.setAttribute("user", savedWorker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);
        return ResponseEntity.ok().body(dto);
    }

    @Transactional
    @PatchMapping("/worker/updateservices")
    public ResponseEntity<?> updateServices(@RequestBody List<String> services, HttpSession session) {
        Worker worker = workerRepository.findByIdWithPhotos(sessionWorker(session).getId()).orElse(null);

        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        List<Service> serviceList = services.stream().map(serviceRepository::findByName).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        worker.setServices(serviceList);

        Worker savedWorker = workerRepository.save(worker);

        session.setAttribute("user", savedWorker);

        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);

        sseStreamService.emitEvent(worker.getId(), "account-update", dto);

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /account/worker/photos
     * Upload a new photo — generates the main thumb and preview thumb.
     */
    @PostMapping("/worker/photos")
    @Transactional
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "title", required = false) String title,
                                         HttpSession session) {

        Worker sessionWorker = sessionWorker(session);
        if (sessionWorker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Optional<Worker> workerOpt = workerRepository.findByIdWithPhotos(sessionWorker.getId());
        if (workerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Worker not found.");
        }
        Worker worker = workerOpt.get();

        System.out.println("Saving photo for worker " + worker.getId() + " with title:" + file.getOriginalFilename());

        try {
            MediaStorageService.SavedMedia saved = mediaStorageService.savePhoto(file, worker.getId());

            Photo photo = new Photo();
            photo.setWorker(worker);
            photo.setTitle(title);
            photo.setOriginalUrl(saved.originalUrl());
            photo.setMainThumbUrl(saved.mainThumbUrl());
            photo.setPreviewThumbUrl(saved.previewThumbUrl());

            photo.setSortOrder(worker.getPhotos().size());

            if (worker.getMainPhoto() == null) {
                worker.setMainPhoto(photo);
                session.setAttribute("user", worker);
            }


            worker.addPhoto(photo);
            photoRepository.save(photo);
            workerRepository.save(worker);

            session.setAttribute("user", worker);

            Worker savedWorker = workerRepository.findByIdWithPhotos(worker.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker introuvable"));

            sseStreamService.emitEvent(worker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

            return ResponseEntity.ok(Map.of(
                    "id",              photo.getId().toString(),
                    "mainThumbUrl",    photo.getMainThumbUrl(),
                    "previewThumbUrl", photo.getPreviewThumbUrl(),
                    "originalUrl",     photo.getOriginalUrl()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /account/worker/photos/{photoId}
     */
    @DeleteMapping("/worker/photos/{photoId}")
    @Transactional
    public ResponseEntity<?> deletePhoto(@PathVariable UUID photoId, HttpSession session) {
        Worker sessionWorker = sessionWorker(session);
        if (sessionWorker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");
        if (photoId == null) return ResponseEntity.badRequest().body("No file provided.");

        Optional<Worker> workerOpt = workerRepository.findByIdWithPhotos(sessionWorker.getId());
        if (workerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Worker not found.");
        }
        Worker worker = workerOpt.get();
        System.out.println("Deleting photo " + photoId + " for worker " + worker.getId());

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        // 1 — Suppression des fichiers physiques sur le disque (Original + Thumbs)
        mediaStorageService.deletePhotoFiles(
                worker.getId(),
                photo.getOriginalUrl(),
                photo.getMainThumbUrl(),
                photo.getPreviewThumbUrl()
        );

        // 2 — Si on supprime la photo principale, on choisit la suivante disponible
        if (worker.getMainPhoto() != null && worker.getMainPhoto().getId().equals(photoId)) {
            worker.getPhotos().stream()
                    .filter(p -> !p.getId().equals(photoId))
                    .findFirst()
                    .ifPresentOrElse(
                            worker::setMainPhoto,
                            () -> worker.setMainPhoto(null)
                    );
        }

        // 3 — Suppression en base de données (déclenché par l'orphanRemoval = true)
        worker.removePhoto(photo);
        workerRepository.save(worker);

        session.setAttribute("user", worker);

        Worker savedWorker = workerRepository.findByIdWithPhotos(worker.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker introuvable"));

        sseStreamService.emitEvent(worker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /account/worker/photos/{photoId}/main
     * Set a photo as the main (gallery card) photo.
     */
    @PatchMapping("/worker/photos/{photoId}/main")
    @Transactional
    public ResponseEntity<?> setMainPhoto(@PathVariable UUID photoId, HttpSession session) {
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        worker.setMainPhoto(photo);
        Worker savedWorker = workerRepository.save(worker);
        session.setAttribute("user", savedWorker);

        sseStreamService.emitEvent(worker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * PATCH /account/worker/photos/reorder
     * Accepts an ordered list of photo IDs and updates sortOrder accordingly.
     */
    @PatchMapping("/worker/photos/reorder")
    @Transactional
    public ResponseEntity<?> reorderPhotos(@RequestBody List<String> orderedIds,
                                           HttpSession session) {
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = UUID.fromString(orderedIds.get(i));
            photoRepository.findById(id).ifPresent(p -> {
                if (p.getWorker().getId().equals(worker.getId()))
                    p.setSortOrder(orderedIds.indexOf(id.toString()));
            });
        }
        return ResponseEntity.ok().build();
    }

    // ── Request record ────────────────────────────────────────────────────────

    public record WorkerProfileUpdateRequest(
            String description,
            String location,
            String region,
            String eyeColor,
            String hairColor,
            String phone,
            String bodyType,
            String mainPhotoId,
            List<String> services
    ) {}
}