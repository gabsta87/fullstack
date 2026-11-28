package com.serv.dto;

import com.serv.database.entities.AdminAuditLog;
import com.serv.database.entities.VenusUser;

import java.time.LocalDateTime;

/**
 * One audit entry, readable without looking anything else up: who (admin id and name), what (action, details)
 * and on what (target type, id and label, or the snapshot of something since deleted).
 */
public record AdminAuditLogDTO(
        Long id,
        String adminId,
        String adminName,
        String actionType,
        String details,
        String targetType,
        String targetId,
        String targetLabel,
        /** Email of the targeted account (admins only see the log), to reach it from the log */
        String targetEmail,
        String targetSnapshot,
        LocalDateTime createdAt
) {
    public static AdminAuditLogDTO from(AdminAuditLog log) {
        String type = null, id = null, label = null, email = null;
        if (log.getUserTarget() != null) {
            VenusUser u = log.getUserTarget();
            type = u.getRole().name();
            id = u.getId().toString();
            label = u.getUsername();
            email = u.getEmail() != null ? u.getEmail().getValue() : null;
        } else if (log.getServiceTarget() != null) {
            type = "SERVICE";
            id = String.valueOf(log.getServiceTarget().getId());
            label = log.getServiceTarget().getName();
        } else if (log.getRegionTarget() != null) {
            type = "REGION";
            id = String.valueOf(log.getRegionTarget().getId());
            label = log.getRegionTarget().getName();
        } else if (log.getLegalTextTarget() != null) {
            type = "LEGAL_TEXT";
            id = String.valueOf(log.getLegalTextTarget().getId());
            label = log.getLegalTextTarget().getName() + " (" + log.getLegalTextTarget().getLanguage() + ")";
        } else if ("SETTINGS_UPDATED".equals(log.getActionType())) {
            type = "SETTINGS";
        }
        VenusUser admin = log.getAdmin();
        return new AdminAuditLogDTO(
                log.getId(),
                admin.getId().toString(),
                admin.getUsername() != null ? admin.getUsername() : admin.getEmail().getValue(), // admins only see this
                log.getActionType(),
                log.getDetails(),
                type,
                id,
                label,
                email,
                log.getTargetSnapshot(),
                log.getCreatedAt()
        );
    }
}
