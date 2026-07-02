package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Data
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private String adminId; // L'UUID de l'admin qui a fait l'action

    @Column(name = "admin_username")
    private String adminUsername;

    @Column(name = "action_type", nullable = false)
    private String actionType; // ex: "UPDATE_DAYS", "CERTIFY_PROFILE", "TOGGLE_STATUS"

    @Column(name = "target_worker_id")
    private String targetWorkerId; // L'UUID de l'annonceur impacté

    @Column(name = "details", length = 1000)
    private String details; // ex: "Passage du crédit de 5 jours à 30 jours"

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}