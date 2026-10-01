package com.serv.controller;

import com.serv.common.*;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.MailService;
import com.serv.service.MediaStorageService;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Worker user) {
        return workerRepository.findByIdWithPhotos(user.getId())
                .map(worker -> ResponseEntity.ok(WorkerFullProfileDTO.from(worker)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/data")
    public ResponseEntity<?> updateWorkerSettings(@RequestBody Requests.AccountDataRequest req,
                                                  Worker worker) {

        if (req.username() != null) worker.setUsername(req.username());
        if (req.email() != null) worker.setEmail(new Email(req.email()));

        Worker saved = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(saved);

        sseStreamService.emitEvent(saved.getId(), "account-update", dto);
        return ResponseEntity.ok(dto);
    }

    @Transactional
    @GetMapping("/request-certification")
    public ResponseEntity<?> requestCertification(Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

        // 1. Récupérer ou générer un code de vérification s'il n'existe pas déjà
        String verificationCode = worker.getVerificationCode();
        if (verificationCode == null || verificationCode.isEmpty()) {
            verificationCode = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            worker.setVerificationCode(verificationCode);
        }

        // On s'assure que le statut reflète qu'on attend la photo
        if (worker.getCertificationStatus() == null || worker.getCertificationStatus() == CertificationStatus.NOT_CERTIFIED) {
            worker.setCertificationStatus(CertificationStatus.PENDING_PHOTO);
        }

        worker = workerRepository.save(worker);

        // 2. Convertir en DTO complet
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(worker);

        // 3. SSE
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);

        return ResponseEntity.ok().body(dto);
    }

    @Transactional
    @PostMapping("/certification-photo")
    public ResponseEntity<?> uploadCertificationPhoto(@RequestParam("file") MultipartFile file, Worker workerArg) {
        System.out.println("Certification photo received");
        try {
            final Worker worker = getWorkerWithPhotos(workerArg);

            // 1. Le service s'occupe de tout : stockage physique, miniatures et instanciation de la Photo
            Photo certificationPhoto = mediaStorageService.savePhoto(file, worker);

            photoRepository.save(certificationPhoto);
            System.out.println("Certification photo saved");

            // 2. Associer la photo de certification au Worker
            worker.setCertificationStatus(CertificationStatus.PENDING_APPROVAL);

            // 3. Mettre à jour ou créer la CertificationRequest pour l'admin
            CertificationRequest request = certificationRequestRepository.findByWorker(worker)
                    .orElseGet(() -> new CertificationRequest(worker));

            request.setStatus(CertificationStatus.PENDING_APPROVAL);
            request.setCertificationPhoto(certificationPhoto);
            certificationRequestRepository.save(request);

            Worker savedWorker = workerRepository.save(worker);

            // 4. Notification SSE en temps réel
            WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
            sseStreamService.emitEvent(savedWorker.getId(), "account-update", dto);

            System.out.println("Certification photo uploaded and saved");

            return ResponseEntity.ok().body(dto);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la sauvegarde : " + e.getMessage()));
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

        boolean requestedAvailability = body.getOrDefault("available", false);
        worker.setAvailable(requestedAvailability);

        if (requestedAvailability) {
            this.setWorkerProfileCompleteness(worker);
        }

        Worker savedWorker = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);

        return ResponseEntity.ok().body(dto);
    }

    /** PATCH /account/profile */
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Requests.WorkerProfileUpdateRequest req,
                                           Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);

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

        if (req.username()    != null) worker.setUsername(req.username());
        if (req.bodyType()    != null) worker.setBodyType(BodyType.valueOf(req.bodyType()));
        if (req.eyeColor()    != null) worker.setEyeColor(EyeColor.valueOf(req.eyeColor()));
        if (req.hairColor()   != null) worker.setHairColor(HairColor.valueOf(req.hairColor()));
        if (req.phone()       != null) worker.setPhone(req.phone());
        if (req.birthdate()   != null){
            try{
                worker.parseAndSetBirthdate(req.birthdate());

                if (worker.getAge() < 18) {
                    // ⚠️ ALERTE CRITIQUE
                    System.out.println("ALERT : updateProfile : " + worker.getAge() + " < 18 : profile deactivated");
                    worker.setLocked(true);
                    worker.setInvalid(true);
                    worker.setBanned(true);
                    this.emailService.sendAlertToLocalAuthorities(worker);

//                  // Returning an error could prevent some investigation. Better to silently warn the authorities
//                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                            .body(Map.of("error", "L'accès à cette plateforme est strictement réservé aux personnes majeures."));
                }
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

        this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);
        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);
        return ResponseEntity.ok().body(dto);
    }

    @PatchMapping("/updateservices")
    public ResponseEntity<?> updateServices(@RequestBody List<Integer> services, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);
        System.out.println("updateServices :" + services+" for worker "+worker.getUsername());

        List<Service> serviceList = services.stream()
                .map(serviceRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        worker.setServices(serviceList);this.setWorkerProfileCompleteness(worker);

        Worker savedWorker = workerRepository.save(worker);

        WorkerFullProfileDTO dto = WorkerFullProfileDTO.from(savedWorker);
        sseStreamService.emitEvent(worker.getId(), "account-update", dto);

        return ResponseEntity.ok(dto);
    }

    // Medias

    @PostMapping("/media")
    public ResponseEntity<?> uploadMedia(@RequestParam("files") List<MultipartFile> files, Worker workerArg) {
        Worker worker = getWorkerWithPhotos(workerArg);
        List<Object> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String contentType = file.getContentType();
                if (contentType != null && contentType.startsWith("image")) {
                    Photo photo = mediaStorageService.savePhoto(file, worker);
                    photo.setSortOrder(worker.getPhotos().size());
                    if (worker.getMainPhoto() == null) {
                        worker.setMainPhoto(photo);
                    }
                    worker.addPhoto(photo);
                    photoRepository.save(photo);
                    responses.add(photo);
                } else if (contentType != null && contentType.startsWith("video")) {
                    Video video = mediaStorageService.saveVideo(file, worker);
                    worker.addVideo(video);
                    videoRepository.save(video);
                    responses.add(video);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.setWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);
        sseStreamService.emitEvent(savedWorker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

        return ResponseEntity.ok(responses);
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

        System.out.println("Deleting photo " + photoId + " for worker " + worker.getUsername());

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || photo.getWorker() == null || !photo.getWorker().getId().equals(worker.getId())) {
            System.out.println("Photo not found or not owned by worker | Photo ID : " + photoId);
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
        Worker savedWorker = finalizeWorkerUpdate(worker);

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

        System.out.println("Deleting video " + videoId + " for worker " + worker.getUsername());

        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || video.getWorker() == null || !video.getWorker().getId().equals(worker.getId())) {
            System.out.println("Video not found or not owned by worker | Video ID : " + videoId);
            return ResponseEntity.notFound().build();
        }

        // 1 — Suppression des fichiers physiques de la vidéo
        mediaStorageService.deleteVideoFiles(worker.getId(), video);

        // 2 — Suppression de la collection et sauvegarde commune
        worker.removeVideo(video);
        Worker savedWorker = finalizeWorkerUpdate(worker);

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    private Worker finalizeWorkerUpdate(Worker worker) {
        this.setWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);
        sseStreamService.emitEvent(savedWorker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));
        return savedWorker;
    }

    /**
     * PATCH /account/photos/{photoId}/main
     * Set a photo as the main (gallery card) photo.
     */
    @PatchMapping("/photos/{photoId}/main")
    public ResponseEntity<WorkerFullProfileDTO> setMainPhoto(@PathVariable UUID photoId, Worker worker) {

        Photo photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !photo.getWorker().getId().equals(worker.getId()))
            return ResponseEntity.notFound().build();

        worker.setMainPhoto(photo);
        this.setWorkerProfileCompleteness(worker);
        Worker savedWorker = workerRepository.save(worker);

        sseStreamService.emitEvent(worker.getId(), "account-update", WorkerFullProfileDTO.from(savedWorker));

        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    /**
     * PATCH /account/photos/reorder
     * Accepts an ordered list of photo IDs and updates sortOrder accordingly.
     */
    @PatchMapping("/photos/reorder")
    public ResponseEntity<?> reorderPhotos(@RequestBody List<String> orderedIds,
                                           Worker worker) {

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
