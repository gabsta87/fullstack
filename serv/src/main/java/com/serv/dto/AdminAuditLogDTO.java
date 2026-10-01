package com.serv.dto;

import com.serv.database.entities.AdminAuditLog;

import java.time.LocalDateTime;

public record AdminAuditLogDTO(
        Long id,
        String adminId,
        String actionType,
        String details,
        LocalDateTime createdAt
) {
    public static AdminAuditLogDTO from(AdminAuditLog log) {
        return new AdminAuditLogDTO(
                log.getId(),
                log.getAdmin().getId().toString(),
                log.getActionType(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}