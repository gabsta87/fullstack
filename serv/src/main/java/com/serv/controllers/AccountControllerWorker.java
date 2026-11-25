package com.serv.controllers;

import com.serv.common.*;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.requests.UpdateWorkerProfile;
import com.serv.requests.UpdateWorkerLanguage;
import com.serv.services.MailService;
import com.serv.services.MediaStorageService;
import com.serv.services.SseStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/account/worker")
@PreAuthorize("hasRole('WORKER')")
@RequiredArgsConstructor
@Transactional
public class AccountControllerWorker {
    private final WorkerRepository workerRepository;
    private final PhotoRepository photoRepository;
    private final VideoRepository videoRepository;
    private final ServiceRepository serviceRepository;
    private final GeographicZoneRepository geographicZoneRepository;
    private final CertificationRequestRepository certificationRequestRepository;
    private final MediaStorageService mediaStorageService;
    private final SseStreamService sseStreamService;
    private final MailService emailService;

    @Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<WorkerFullProfileDTO> getMe(Worker user) {
        // 1. Recharger le worker depuis le repository pour le rendre "managed" dans la session active
        Worker managedWorker = workerRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker introuvable"));

        // 2. Le mapper vers le DTO (les collections lazy pourront s'initialiser grâce à la session active et au @BatchSize)
        return ResponseEntity.ok(WorkerFullProfileDTO.from(managedWorker));
    }

    @Transactional
    @GetMapping("/request-certification")
    public ResponseEntity<?> requestCertification(Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);
        rejectIfCertificationInProgress(worker);

        String verificationCode = worker.getVerificationCode();
        if (verificationCode == null || verificationCode.isEmpty()) {
            verificationCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            worker.setVerificationCode(verificationCode);
        }

        CertificationRequest request = certificationRequestRepository.findByWorker(worker)
                .orElseGet(() -> new CertificationRequest(workerArg));

        if (request.getStatus() == null || request.getStatus() == CertificationStatus.NOT_CERTIFIED) {
            request.setStatus(CertificationStatus.PENDING_PHOTO);
        }

        worker = workerRepository.save(worker);

        sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", worker.getId());

