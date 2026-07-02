package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.AdminAuditLog;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.AdminAuditLogRepository;
import com.serv.database.repositories.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')") // Décommente si tu utilises Spring Security
public class AdminController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private AdminAuditLogRepository auditLogRepository;

    /**
     * 1. Récupérer tous les profils (actifs et inactifs) pour modération
     */
    @GetMapping("/profiles")
    public ResponseEntity<List<Worker>> getAllProfiles() {
        return ResponseEntity.ok(workerRepository.findAll());
    }

    @PostMapping("/profiles/update")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Requests.AdminUpdateProfileRequest request,
            @RequestHeader("X-Admin-Id") String adminId,
            @RequestHeader("X-Admin-Username") String adminUsername) {

        UUID workerId;
        try{
            workerId = UUID.fromString(request.workerId());
        }catch(IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid worker ID"));
        }

        if (request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Un motif est requis pour toute action administrative."));
        }

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Worker not found"));

        StringBuilder auditDetails = new StringBuilder("Mise à jour du profil. Changements : ");

        // 1. Contrôle Idempotent du Statut En ligne / Hors ligne
        if (request.active() != null) {
            if (worker.isActive() != request.active()) {
                worker.setActive(request.active());
                auditDetails.append(String.format("[Statut: %s] ", request.active() ? "ACTIVE" : "INACTIVE"));
            }
        }

        // 2. Modification du crédit de jours
        if (request.remainingDaysCredit() != null) {
            int oldDays = worker.getRemainingDaysCredit();
            if (oldDays != request.remainingDaysCredit()) {
                worker.setRemainingDaysCredit(request.remainingDaysCredit());
                auditDetails.append(String.format("[Jours: %d -> %d] ", oldDays, request.remainingDaysCredit()));
            }
        }

        // 3. Modification de la certification
        if (request.certificationStatus() != null) {
            worker.setCertificationStatus(request.certificationStatus());
            if ("CERTIFIED".equals(request.certificationStatus())) {
                worker.setCertifiedAt(LocalDateTime.now());
                worker.setCertificationExpiresAt(LocalDateTime.now().plusMonths(12));
            }
            auditDetails.append(String.format("[Certification: %s] ", request.certificationStatus()));
        }

        workerRepository.save(worker);

        AdminAuditLog log = new AdminAuditLog();
        log.setAdminId(adminId);
        log.setAdminUsername(adminUsername);
        log.setActionType("ADMIN_PROFILE_UPDATE");
        log.setTargetWorkerId(request.workerId());
        log.setDetails(auditDetails.toString() + " | Motif : " + request.reason());
        auditLogRepository.save(log);

        return ResponseEntity.ok(Map.of("success", true, "worker", worker));
    }
}