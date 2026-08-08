package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.service.PasswordResetService;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final WorkerRepository workerRepository;
    private final AdminRepository adminRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final GeographicZoneRepository geographicZoneRepository;
    private final ServiceRepository serviceRepository;
    private final LegalTextRepository legalTextRepository;
    private final SseStreamService sseStreamService;
    private final PasswordResetService passwordResetService;

    // ── PROFILES & LOGS ──────────────────────────────────────────────────────

    @GetMapping("/profiles")
    public ResponseEntity<List<Worker>> getAllProfiles() {
        return ResponseEntity.ok(workerRepository.findAll());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @PostMapping("/profiles/{id}/set-locked")
    @Transactional
    public ResponseEntity<?> setStatus(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @RequestParam boolean lock) {
        Admin admin = getAdminInfo(jwt);
        Worker targetWorker = workerRepository.findById(id).orElse(null);
        if (targetWorker == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Worker not found"));

        targetWorker.setLocked(lock);
        Worker savedWorker = workerRepository.save(targetWorker);

        logAdminAction(admin, "TOGGLE_STATUS", targetWorker, "Status modified : " + (targetWorker.isLocked() ? "LOCKED" : "UNLOCKED"));
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    @PostMapping("/profiles/update-days")
    @Transactional
    public ResponseEntity<?> updateDays(@RequestBody Requests.AdminUpdateDaysRequest req, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);
        Worker targetWorker = workerRepository.findById(UUID.fromString(req.workerId())).orElse(null);
        if (targetWorker == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Worker not found"));

        int oldDays = targetWorker.getRemainingDaysCredit();
        targetWorker.setRemainingDaysCredit(req.newDaysValue());

        Worker savedWorker = workerRepository.save(targetWorker);

        logAdminAction(admin, "UPDATE_CREDIT_DAYS", targetWorker, String.format("Jours modifiés: %d -> %d | Motif: %s", oldDays, req.newDaysValue(), req.reason()));
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    @PostMapping("/profiles/verify-certification")
    @Transactional
    public ResponseEntity<?> verifyCertification(@RequestBody Requests.AdminVerifyCertifRequest req, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);
        Worker targetWorker = workerRepository.findById(UUID.fromString(req.workerId())).orElse(null);
        if (targetWorker == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Worker not found"));

        if (req.approved()) {
            targetWorker.setCertificationStatus("CERTIFIED");
            targetWorker.setCertifiedAt(LocalDateTime.now());
            targetWorker.setCertificationExpiresAt(LocalDateTime.now().plusMonths(12));
        } else {
            targetWorker.setCertificationStatus("REJECTED");
        }
        Worker savedWorker = workerRepository.save(targetWorker);

        logAdminAction(admin, "VERIFY_CERTIFICATION", targetWorker, String.format("Certification : %s | Motif : %s", req.approved() ? "APPROUVEE" : "REFUSEE", req.rejectionReason()));
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    // ── GESTION DES SERVICES ────────────────────────

    @GetMapping("/services")
    public ResponseEntity<List<Service>> getAllServices() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    /**
     * 🔄 SAVE OR UPDATE unique pour les services
     */
    @PostMapping("/services")
    @Transactional
    public ResponseEntity<?> saveOrUpdateService(@RequestBody Service service, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);
        boolean isUpdate = service.getId() != null && service.getId() > 0;

        if (isUpdate) {
            Service existing = serviceRepository.findById(service.getId()).orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();

            String oldName = existing.getName();
            existing.setName(service.getName().trim());
            serviceRepository.save(existing);

            logAdminAction(admin, "UPDATE_SERVICE", existing, String.format("Service renommé : %s -> %s", oldName, existing.getName()));
            sseStreamService.emitEvent(admin.getId(), "SERVICE_UPDATED", existing);
        } else {
            if (serviceRepository.findByName(service.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Service already exists");
            }
            serviceRepository.save(service);

            logAdminAction(admin, "CREATE_SERVICE", service, "Création du service : " + service.getName());
            sseStreamService.emitEvent(admin.getId(), "SERVICE_CREATED", service);
        }

        return ResponseEntity.ok(serviceRepository.findAll());
    }

    // ── GESTION DES RÉGIONS & TEXTES LÉGAUX ───────────────────────────────────

    @DeleteMapping("/regions/{id}")
    @Transactional
    public ResponseEntity<?> deleteRegion(@PathVariable int id, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);
        int count = workerRepository.countByGeographicZoneId(id);
        if (count > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Impossible de supprimer cette région : " + count + " annonceur(s) y sont rattaché(s)."));
        }

        geographicZoneRepository.findById(id).ifPresent(zone -> {
            geographicZoneRepository.delete(zone);
            // On passe une chaîne JSON descriptive dans la colonne snapshot puisqu'on supprime la clé étrangère
            logAdminAction(admin, "DELETE_REGION", String.format("{\"id\": %d, \"name\": \"%s\"}", id, zone.getName()), "Suppression de la zone");
        });

        return ResponseEntity.ok().build();
    }

    @PostMapping("/legal")
    @Transactional
    public ResponseEntity<?> updateLegalText(@RequestBody Requests.LegalTextUpdateRequest req, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);

        LegalText legal = new LegalText();
        legal.setName(req.key());
        legal.setContent(req.content());
        legal.setLastUpdate(LocalDateTime.now());
        legal.setAuthor(admin); // Ajout de l'auteur légal pour l'historique !
        legalTextRepository.save(legal);

        logAdminAction(admin, "UPDATE_LEGAL_TEXT", legal, "Mise à jour du texte légal : " + req.key());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── ADMINS INVITATIONS ────────────────────────────────────
    @PostMapping("/admins/invite")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> inviteAdmin(@RequestBody Requests.AdminInviteRequest req, @AuthenticationPrincipal Jwt jwt) {
        Admin admin = getAdminInfo(jwt);
        Email email;

        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
        } else {
            try { email = new Email(req.email()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Email invalide")); }
        }

        // 1. On vérifie les doublons
        if (adminRepository.findByEmail(email).isPresent()) {
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

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Le compte administrateur a été pré-créé et le processus de configuration du mot de passe a été envoyé par e-mail."
        ));
    }

    // ── SECURED CONTROLLER HELPERS ──────────────────────────────────────────

    private Admin getAdminInfo(Jwt jwt) {
        if (jwt == null) return null;
        String userIdStr = jwt.getClaimAsString("userId");
        if (userIdStr != null) {
            try {
                return adminRepository.findById(UUID.fromString(userIdStr)).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }
        try{
            Email email = new Email(jwt.getClaimAsString("email"));
            return adminRepository.findByEmail(email).orElse(null);
        }catch (IllegalArgumentException ignored){
            System.out.println("Invalid JWT email: " + jwt.getClaimAsString("email"));
            return null;
        }
    }

    // Encapsulation centralisée des surcharges de logs
    private void logAdminAction(Admin admin, String actionType, Object target, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(admin);
        log.setActionType(actionType);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());

        if (target instanceof VenusUser) log.setTarget((VenusUser) target);
        else if (target instanceof GeographicZone) log.setTarget((GeographicZone) target);
        else if (target instanceof Service) log.setTarget((Service) target);
        else if (target instanceof LegalText) log.setTarget((LegalText) target);
        else if (target instanceof Comment) log.setTarget((Comment) target);
        else if (target instanceof String) log.setTarget((String) target); // snapshot JSON

        auditLogRepository.save(log);
    }
}