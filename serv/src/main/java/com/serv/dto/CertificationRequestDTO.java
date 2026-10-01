package com.serv.dto;

import com.serv.common.CertificationStatus;
import com.serv.database.entities.CertificationRequest;

import java.time.LocalDateTime;

public record CertificationRequestDTO(
        Long id,
        String workerId,
        String workerUsername,
        String verificationCode,
        String certificationPhotoUrl,
        CertificationStatus status,
        boolean underReview,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        LocalDateTime lockedAt,
        String comment
) {
    public static CertificationRequestDTO from(CertificationRequest req) {
        return new CertificationRequestDTO(
                req.getId(),
                req.getWorker().getId().toString(),
                req.getWorker().getUsername(),
                req.getVerificationCode(),
                req.getCertificationPhoto() != null ? req.getCertificationPhoto().getUrl() : null,
                req.getStatus(),
                req.isUnderReview(),
                req.getCreatedAt(),
                req.getProcessedAt(),
                req.getLockedAt(),
                req.getComment()
        );
    }
}