package com.serv.controller;

import com.serv.common.*;
import com.serv.configuration.JwtProvider;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.ClientDTO;
import com.serv.dto.VenusUserDTO;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.MediaStorageService;
import com.serv.service.SseStreamService;
import com.serv.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository       userRepository;
    private final WorkerRepository     workerRepository;
    private final ClientRepository     clientRepository;
    private final PhotoRepository      photoRepository;
    private final WorkerService        galleryService;
    private final MediaStorageService  mediaStorageService;
    private final ServiceRepository    serviceRepository;
    private final SseStreamService     sseStreamService;
    private final GeographicZoneRepository geographicZoneRepository;
    private final JwtProvider          jwtProvider;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VenusUser jwtUser(Jwt jwt) {
        if (jwt == null) return null;
        // 🎯 jwt.getSubject() contient désormais l'email
        return userRepository.findByEmail(jwt.getSubject()).orElse(null);
    }

    private Worker jwtWorkerWithPhotos(Jwt jwt) {
        if (jwt == null) return null;
        // 🎯 Appel de ta nouvelle méthode optimisée par email
        return workerRepository.findByEmailWithPhotos(new Email(jwt.getSubject())).orElse(null);
    }

    private Worker jwtWorker(Jwt jwt) {
        VenusUser u = jwtUser(jwt);
        return (u instanceof Worker w) ? w : null;
    }

    private Client jwtClient(Jwt jwt) {
        VenusUser u = jwtUser(jwt);
        return (u instanceof Client c) ? c : null;
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        VenusUser user = jwtUser(jwt);
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

    @PatchMapping("/language")
    @Transactional
    public ResponseEntity<?> setLanguage(@RequestBody String language, @AuthenticationPrincipal Jwt jwt) {
        VenusUser user = jwtUser(jwt);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Optional<Language> languageOpt = Language.fromRepositoryOrString(language);

        if (languageOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid language code.");
        }
        Language newLanguage = languageOpt.get();

        user.setLanguage(newLanguage);
        userRepository.save(user);

        VenusUser patchedUser = userRepository.save(user);

        sseStreamService.emitEvent(patchedUser.getId(), "account-update", VenusUserDTO.from(patchedUser));

        return ResponseEntity.ok(patchedUser);
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

        if(req.username() != null) user.setUsername(req.username());
        if(req.email() != null) user.setEmail(new Email(req.email()));
        if(req.password() != null) user.setPassword(req.password());

        VenusUser patchedUser = userRepository.save(user);

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
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);

        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        List<UUID> ids = client.getFavorites().stream().map(Worker::getId).toList();
        return ResponseEntity.ok(galleryService.getGalleryByIds(ids));
    }

    /** POST /account/favorites/{workerId} */
    @PostMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> addFavorite(@PathVariable UUID workerId, @AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);

        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Optional<Worker> foundWorker = workerRepository.findById(workerId);
        if (foundWorker.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Worker not found.");

        Worker worker = foundWorker.get();

        // 🎯 FIX 1 : Protection contre le double-clic (Idempotence)
        // On vérifie si ce worker n'est pas déjà dans les favoris du client
        boolean alreadyFavorite = client.getFavorites().stream()
                .anyMatch(w -> w.getId().equals(workerId));

        if (!alreadyFavorite) {
            client.getFavorites().add(worker);
            client = userRepository.save(client);

            // On n'émet l'événement SSE que s'il y a eu un réel changement
            sseStreamService.emitEvent(client.getId(), "account-update", ClientDTO.from(client));
        }

        // 🎯 FIX 2 : On renvoie le ClientDTO.from() au lieu de l'entité pour éviter le crash Jackson
        return ResponseEntity.ok(ClientDTO.from(client));
    }

    /** DELETE /account/favorites/{workerId} */
    @DeleteMapping("/favorites/{workerId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable UUID workerId, @AuthenticationPrincipal Jwt jwt) {
        Client client = jwtClient(jwt);
        if (client == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        client.getFavorites().removeIf(w -> w.getId().equals(workerId));
        Client patchedUser = userRepository.save(client);
        sseStreamService.emitEvent(patchedUser.getId(), "account-update", ClientDTO.from(patchedUser));

        return ResponseEntity.ok(patchedUser);
    }

    // ── WORKER ────────────────────────────────────────────────────────────────

    /** PATCH /account/worker/availability */
    @PatchMapping("/worker/availability")
    @Transactional
    public ResponseEntity<?> setAvailability(@RequestBody Map<String, Boolean> body,
                                             @AuthenticationPrincipal Jwt jwt) {
        System.out.println("setAvailability: " + body);
        Worker worker = jwtWorker(jwt);

        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        worker.setAvailable(body.getOrDefault("available", false));

        this.evaluateWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);
        return ResponseEntity.ok().body(dto);
    }

    /** PATCH /account/worker/profile */
    @PatchMapping("/worker/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@RequestBody Requests.WorkerProfileUpdateRequest req,
                                           @AuthenticationPrincipal Jwt jwt) {
        Worker worker = jwtWorkerWithPhotos(jwt);

        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        System.out.println("updateProfile: " + req);

        if (req.description() != null) worker.setDescription(req.description());

        if (req.geographicZoneId() != null) {
            if (req.geographicZoneId() == -1) {
                worker.setGeographicZone(null);
            } else {
                geographicZoneRepository.findById(req.geographicZoneId()).ifPresent(zone -> {
                    worker.setGeographicZone(zone);
                    System.out.println("updateProfile zone associée : " + zone.getName());
                });
            }
        }

        if (req.bodyType()    != null) worker.setBodyType(BodyType.valueOf(req.bodyType()));
        if (req.eyeColor()    != null) worker.setEyeColor(EyeColor.valueOf(req.eyeColor()));
        if (req.hairColor()   != null) worker.setHairColor(HairColor.valueOf(req.hairColor()));
        if (req.phone()       != null) worker.setPhone(req.phone());
        if (req.birthdate()   != null){
            try{
                worker.parseBirthdate(req.birthdate());
                System.out.println("updateProfile :" + worker.getBirthdate());
            }catch (ParseException e){
                return ResponseEntity.badRequest().body("Invalid birthdate format.");
            }

        }

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
            System.out.println("updateProfile :" + worker.getServices());
        }

        this.evaluateWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);
        return ResponseEntity.ok().body(dto);
    }

    @Transactional
    @PatchMapping("/worker/updateservices")
    public ResponseEntity<?> updateServices(@RequestBody List<String> services, @AuthenticationPrincipal Jwt jwt) {

        Worker worker = jwtWorkerWithPhotos(jwt);

        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        List<Service> serviceList = services.stream()
                .map(serviceRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        worker.setServices(serviceList);this.evaluateWorkerProfileCompleteness(worker);


        Worker savedWorker = workerRepository.save(worker);

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
                                         @AuthenticationPrincipal Jwt jwt) {

        // 🚀 Optimisé & Correction des bugs de variable :
        Worker worker = jwtWorkerWithPhotos(jwt);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

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
            }

            worker.addPhoto(photo);
            photoRepository.save(photo);

            // On sauvegarde l'état du worker mis à jour
            this.evaluateWorkerProfileCompleteness(worker);
            Worker savedWorker = workerRepository.save(worker);

            // Plus besoin de refaire un findByIdWithPhotos ici, l'entité est déjà à jour dans la session Hibernate
            sseStreamService.emitEvent(savedWorker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

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
    public ResponseEntity<?> deletePhoto(@PathVariable UUID photoId, @AuthenticationPrincipal Jwt jwt) {
        // 🚀 Optimisé & Correction du bug sessionWorker :
        Worker worker = jwtWorkerWithPhotos(jwt);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");
        if (photoId == null) return ResponseEntity.badRequest().body("No file provided.");

        System.out.println("Deleting photo " + photoId + " for worker " + worker.getId());

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        // 1 — Suppression des fichiers physiques sur le disque
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
        this.evaluateWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);

        // Émission du profil mis à jour
        sseStreamService.emitEvent(savedWorker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /account/worker/photos/{photoId}/main
     * Set a photo as the main (gallery card) photo.
     */
    @PatchMapping("/worker/photos/{photoId}/main")
    @Transactional
    public ResponseEntity<?> setMainPhoto(@PathVariable UUID photoId, @AuthenticationPrincipal Jwt jwt) {
        Worker worker = jwtWorker(jwt);
        if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        worker.setMainPhoto(photo);
        this.evaluateWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);

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
                                           @AuthenticationPrincipal Jwt jwt) {
        Worker worker = jwtWorker(jwt);
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

    // Utility method

    private void evaluateWorkerProfileCompleteness(Worker worker) {
        boolean isComplete = worker.getUsername() != null && !worker.getUsername().trim().isEmpty()
                && worker.getEmail() != null && !worker.getEmail().getValue().trim().isEmpty()
                && worker.getDescription() != null && !worker.getDescription().trim().isEmpty()
                && worker.getGeographicZone() != null
                && worker.getPhone() != null && !worker.getPhone().trim().isEmpty()
                && worker.getServices() != null && !worker.getServices().isEmpty()
                && worker.getPhotos() != null && !worker.getPhotos().isEmpty()
                && worker.getBirthdate() != null;

        worker.setDisabled(!isComplete);
    }

}