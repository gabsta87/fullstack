package com.serv.controller;

import com.serv.common.BodyType;
import com.serv.common.EyeColor;
import com.serv.common.HairColor;
import com.serv.common.Requests;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.MediaStorageService;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account/worker")
@RequiredArgsConstructor
public class AccountControllerWorker {
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final PhotoRepository photoRepository;
    private final ServiceRepository serviceRepository;
    private final GeographicZoneRepository geographicZoneRepository;
    private final MediaStorageService mediaStorageService;
    private final SseStreamService sseStreamService;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        Worker user = jwtWorker(jwt);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in.");

        return workerRepository.findByIdWithPhotos(user.getId())
                .map(worker -> ResponseEntity.ok(WorkerFullProfileDTO.from(worker)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/data")
    @Transactional
    public ResponseEntity<?> updateWorkerSettings(@RequestBody Requests.AccountDataRequest req,
                                                  @AuthenticationPrincipal Jwt jwt) {
        Worker worker = jwtWorker(jwt);
        if (worker == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");

        if (req.username() != null) worker.setUsername(req.username());
        if (req.email() != null) worker.setEmail(new Email(req.email()));

        Worker saved = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(saved);

        sseStreamService.emitEvent(saved.getId(), "account-update", dto);
        return ResponseEntity.ok(dto);
    }


    /** PATCH /account/availability */
    @PatchMapping("/availability")
    @org.springframework.transaction.annotation.Transactional
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

    /** PATCH /account/profile */
    @PatchMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
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

    @org.springframework.transaction.annotation.Transactional
    @PatchMapping("/updateservices")
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
     * POST /account/photos
     * Upload a new photo — generates the main thumb and preview thumb.
     */
    @PostMapping("/photos")
    @org.springframework.transaction.annotation.Transactional
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
     * DELETE /account/photos/{photoId}
     */
    @DeleteMapping("/photos/{photoId}")
    @org.springframework.transaction.annotation.Transactional
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
     * PATCH /account/photos/{photoId}/main
     * Set a photo as the main (gallery card) photo.
     */
    @PatchMapping("/photos/{photoId}/main")
    @org.springframework.transaction.annotation.Transactional
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
     * PATCH /account/photos/reorder
     * Accepts an ordered list of photo IDs and updates sortOrder accordingly.
     */
    @PatchMapping("/photos/reorder")
    @org.springframework.transaction.annotation.Transactional
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

    private Worker jwtWorker(Jwt jwt) {
        if (jwt == null) return null;

        String userIdStr = jwt.getClaimAsString("userId");
        if (userIdStr == null) return null;

        return workerRepository.findById(UUID.fromString(userIdStr)).orElse(null);
    }

    private Worker jwtWorkerWithPhotos(Jwt jwt) {
        if (jwt == null) return null;

        String userIdStr = jwt.getClaimAsString("userId");
        if (userIdStr == null) return null;

        return workerRepository.findByIdWithPhotos(UUID.fromString(userIdStr)).orElse(null);
    }
}
