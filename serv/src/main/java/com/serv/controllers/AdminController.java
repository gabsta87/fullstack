package com.serv.controllers;

import com.serv.common.CertificationStatus;
import com.serv.common.ReportStatus;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.*;
import com.serv.requests.admin.*;
import com.serv.services.PasswordResetService;
import com.serv.services.SseStreamService;
import com.serv.services.UserCleanupService;
import com.serv.common.InputLimits;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final GeographicZoneRepository geographicZoneRepository;
    private final ServiceRepository serviceRepository;
    private final LegalTextRepository legalTextRepository;
    private final SseStreamService sseStreamService;
    private final PasswordResetService passwordResetService;
    private final CommentRepository commentRepository;
    private final GeographicZoneRepository zoneRepository;
    private final CertificationRequestRepository certificationRequestRepository;
    private final UserCleanupService userCleanupService;
    private final ReportRepository reportRepository;
    private final com.serv.services.MediaStorageService mediaStorageService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAdmin(Admin adminArg) {
        return sseStreamService.createAdminStream(adminArg.getId());
    }

    // USERS

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        // 1. On récupère tous les workers et toutes les requêtes de certification d'un coup
        List<VenusUser> users = userRepository.findAll();

        // On crée une map pour associer facilement un workerId à sa date de demande
        Map<UUID, String> requestDatesMap = certificationRequestRepository.findAll().stream()
                .filter(req -> req.getWorker() != null && req.getCreatedAt() != null)
                .collect(Collectors.toMap(
                        req -> req.getWorker().getId(),
                        req -> req.getCreatedAt().toString(),
                        (existing, replacement) -> existing // Sécurité en cas de doublon
                ));

        // 2. On transforme en DTO en passant la map ou la date correspondante
        List<AdminUserDTO> userDTOs = users.stream()
                .map(user -> {
                    String reqDate = (user instanceof Worker w) ? requestDatesMap.get(w.getId()) : null;
                    return AdminUserDTO.from(user, reqDate); // On passe la date en paramètre
                })
                .toList();

        return ResponseEntity.ok(userDTOs);
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable UUID id, Admin admin) {
        VenusUser userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Empêcher un admin de se supprimer lui-même
        if (userToDelete.getId().equals(admin.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "An admin cannot suppress himself"));
        }
        requireAuthorityOver(admin, userToDelete);

        // Appel du service de nettoyage et de suppression
        userCleanupService.deleteUserCompletely(userToDelete);

        // Audit Log
        logAdminAction(admin, "DELETE_USER", "null", "Account deleted " + userToDelete.getEmail());

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // WORKERS PROFILES

    @Transactional(readOnly = true)
    @GetMapping("/workers/{workerId}")
    public ResponseEntity<WorkerFullProfileDTO> getWorkerProfileForAdmin(@PathVariable UUID workerId) {
        Worker worker = workerRepository.findByIdWithPhotos(workerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        return ResponseEntity.ok(WorkerFullProfileDTO.from(worker));
    }

    @PostMapping("/profiles/{id}/status")
    @Transactional
    public ResponseEntity<?> updateWorkerStatus(@PathVariable UUID id, @RequestBody UpdateWorkerStatus req, Admin admin) {
        VenusUser targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        if (targetUser.getId().equals(admin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An admin cannot change his own status.");
        }
        requireAuthorityOver(admin, targetUser);

        StringBuilder changes = new StringBuilder();

        if (req.locked() != null) {
            targetUser.setLocked(req.locked());
            changes.append("Verrouillé: ").append(req.locked()).append(" ");
        }
        if (req.banned() != null) {
            targetUser.setBanned(req.banned());
            changes.append("Banni: ").append(req.banned()).append(" ");
        }

        VenusUser savedUser = userRepository.save(targetUser);
        logAdminAction(admin, "UPDATE_WORKER_STATUS", targetUser, "Profile updated -> " + changes.toString().trim());

        return ResponseEntity.ok(AdminUserDTO.from(savedUser));
    }

    @PostMapping("/profiles/update-days")
    @Transactional
    public ResponseEntity<?> updateDays(@Valid @RequestBody UpdateWorkerDays req, Admin admin) {
        Worker targetWorker = workerRepository.findById(req.workerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));

        int oldDays = targetWorker.getRemainingDaysCredit() != null ? targetWorker.getRemainingDaysCredit() : 0;
        targetWorker.setRemainingDaysCredit(req.newDaysValue());

        Worker savedWorker = workerRepository.save(targetWorker);

        logAdminAction(admin, "UPDATE_CREDIT_DAYS", targetWorker, String.format("Days modified: %d -> %d | Cause: %s", oldDays, req.newDaysValue(), req.reason()));
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    // CERTIFICATIONS

    @Transactional
    @GetMapping("/certification-requests")
    public ResponseEntity<List<CertificationRequestDTO>> getPendingRequests() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(1);

        List<CertificationRequest> requests = certificationRequestRepository.findByStatus(CertificationStatus.PENDING_APPROVAL);

        for (CertificationRequest req : requests) {
            if (req.isUnderReview() && req.getLockedAt() != null && req.getLockedAt().isBefore(expirationThreshold)) {
                req.setUnderReview(false);
                req.setLockedByAdmin(null);
                req.setLockedAt(null);
                certificationRequestRepository.save(req);
            }
        }

        return ResponseEntity.ok(requests.stream().map(CertificationRequestDTO::from).collect(Collectors.toList()));
    }

    @Transactional
    @PostMapping("/certification-request/{requestId}/lock")
    public ResponseEntity<?> lockCertificationReview(@PathVariable Long requestId, Admin admin) {
        CertificationRequest certificationRequest = certificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        if (certificationRequest.isUnderReview()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        certificationRequest.setUnderReview(true);
        certificationRequest.setLockedByAdmin( admin );
        certificationRequest.setLockedAt(LocalDateTime.now());
        CertificationRequest saved = certificationRequestRepository.save(certificationRequest);
        sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", certificationRequest.getId());

        return ResponseEntity.ok(CertificationRequestDTO.from(saved));
    }

    @Transactional
    @PostMapping("/certification-request/{requestId}/unlock")
    public ResponseEntity<?> unlockCertificationReview(@PathVariable Long requestId) {
        CertificationRequest certificationRequest = certificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        certificationRequest.setUnderReview(false);
        certificationRequest.setLockedByAdmin(null);
        CertificationRequest saved = certificationRequestRepository.save(certificationRequest);
        sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", certificationRequest.getId());
        return ResponseEntity.ok(CertificationRequestDTO.from(saved));
    }

    @PostMapping("/profiles/verify-certification")
    @Transactional
    public ResponseEntity<?> processCertification(@Valid @RequestBody CertificationReviewDTO dto, Admin admin) {
        CertificationRequest request = certificationRequestRepository.findById(dto.requestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        Worker worker = request.getWorker();
        if (worker == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found");
        }
        if (request.getStatus() != CertificationStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is not awaiting approval.");
        }

        CertificationRequest saved;

        if (dto.approved()) {
            request.setStatus(CertificationStatus.CERTIFIED);
            saved = certificationRequestRepository.save(request);

            worker.setCertifiedAt(LocalDateTime.now());
            worker.setVerificationCode(null);
            workerRepository.save(worker);

            logAdminAction(admin, "CERTIFICATION_APPROVED", worker, "Certification approved for worker " + worker.getEmail());

        } else {
            request.setStatus(CertificationStatus.REJECTED);
            request.setProcessedAt(LocalDateTime.now());
            request.setUnderReview(false);
            request.setLockedByAdmin(null);
            request.setComment(dto.comment());

            saved = certificationRequestRepository.save(request);
            logAdminAction(admin, "CERTIFICATION_DENIED", worker, "Certification denied for worker " + worker.getEmail() + " because of " + dto.comment());
        }
        sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", saved.getId());

        WorkerFullProfileDTO workerDto = WorkerFullProfileDTO.from(worker);
        sseStreamService.emitEvent(worker.getId(), "account-update", workerDto);

        return ResponseEntity.ok(CertificationRequestDTO.from(saved));
    }

    @Transactional
    @PostMapping("/profiles/revoke-certification")
    public ResponseEntity<?> revokeCertification(@Valid @RequestBody CertificationRevocationDTO dto, Admin admin) {
        Worker worker = workerRepository.findById(dto.workerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Worker not found"));
        if (worker.getLastCertificationRequest() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This worker has no certification.");
        }

        worker.clearCertification();

        CertificationRequest savedRequest = certificationRequestRepository.save(worker.getLastCertificationRequest());
        workerRepository.save(worker);

        logAdminAction(admin, "CERTIFICATION_REVOKED", worker, "Certification revoked for worker " + worker.getEmail()+ " for reason: " + dto.reason());

        sseStreamService.broadcastAdminEvent("CERTIFICATION_UPDATED", savedRequest.getId());

        WorkerFullProfileDTO workerDto = WorkerFullProfileDTO.from(worker);
        sseStreamService.emitEvent(worker.getId(), "account-update", workerDto);

        return ResponseEntity.ok(CertificationRequestDTO.from(savedRequest));
    }

    /**
     * Certification photos are private: they are only served here, to admins, and never cached.
     */
    @GetMapping("/certification-photos/{workerId}/{fileName}")
    public ResponseEntity<org.springframework.core.io.Resource> getCertificationPhoto(@PathVariable UUID workerId,
                                                                                      @PathVariable String fileName) {
        return mediaStorageService.findCertificationPhoto(workerId, fileName)
                .map(path -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .cacheControl(org.springframework.http.CacheControl.noStore().cachePrivate())
                        .body((org.springframework.core.io.Resource) new org.springframework.core.io.FileSystemResource(path)))
                .orElse(ResponseEntity.notFound().build());
    }

    // SERVICES

    /**
     * 🔄 SAVE OR UPDATE unique pour les services
     */
    @PostMapping("/service")
    @Transactional
    public ResponseEntity<?> saveOrUpdateService(@Valid @RequestBody UpdateService service, Admin admin) {

        boolean isUpdate = service.id() != null;
        String trimmedName = service.name().trim();

        if (isUpdate) {
            Service existing = serviceRepository.findById(service.id()).orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();
            if (serviceRepository.findByName(trimmedName).filter(other -> !other.getId().equals(existing.getId())).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Service already exists");
            }

            String oldName = existing.getName();
            existing.setName(service.name().trim());
            if(service.description() != null)
                existing.setDescription(service.description());
            serviceRepository.save(existing);

            logAdminAction(admin, "UPDATE_SERVICE", existing, String.format("Service %s renamed to %s", oldName, existing.getName()));
        } else {
            if (serviceRepository.findByName(trimmedName).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Service already exists");
            }
            Service newService = new Service();
            newService.setName(service.name().trim());
            if(service.description() != null)
                newService.setDescription(service.description());
            serviceRepository.save(newService);

            logAdminAction(admin, "CREATE_SERVICE", service, "Création du service : " + newService.getName());
        }

        List<ServiceDTO> updatedServices = serviceRepository.findAll().stream().map(ServiceDTO::from).collect(Collectors.toList());

        sseStreamService.broadcastAdminEvent("SERVICES_UPDATED", updatedServices);

        return ResponseEntity.ok(updatedServices);
    }

    @DeleteMapping("/services/{id}")
    @Transactional
    public ResponseEntity<?> deleteService(@PathVariable int id, Admin admin) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        // 1. Sauvegarde des infos pour l'audit
        String serviceName = service.getName();
        String snapshot = String.format("{\"id\": %d, \"name\": \"%s\"}", id, serviceName);

        // 2. Suppression effective
        serviceRepository.delete(service);

        // 3. Enregistrement de l'audit
        logAdminAction(admin, "DELETE_SERVICE", snapshot, "Suppression du service : " + serviceName);

        // 4. Récupération de la liste mise à jour des services
        List<ServiceDTO> updatedServices = serviceRepository.findAll()
                .stream()
                .map(ServiceDTO::from)
                .collect(Collectors.toList());

        // 5. Émission de l'événement SSE
        sseStreamService.broadcastAdminEvent("SERVICES_UPDATED", updatedServices);

        // 6. Retour HTTP final
        return ResponseEntity.ok(updatedServices);
    }

    // LEGAL TEXTS

    @PostMapping("/legal")
    @Transactional
    public ResponseEntity<?> updateLegalText(@Valid @RequestBody UpdateLegalText req, Admin admin) {
        LegalText legal = new LegalText();
        legal.setName(req.key());
        legal.setContent(req.content());
        legal.setLastUpdate(LocalDateTime.now());
        legal.setAuthor(admin);
        legalTextRepository.save(legal);

        logAdminAction(admin, "UPDATE_LEGAL_TEXT", legal, "Mise à jour du texte légal : " + req.key());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // REGIONS

    @Transactional(readOnly = true)
    @GetMapping("/regions-flat")
    public ResponseEntity<List<GeographicZoneWithParentDTO>> getAllLocationsFlat() {
        List<GeographicZoneWithParentDTO> zones = zoneRepository.findAll().stream()
                .map(GeographicZoneWithParentDTO::from)
                .toList();
        return ResponseEntity.ok(zones);
    }

    @DeleteMapping("/regions/{id}")
    @Transactional
    public ResponseEntity<?> deleteRegion(@PathVariable int id, Admin admin) {
        GeographicZone zone = geographicZoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));

        // 1. Rassembler l'ID de la zone parente et de ses enfants directs (2 niveaux max)
        List<Integer> zoneIdsToCheck = new java.util.ArrayList<>();
        zoneIdsToCheck.add(zone.getId());

        // Récupération des enfants (adapte la méthode selon ton repository, ex: findByParentId ou findByParent)
        List<GeographicZone> childrenZones = geographicZoneRepository.findByParentId(id);
        for (GeographicZone child : childrenZones) {
            zoneIdsToCheck.add(child.getId());
        }

        // 2. Compter les annonceurs présents dans la zone OU dans ses enfants
        long totalWorkers = workerRepository.countByGeographicZoneIdIn(zoneIdsToCheck);
        if (totalWorkers > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Impossible de supprimer cette région (ou ses sous-régions) : " + totalWorkers + " annonceur(s) y sont rattaché(s)."
            ));
        }

        // 3. Sauvegarde des infos pour le log d'audit
        String zoneName = zone.getName();
        String snapshot = String.format("{\"id\": %d, \"name\": \"%s\"}", id, zoneName);

        // 4. Suppression effective (si cascade est configuré sur les enfants, ils seront supprimés avec)
        geographicZoneRepository.delete(zone);

        // 5. Enregistrement de l'audit
        logAdminAction(admin, "DELETE_REGION", snapshot, "Suppression de la zone géographique : " + zoneName);

        // 6. Récupération de la liste mise à jour des régions
        List<GeographicZoneWithChildrenDTO> updatedZones = geographicZoneRepository.findAll()
                .stream()
                .map(GeographicZoneWithChildrenDTO::from)
                .collect(Collectors.toList());

        // 7. Émission de l'événement SSE
        sseStreamService.broadcastAdminEvent("REGIONS_UPDATED", updatedZones);

        // 8. Retour HTTP final
        return ResponseEntity.ok(updatedZones);
    }

    @PostMapping("/region")
    @Transactional
    public ResponseEntity<List<GeographicZoneWithParentDTO>> updateRegion(Admin admin, @Valid @RequestBody UpdateRegion region){
        GeographicZone savedZone;
        if(region.id() == null){
            // Create new Region
            GeographicZone newZone = new GeographicZone();
            newZone.setName(region.name().trim());

            if (region.parentId() != null) {
                GeographicZone parent = geographicZoneRepository.findById(region.parentId()).orElse(null);
                if (parent == null) return ResponseEntity.notFound().build();
                newZone.setParent(parent);
            } else {
                newZone.setParent(null);
            }

            savedZone = geographicZoneRepository.save(newZone);

            logAdminAction(admin,"ZONE_CREATED", savedZone,"Zone "+ region.name() +" with "+ (newZone.getParent() != null ? newZone.getParent().getName() : "no parent") +" created");
        }else{
            // Modify existing Region
            GeographicZone zone = geographicZoneRepository.findById(region.id()).orElse(null);
            if(zone == null) return ResponseEntity.notFound().build();

            String oldName = zone.getName();
            String oldParentName = (zone.getParent() != null) ? zone.getParent().getName() : "Aucun";

            // Gérer le cas où le parentId est fourni ou mis à null
            if (region.parentId() != null) {
                GeographicZone parentZone = geographicZoneRepository.findById(region.parentId()).orElse(null);
                if(parentZone == null) return ResponseEntity.notFound().build();
                if (createsCycle(zone, parentZone)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A region cannot be its own ancestor.");
                }
                zone.setParent(parentZone);
            } else {
                zone.setParent(null); // Devient une zone racine
            }

            zone.setName(region.name().trim());
            geographicZoneRepository.save(zone);

            String newParentName = (zone.getParent() != null) ? zone.getParent().getName() : "Aucun";
            logAdminAction(admin, "ZONE_MODIFIED", zone, "Zone " + oldName + " with parent " + oldParentName + " modified to " + zone.getName() + " with parent " + newParentName);
        }

        List<GeographicZoneWithParentDTO> updatedZones = geographicZoneRepository.findAll().stream().map(GeographicZoneWithParentDTO::from).collect(Collectors.toList());

        sseStreamService.broadcastAdminEvent("REGIONS_UPDATED", updatedZones);

        return ResponseEntity.ok(updatedZones);
    }

    // COMMENTS

    @DeleteMapping("/comments/{id}")
    @Transactional
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Admin admin) {
        Comment c = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        Worker targetWorker = c.getWorker();
        if (c.getAuthor() != null) {
            c.getAuthor().removeComment(c);
        }

        List<CommentDTO> remainingComments = List.of();
        if (targetWorker != null) {
            targetWorker.getComments().remove(c);
            remainingComments = targetWorker.getComments().stream()
                    .map(CommentDTO::from)
                    .collect(Collectors.toList());
        }

        commentRepository.delete(c);

        logAdminAction(admin, "COMMENT_DELETED", "comment:" + id,
                "Comment deleted on worker " + (targetWorker != null ? targetWorker.getEmail() : "unknown"));

        return ResponseEntity.ok().body(remainingComments);
    }

    @PatchMapping("/comments/{id}")
    @Transactional
    public ResponseEntity<CommentDTO> editComment(@PathVariable Long id, @RequestBody String content, Admin admin) {
        if (content == null || content.isBlank() || content.length() > InputLimits.COMMENT_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid comment content.");
        }
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        comment.setContent(content.trim());

        logAdminAction(admin, "COMMENT_EDITED", comment.getId(), "Comment edited on worker " + comment.getWorker().getEmail());

        return ResponseEntity.ok(CommentDTO.from(comment));
    }

    @DeleteMapping("/comment/{commentId}")
    @Transactional
    public ResponseEntity<?> deleteCommentByAdmin(@PathVariable Long commentId, Admin admin) {
        Comment comm = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        Client author = comm.getAuthor();

        if(author != null){
            author.removeComment(comm);
        }

        if (comm.getWorker() != null) {
            comm.getWorker().removeComment(comm);
        }

        commentRepository.delete(comm);
        logAdminAction(admin, "COMMENT_DELETED", "comment:" + commentId,
                "Comment from " + (author != null ? author.getEmail() : "unknown") + " deleted by admin");

        return author != null ? ResponseEntity.ok(ClientDTO.from(author)) : ResponseEntity.ok().build();
    }

    // REPORTS

    @GetMapping("/reports-summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminWorkerReportDTO>> getReportsSummary() {
        List<Report> allReports = reportRepository.findAll();

        Map<Worker, List<Report>> reportsByWorker = allReports.stream()
                .collect(Collectors.groupingBy(Report::getWorker));

        List<AdminWorkerReportDTO> summary = reportsByWorker.entrySet().stream()
                .map(entry -> {
                    Worker worker = entry.getKey();
                    List<Report> reports = entry.getValue();

                    // On compte uniquement les signalements actifs (PENDING)
                    long activeCount = reports.stream()
                            .filter(r -> r.getStatus() == ReportStatus.PENDING)
                            .count();

                    List<AdminWorkerReportDTO.ReportDetailDTO> reportDetails = reports.stream()
                            .map(AdminWorkerReportDTO.ReportDetailDTO::from)
                            .sorted(Comparator.comparing(AdminWorkerReportDTO.ReportDetailDTO::createdAt).reversed())
                            .toList();

                    return new AdminWorkerReportDTO(
                            worker.getId(),
                            worker.getUsername(),
                            worker.getEmail().getValue(),
                            activeCount,
                            reportDetails
                    );
                })
                .filter(dto -> dto.reportCount() > 0)
                .sorted(Comparator.comparingLong(AdminWorkerReportDTO::reportCount).reversed())
                .toList();

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/reports/{reportId}/dismiss")
    @Transactional
    public ResponseEntity<?> dismissReport(@PathVariable Long reportId, Admin admin) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));

        report.setStatus(ReportStatus.DISMISSED);
        reportRepository.save(report);

        logAdminAction(admin, "DISMISS_REPORT", report, "Signalement classé sans suite pour le worker " + report.getWorker().getEmail() + " : "+ report.getStatus());

        // On diffuse la mise à jour aux autres admins connectés
        sseStreamService.broadcastAdminEvent("REPORT_UPDATED", report.getId());

        return ResponseEntity.ok(AdminWorkerReportDTO.ReportDetailDTO.from(report));
    }

    // ADMINS INVITATIONS

    @PostMapping("/invite")
    @Transactional
    public ResponseEntity<?> inviteAdmin(@Valid @RequestBody AdminInvitation req, Admin admin) {
        Email email;

        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
        } else {
            try { email = new Email(req.email()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Email invalide")); }
        }

        // 1. On vérifie les doublons
        if (userRepository.findByEmail(email.getValue()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Un utilisateur avec cet e-mail existe déjà"));
        }

        // 2. On crée l'Admin en BDD "verrouillé"
        Admin newAdmin = new Admin();
        newAdmin.setEmail(email);
        newAdmin.setUsername(email.toString());
        newAdmin.setLocked(true); // Verrouillé tant que le premier mot de passe n'est pas posé
        newAdmin.setPassword("PENDING_ACTIVATION_" + UUID.randomUUID()); // Chaîne BCrypt non valide par sécurité
        adminRepository.save(newAdmin);

        // 3. Réutilisation immédiate avec le paramètre d'invitation à true
        passwordResetService.createTokenAndSendEmail(newAdmin, true);

        // 4. Audit Log
        String snapshotJson = String.format("{\"invited_email\": \"%s\", \"role_assigned\": \"ADMIN\"}", email);
        logAdminAction(admin, "INVITE_ADMIN", snapshotJson, "Compte admin pré-créé. Flux de reset password déclenché pour activation.");

        return ResponseEntity.ok(AdminDTO.from(newAdmin));
    }

    // ADMIN LOGS

    @GetMapping("/logs")
    public ResponseEntity<List<AdminAuditLogDTO>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll().stream().map(AdminAuditLogDTO::from).collect(Collectors.toList()));
    }

    /**
     * An admin may act on clients and workers. Acting on another admin (ban, lock, delete)
     * requires the super admin role, and nobody can act on a super admin.
     */
    private static void requireAuthorityOver(Admin actor, VenusUser target) {
        if (target.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A super admin cannot be modified.");
        }
        if (target.isAdmin() && !actor.isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a super admin can modify an admin.");
        }
    }

    /** True if making {@code newParent} the parent of {@code zone} would create a loop. */
    private static boolean createsCycle(GeographicZone zone, GeographicZone newParent) {
        GeographicZone current = newParent;
        int depth = 0;
        while (current != null && depth++ < 100) {
            if (current.getId() != null && current.getId().equals(zone.getId())) return true;
            current = current.getParent();
        }
        return current != null;
    }

    private void logAdminAction(Admin admin, String actionType, Object target, String details) {
        // Full details (may contain personal data) stay in the audit table; the log only keeps who did what.
        log.info("ADMIN {} by admin {}{}", actionType, admin.getId(),
                target instanceof VenusUser u ? " on account " + u.getId() : "");
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(admin);
        log.setActionType(actionType);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());

        switch (target) {
            case VenusUser venusUser -> log.setTarget(venusUser);
            case GeographicZone geographicZone -> log.setTarget(geographicZone);
            case Service service -> log.setTarget(service);
            case LegalText legalText -> log.setTarget(legalText);
            case Comment comment -> log.setTarget(comment);
            case String s -> log.setTarget(s);
            default -> {
            }
        }

        auditLogRepository.save(log);
    }
}