        return ResponseEntity.ok().body(WorkerFullProfileDTO.from(worker));
    }

    @Transactional
    @PostMapping("/certification-photo")
    public ResponseEntity<?> uploadCertificationPhoto(@RequestParam("file") MultipartFile file, Worker workerArg) {
        try {
            final Worker worker = getWorkerWithPhotos(workerArg);
            rejectIfCertificationInProgress(worker);

            Photo certificationPhoto = mediaStorageService.saveCertificationPhoto(file, worker);
            certificationPhoto.setType(PhotoType.CERTIFICATION);
            photoRepository.save(certificationPhoto);

            CertificationRequest request = certificationRequestRepository.findByWorker(worker)
                    .orElseGet(() -> new CertificationRequest(worker));

            request.setStatus(CertificationStatus.PENDING_APPROVAL);
            request.setCertificationPhoto(certificationPhoto);

            CertificationRequest savedRequest = certificationRequestRepository.save(request);
            worker.setLastCertificationRequest(savedRequest);

            Worker savedWorker = workerRepository.save(worker);

            sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", savedRequest.getId());

            return ResponseEntity.ok().body(WorkerFullProfileDTO.from(savedWorker));
        } catch (IOException e) {
            log.error("Could not store certification photo of worker {}", workerArg.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not store the file."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @Transactional
    @PatchMapping("/availability")
    public ResponseEntity<?> setAvailability(@RequestBody Map<String, Boolean> body,
                                             Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        Boolean requestedAvailability = body == null ? null : body.get("available");
        if (requestedAvailability == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'available' is required.");
        }
        worker.setAvailable(requestedAvailability);

        if (requestedAvailability) {
            this.setWorkerProfileCompleteness(worker);
        }

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok().body(WorkerFullProfileDTO.from(savedWorker));
    }

    @Transactional
    @PatchMapping("/visibility")
    public ResponseEntity<?> setVisibility(@RequestBody Map<String, Boolean> body,
                                             Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);
        Boolean requestedVisibility = body == null ? null : body.get("hidden");
        if (requestedVisibility == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'hidden' is required.");
        }

        worker.setHidden(requestedVisibility);

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok().body(WorkerFullProfileDTO.from(savedWorker));
    }

    /** PATCH /account/worker/profile — partial update, every field is validated before anything is changed. */
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateWorkerProfile req,
                                           Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        // 1. Validate and resolve everything first: an invalid field must not leave a half-updated profile.
        BodyType bodyType = parseEnum(BodyType.class, req.bodyType());
        EyeColor eyeColor = parseEnum(EyeColor.class, req.eyeColor());
        HairColor hairColor = parseEnum(HairColor.class, req.hairColor());

        GeographicZone zone = null;
        if (req.geographicZoneId() != null && req.geographicZoneId() != -1) {
            zone = geographicZoneRepository.findById(req.geographicZoneId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown geographic zone."));
        }

        Photo mainPhoto = null;
        if (req.mainPhotoId() != null) {
            mainPhoto = findOwnGalleryPhoto(worker, req.mainPhotoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found."));
        }

        List<Service> services = null;
        if (req.services() != null) {
            services = new ArrayList<>();
            for (String name : new LinkedHashSet<>(req.services())) {
                services.add(serviceRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service.")));
            }
        }

        if (req.birthdate() != null) {
            worker.parseAndSetBirthdate(req.birthdate()); // IllegalArgumentException -> 400
        }

        // 2. Apply.
        if (req.geographicZoneId() != null) worker.setGeographicZone(zone);
        if (req.description() != null) worker.setDescription(blankToNull(req.description()));
        if (req.username() != null) worker.setUsername(blankToNull(req.username()));
        if (req.shortDescription() != null) worker.setShortDescription(blankToNull(req.shortDescription()));
        if (req.bodyType() != null) worker.setBodyType(bodyType);
        if (req.eyeColor() != null) worker.setEyeColor(eyeColor);
        if (req.hairColor() != null) worker.setHairColor(hairColor);
        if (req.phone() != null) worker.setPhone(blankToNull(req.phone()));
        if (mainPhoto != null) worker.setMainPhoto(mainPhoto);
        if (services != null) worker.setServices(services);

        if (req.birthdate() != null && worker.getBirthdate() != null && worker.getAge() < 18) {
            // Minors are silently deactivated and reported (returning an error could hinder an investigation).
            log.warn("Worker {} declared an age under 18: profile deactivated", worker.getId());
            worker.setLocked(true);
            worker.setInvalid(true);
            worker.setBanned(true);
            this.emailService.sendAlertToLocalAuthorities(worker);
        }

        this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok().body(WorkerFullProfileDTO.from(savedWorker));
    }

    @PatchMapping("/updateservices")
    public ResponseEntity<?> updateServices(@RequestBody List<Integer> services, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        if (services == null || services.size() > InputLimits.LIST_MAX || services.contains(null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid services list.");
        }

        List<Service> serviceList = new ArrayList<>();
        for (Integer id : new LinkedHashSet<>(services)) {
            serviceList.add(serviceRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service.")));
        }

        worker.setServices(serviceList);this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    // Medias

    @PostMapping("/media")
    public ResponseEntity<?> uploadMedia(@RequestParam("files") List<MultipartFile> files, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided.");
        }
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At most " + MAX_FILES_PER_UPLOAD + " files per upload.");
        }

        long incoming = files.stream().mapToLong(MultipartFile::getSize).sum();
        long used = mediaStorageService.usedBytes(worker.getId());
        if (used + incoming > worker.getMaxStorageBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Storage limit reached: " + (worker.getMaxStorageBytes() / (1024 * 1024)) + " MB.");
        }

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only images and videos are accepted.");
            }
        }

        boolean mediaAdded = false;
        for (MultipartFile file : files) {
            try {
                if (file.getContentType().startsWith("image/")) {
                    Photo photo = mediaStorageService.savePhoto(file, worker);
                    photo.setSortOrder(worker.getPhotos().size());
                    if (worker.getMainPhoto() == null) {
                        worker.setMainPhoto(photo);
                    }
                    worker.addPhoto(photo);
                    photoRepository.save(photo);
                } else {
                    Video video = mediaStorageService.saveVideo(file, worker);
                    worker.addVideo(video);
                    videoRepository.save(video);
                }
                mediaAdded = true;
            } catch (IllegalArgumentException e) {
                // Invalid file: reject the request (files already stored are kept, they are valid).
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            } catch (IOException e) {
                log.error("Could not store media of worker {}", worker.getId(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the file.");
            }
        }

        if (mediaAdded) {
            worker.clearCertification();

            CertificationRequest lastRequest = worker.getLastCertificationRequest();
            if (lastRequest != null) {
                certificationRequestRepository.save(lastRequest);
            }
        }

        this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        if (mediaAdded) {
            sseStreamService.emitEvent(savedWorker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));
        }

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * DELETE /account/photos/{photoId}
     */
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<WorkerFullProfileDTO> deletePhoto(@PathVariable UUID photoId, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        if (photoId == null) {
            return ResponseEntity.badRequest().build();
        }

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || photo.getWorker() == null || !photo.getWorker().getId().equals(worker.getId())) {
            return ResponseEntity.notFound().build();
        }

        // 1 — Suppression des fichiers physiques de la photo
        mediaStorageService.deletePhotoFiles(worker.getId(), photo);

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

        // 3 — Suppression de la collection et sauvegarde commune
        worker.removePhoto(photo);

        this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * DELETE /account/videos/{videoId}
     */
    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<WorkerFullProfileDTO> deleteVideo(@PathVariable UUID videoId, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        if (videoId == null) {
            return ResponseEntity.badRequest().build();
        }

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || video.getWorker() == null || !video.getWorker().getId().equals(worker.getId())) {
            return ResponseEntity.notFound().build();
        }

        // 1 — Suppression des fichiers physiques de la vidéo
        mediaStorageService.deleteVideo(worker.getId(), video);

        // 2 — Suppression de la collection et sauvegarde commune
        worker.removeVideo(video);

        this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * PATCH /account/photos/{photoId}/main
     * Set a photo as the main (gallery card) photo.
     */
    @PatchMapping("/photos/{photoId}/main")
    public ResponseEntity<WorkerFullProfileDTO> setMainPhoto(@PathVariable UUID photoId, Worker worker) {
        worker = getWorkerWithPhotos(worker);
        Photo photo = findOwnGalleryPhoto(worker, photoId).orElse(null);
        if (photo == null)
            return ResponseEntity.notFound().build();

        worker.setMainPhoto(photo);
        this.setWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * PATCH /account/photos/reorder
     * Accepts an ordered list of photo IDs and updates sortOrder accordingly.
     */
    @PatchMapping("/photos/reorder")
    public ResponseEntity<WorkerFullProfileDTO> reorderPhotos(@RequestBody List<String> orderedIds,
                                                              Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        if (orderedIds == null || orderedIds.size() > MAX_PHOTOS_REORDER || new HashSet<>(orderedIds).size() != orderedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo list.");
        }

        List<Photo> ordered = new ArrayList<>();
        for (String rawId : orderedIds) {
            UUID id = UUID.fromString(rawId); // IllegalArgumentException -> 400
            ordered.add(findOwnGalleryPhoto(worker, id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown photo.")));
        }
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i);
            photoRepository.save(ordered.get(i));
        }

        // 💡 On recharge le worker propre avec ses photos triées et on notifie le front
        this.setWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);
        WorkerFullProfileDTO updatedDto = WorkerFullProfileDTO.from(savedWorker);

        return ResponseEntity.ok(updatedDto);
    }

    // Spoken languages

    @Transactional
    @PostMapping("/spoken-languages")
    public ResponseEntity<WorkerFullProfileDTO> updateSpokenLanguages(
            @RequestBody @Valid UpdateWorkerLanguage request,
            Worker workerArg) {

        Worker managedWorker = workerRepository.findById(workerArg.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker introuvable."));

        Set<WorkerLanguage> newLanguages = request.languages().stream()
                .map(dto -> new WorkerLanguage(dto.language(), dto.level()))
                .collect(Collectors.toSet());

        managedWorker.setSpokenLanguages(newLanguages);

        Worker savedWorker = workerRepository.save(managedWorker);
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    // Utility methods

    private static final int MAX_FILES_PER_UPLOAD = 10;
    private static final int MAX_PHOTOS_REORDER = 200;

    /** A photo the worker owns and that is part of the public gallery (never a certification photo). */
    private Optional<Photo> findOwnGalleryPhoto(Worker worker, UUID photoId) {
        return photoRepository.findById(photoId)
                .filter(p -> p.getWorker() != null && p.getWorker().getId().equals(worker.getId()))
                .filter(p -> p.getType() == PhotoType.GALLERY);
    }

    private void rejectIfCertificationInProgress(Worker worker) {
        if (worker.isCertified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile already certified.");
        }
        CertificationRequest last = worker.getLastCertificationRequest();
        if (last != null && last.getStatus() == CertificationStatus.PENDING_APPROVAL && last.isUnderReview()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Certification is being reviewed.");
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value for " + type.getSimpleName() + ".");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setWorkerProfileCompleteness(Worker worker) {
        boolean isComplete = worker.getUsername() != null && !worker.getUsername().trim().isEmpty()
                && worker.getEmail() != null && !worker.getEmail().getValue().trim().isEmpty()
                && worker.getDescription() != null && !worker.getDescription().trim().isEmpty()
                && worker.getGeographicZone() != null
                && worker.getPhone() != null && !worker.getPhone().trim().isEmpty()
                && worker.getServices() != null && !worker.getServices().isEmpty()
                && worker.getPhotos() != null && !worker.getPhotos().isEmpty()
                && worker.getBirthdate() != null
                && worker.getAge() >= 18;

        worker.setInvalid(!isComplete);
    }

    private Worker getWorkerWithPhotos(VenusUser user) {
        return workerRepository.findByIdWithPhotos(user.getId()).orElse((Worker) user);
    }
}
