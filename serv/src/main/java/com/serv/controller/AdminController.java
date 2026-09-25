package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.*;
import com.serv.database.repositories.*;
import com.serv.dto.*;
import com.serv.service.PasswordResetService;
import com.serv.service.SseStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    // ── PROFILES & LOGS ──────────────────────────────────────────────────────

    @GetMapping("/profiles")
    public ResponseEntity<List<Worker>> getAllProfiles() {
        return ResponseEntity.ok(workerRepository.findAll());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers() {
        List<AdminUserDTO> users = userRepository.findAll().stream()
                .map(AdminUserDTO::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/services")
    public ResponseEntity<List<Service>> getServices(){
        return ResponseEntity.ok(serviceRepository.findAll());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @PostMapping("/profiles/{id}/status")
    @Transactional
    public ResponseEntity<?> updateWorkerStatus(@PathVariable UUID id, @RequestBody Requests.AdminUpdateStatusRequest req, Admin admin) {
        Worker targetWorker = workerRepository.findById(id).orElse(null);
        if (targetWorker == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        StringBuilder changes = new StringBuilder();

        if (req.locked() != null) {
            targetWorker.setLocked(req.locked());
            changes.append("Verrouillé: ").append(req.locked()).append(" ");
        }
        if (req.banned() != null) {
            targetWorker.setBanned(req.banned());
            changes.append("Banni: ").append(req.banned()).append(" ");
        }
        if (req.hidden() != null) {
            targetWorker.setHidden(req.hidden());
            changes.append("Masqué: ").append(req.hidden()).append(" ");
        }

        Worker savedWorker = workerRepository.save(targetWorker);
        logAdminAction(admin, "UPDATE_WORKER_STATUS", targetWorker, "Statuts modifiés -> " + changes.toString().trim());

        sseStreamService.emitEvent(targetWorker.getId(), "WORKER_STATUS_UPDATED", savedWorker);
        return ResponseEntity.ok(WorkerFullProfileDTO.from(savedWorker));
    }

    @PostMapping("/profiles/update-days")
    @Transactional
    public ResponseEntity<?> updateDays(@RequestBody Requests.AdminUpdateDaysRequest req, Admin admin) {
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
    public ResponseEntity<?> verifyCertification(@RequestBody Requests.AdminVerifyCertifRequest req, Admin admin) {
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

    /**
     * 🔄 SAVE OR UPDATE unique pour les services
     */
    @PostMapping("/service")
    @Transactional
    public ResponseEntity<?> saveOrUpdateService(@RequestBody Requests.ServiceRequest service, Admin admin) {
        System.out.println("service : "+service);

        boolean isUpdate = service.id() != null;

        if (isUpdate) {
            System.out.println("update");
            Service existing = serviceRepository.findById(service.id()).orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();

            String oldName = existing.getName();
            existing.setName(service.name().trim());
            if(service.description() != null)
                existing.setDescription(service.description());
            serviceRepository.save(existing);

            logAdminAction(admin, "UPDATE_SERVICE", existing, String.format("Service %s renamed to %s", oldName, existing.getName()));
        } else {
            System.out.println("create");
            if (serviceRepository.findByName(service.name()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Service already exists");
            }
            Service newService = new Service();
            newService.setName(service.name().trim());
            if(service.description() != null)
                newService.setDescription(service.description());
            serviceRepository.save(newService);

            logAdminAction(admin, "CREATE_SERVICE", service, "Création du service : " + newService.getName());
        }

        sseStreamService.emitEvent(admin.getId(), "SERVICES_UPDATED", serviceRepository.findAll());
        return ResponseEntity.ok(serviceRepository.findAll().stream().map(ServiceDTO::from).collect(java.util.stream.Collectors.toList()));
    }

    @DeleteMapping("/services/{id}")
    @Transactional
    public ResponseEntity<?> deleteService(@PathVariable int id, Admin admin) {
        Service service = serviceRepository.findById(id).orElse(null);
        if (service == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Service introuvable."));
        }

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
        sseStreamService.emitEvent(admin.getId(), "SERVICES_UPDATED", updatedServices);

        // 6. Retour HTTP final
        return ResponseEntity.ok(updatedServices);
    }

    // ── GESTION DES TEXTES LÉGAUX ───────────────────────────────────

    @PostMapping("/legal")
    @Transactional
    public ResponseEntity<?> updateLegalText(@RequestBody Requests.LegalTextUpdateRequest req, Admin admin) {
        LegalText legal = new LegalText();
        legal.setName(req.key());
        legal.setContent(req.content());
        legal.setLastUpdate(LocalDateTime.now());
        legal.setAuthor(admin);
        legalTextRepository.save(legal);

        logAdminAction(admin, "UPDATE_LEGAL_TEXT", legal, "Mise à jour du texte légal : " + req.key());
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── GESTION DES RÉGIONS ───────────────────────────────────

    @Transactional(readOnly = true)
    @GetMapping("/locations-flat")
    public ResponseEntity<List<GeographicZoneWithParentDTO>> getAllLocationsFlat() {
        List<GeographicZoneWithParentDTO> zones = zoneRepository.findAll().stream()
                .map(GeographicZoneWithParentDTO::from)
                .toList();
        return ResponseEntity.ok(zones);
    }

    @DeleteMapping("/regions/{id}")
    @Transactional
    public ResponseEntity<?> deleteRegion(@PathVariable int id, Admin admin) {
        GeographicZone zone = geographicZoneRepository.findById(id).orElse(null);
        if (zone == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Région introuvable."));
        }

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
        sseStreamService.emitEvent(admin.getId(), "REGIONS_UPDATED", updatedZones);

        // 8. Retour HTTP final
        return ResponseEntity.ok(updatedZones);
    }

    @PostMapping("/region")
    @Transactional
    public ResponseEntity<List<GeographicZoneWithParentDTO>> updateRegion(Admin admin, @RequestBody Requests.RegionRequest region){
        System.out.println("Admin : "+admin);
        GeographicZone savedZone;
        if(region.id() == null){
            System.out.println("Region ID = null");
            // Create new Region
            GeographicZone newZone = new GeographicZone();
            newZone.setName(region.name());

            // CORRECTION ICI : On vérifie que le parentId n'est pas null avant de le chercher
            if (region.parentId() != null) {
                newZone.setParent(geographicZoneRepository.findById(region.parentId()).orElse(null));
            } else {
                newZone.setParent(null);
            }

            savedZone = geographicZoneRepository.save(newZone);

            sseStreamService.emitEvent(admin.getId(), "ZONE_CREATED", newZone);
            logAdminAction(admin,"ZONE_CREATED", savedZone,"Zone "+ region.name() +" with "+ (newZone.getParent() != null ? newZone.getParent().getName() : "no parent") +" created");
        }else{
            System.out.println("Region ID : "+" "+region.id());
            // Modify existing Region
            GeographicZone zone = geographicZoneRepository.findById(region.id()).orElse(null);
            if(zone == null) return ResponseEntity.notFound().build();

            String oldName = zone.getName();
            String oldParentName = (zone.getParent() != null) ? zone.getParent().getName() : "Aucun";

            // Gérer le cas où le parentId est fourni ou mis à null
            if (region.parentId() != null) {
                GeographicZone parentZone = geographicZoneRepository.findById(region.parentId()).orElse(null);
                if(parentZone == null) return ResponseEntity.notFound().build();
                zone.setParent(parentZone);
            } else {
                zone.setParent(null); // Devient une zone racine
            }

            zone.setName(region.name());
            geographicZoneRepository.save(zone);

            String newParentName = (zone.getParent() != null) ? zone.getParent().getName() : "Aucun";
            sseStreamService.emitEvent(admin.getId(), "ZONE_MODIFIED", zone);
            logAdminAction(admin, "ZONE_MODIFIED", zone, "Zone " + oldName + " with parent " + oldParentName + " modified to " + zone.getName() + " with parent " + newParentName);
        }
        return ResponseEntity.ok().body(geographicZoneRepository.findAll().stream().map(GeographicZoneWithParentDTO::from).collect(Collectors.toList()));
    }

    // ── GESTION DES COMMENTAIRES ───────────────────────────────────

    @DeleteMapping("/comments/{id}")
    @Transactional
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Admin admin) {
        if(commentRepository.findById(id).isEmpty()) return ResponseEntity.notFound().build();
        Worker targetWorker = commentRepository.findById(id).get().getWorker();
        commentRepository.deleteById(id);
        logAdminAction(admin, "COMMENT_DELETED", id, "Comment deleted on worker " + targetWorker.getEmail());
        return ResponseEntity.ok().body(targetWorker);
    }

    // ── ADMINS INVITATIONS ────────────────────────────────────
    @PostMapping("/admins/invite")
    @Transactional
    public ResponseEntity<?> inviteAdmin(@RequestBody Requests.AdminInviteRequest req, Admin admin) {
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