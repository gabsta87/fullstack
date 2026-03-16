package com.serv.controller;

import com.serv.database.entities.*;
import com.serv.common.BodyType;
import com.serv.common.Service;
import com.serv.database.repositories.*;
import com.serv.service.WorkerGalleryService;
import com.serv.service.MediaStorageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository       userRepository;
    private final WorkerRepository     workerRepository;
    private final PhotoRepository      photoRepository;
    private final WorkerGalleryService galleryService;
    private final MediaStorageService  mediaStorageService;

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

    /**
     * GET /account/me
     * Returns current user info + role-specific fields.
     * Angular uses this on page load to decide which page to render
     * and pre-fill the settings form.
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(HttpSession session) {
        VenusUser user = sessionUser(session);
        if (user == null) return ResponseEntity.status(401).build();

        boolean isWorker = user instanceof Worker;
        Map<String, Object> result = new HashMap<>();
        result.put("id",       user.getId().toString());
        result.put("username", user.getUsername());
        result.put("email",    user.getEmail());
        result.put("role",     isWorker ? "WORKER" : "CLIENT");

        if (isWorker) {
            Worker w = (Worker) user;
            result.put("available",    w.isAvailable());
            result.put("region",       w.getRegion());
            result.put("location",     w.getLocation());
            result.put("bodyType",     w.getBodyType() != null ? w.getBodyType().name() : null);
            result.put("height",       w.getHeight());
            result.put("weight",       w.getWeight());
            result.put("description",  w.getDescription());
            result.put("services",     w.getServices() != null
                    ? w.getServices().stream().map(Enum::name).toList() : List.of());
            result.put("mainThumbUrl", w.getMainPhoto() != null
                    ? w.getMainPhoto().getMainThumbUrl() : null);
            result.put("subscriptionDaysLeft", w.getSubscriptionDaysLeft());
            result.put("expired",      w.isExpired());
        }

        return ResponseEntity.ok(result);
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
        if (user == null) return ResponseEntity.status(401).build();

        if (body.containsKey("username") && !body.get("username").isBlank())
            user.setUsername(body.get("username"));
        if (body.containsKey("email") && !body.get("email").isBlank())
            user.setEmail(new Email(body.get("email")));
        if (body.containsKey("password") && !body.get("password").isBlank())
            user.setPassword(body.get("password"));

        userRepository.save(user);
        session.setAttribute("user", user);
        return ResponseEntity.ok(Map.of("message", "Settings updated."));
    }

    // ── CLIENT ────────────────────────────────────────────────────────────────

    /**
     * GET /account/favorites
     * Returns favorited workers as gallery DTOs — reuses the same worker-card component.
     */
    @GetMapping("/favorites")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFavorites(HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(401).build();

        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    /** POST /account/favorites/{workerId} */
    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(401).build();

        workerRepository.findById(workerId).ifPresent(w -> client.getFavorites().add(w));
        userRepository.save(client);
        session.setAttribute("user", client);
        return ResponseEntity.ok().build();
    }

    /** DELETE /account/favorites/{workerId} */
    @DeleteMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, HttpSession session) {
        Client client = sessionClient(session);
        if (client == null) return ResponseEntity.status(401).build();

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
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(401).build();

        worker.setAvailable(body.getOrDefault("available", false));
        workerRepository.save(worker);
        session.setAttribute("user", worker);
        return ResponseEntity.ok(Map.of("available", worker.isAvailable()));
    }

    /** PATCH /account/worker/profile */
    @PatchMapping("/worker/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody WorkerProfileUpdateRequest req,
                                           HttpSession session) {
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(401).build();

        if (req.description() != null) worker.setDescription(req.description());
        if (req.location()    != null) worker.setLocation(req.location());
        if (req.region()      != null) worker.setRegion(req.region());
        if (req.height()      != null) worker.setHeight(req.height());
        if (req.weight()      != null) worker.setWeight(req.weight());
        if (req.bodyType()    != null) worker.setBodyType(BodyType.valueOf(req.bodyType()));
        if (req.services()    != null) {
            List<Service> svcs = req.services().stream().map(Service::valueOf).toList();
            worker.setServices(svcs);
        }

        workerRepository.save(worker);
        session.setAttribute("user", worker);
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    /**
     * POST /account/worker/photos
     * Upload a new photo — generates main thumb + preview thumb.
     */
    @PostMapping("/worker/photos")
    @Transactional
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "title", required = false) String title,
                                         HttpSession session) {
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(401).build();

        try {
            // 1 — Save files to disk, get back URLs
            MediaStorageService.SavedMedia saved = mediaStorageService.savePhoto(file, worker.getId());

            // 2 — Create and persist the Photo entity
            Photo photo = new Photo();
            photo.setWorker(worker);
            photo.setTitle(title);
            photo.setOriginalUrl(saved.originalUrl());
            photo.setMainThumbUrl(saved.mainThumbUrl());
            photo.setPreviewThumbUrl(saved.previewThumbUrl());
            photo.setSortOrder(worker.getPhotos().size()); // append at end
            photoRepository.save(photo);

            // 3 — Set as main if first photo
            if (worker.getMainPhoto() == null) {
                worker.setMainPhoto(photo);
                workerRepository.save(worker);
                session.setAttribute("user", worker);
            }

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
        Worker worker = sessionWorker(session);
        if (worker == null) return ResponseEntity.status(401).build();

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        // If deleting main photo, pick next available
        if (worker.getMainPhoto() != null && worker.getMainPhoto().getId().equals(photoId)) {
            worker.getPhotos().stream()
                    .filter(p -> !p.getId().equals(photoId))
                    .findFirst()
                    .ifPresentOrElse(
                            p -> worker.setMainPhoto(p),
                            () -> worker.setMainPhoto(null)
                    );
        }
        worker.getPhotos().remove(photo);
        workerRepository.save(worker);
        session.setAttribute("user", worker);
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
        if (worker == null) return ResponseEntity.status(401).build();

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        worker.setMainPhoto(photo);
        workerRepository.save(worker);
        session.setAttribute("user", worker);
        return ResponseEntity.ok().build();
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
        if (worker == null) return ResponseEntity.status(401).build();

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
            Integer height,
            Integer weight,
            String bodyType,
            List<String> services
    ) {}
}