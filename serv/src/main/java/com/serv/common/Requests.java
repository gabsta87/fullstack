package com.serv.common;

import com.serv.dto.VenusUserDTO;

import java.util.List;

public class Requests {

    public record RegisterRequest(String email, String password) { }

    public record RegionRequest(Integer id, String name, Integer parentId) { }

    public record ServiceRequest(Integer id, String name, String description) { }

    public record ForgotPasswordRequest (String email) { }

    public record AdminUpdateStatusRequest(
            Boolean locked,
            Boolean banned,
            Boolean hidden
    ) {}

    public record LoginRequest(String email, String password) { }
    public record LoginResponse(String token, VenusUserDTO user) { }

    public record AdminUpdateDaysRequest(String workerId, int newDaysValue, String reason) {}
    public record AdminVerifyCertifRequest(String workerId, boolean approved, String rejectionReason) {}
    public record LegalTextUpdateRequest(String key, String content) {}
    public record AdminInviteRequest(String email) {}

    public record WorkerProfileUpdateRequest(
            String username,
            String description,
            Integer geographicZoneId,
            String eyeColor,
            String hairColor,
            String phone,
            String bodyType,
            String mainPhotoId,
            List<String> services,
            String birthdate
    ) {}

    public record AccountDataRequest(
            String username,
            String email,
            String password
    ) {}

    public record WorkerSearchRequest(
            Integer page,
            String zoneId,
            String username,
            String gender,
            String bodyType,
            String eyeColor,
            String hairColor,
            Integer minAge,
            Integer maxAge,
            List<String> languages,
            List<String> services
    ) {
        public int getPageOrZero() {
            return page != null ? page : 0;
        }
    }

    public record UpdateDaysRequest(String workerId, int newDaysValue, String reason) {}

    public record CertificationApprovalRequest(String workerId, boolean approved, String rejectionReason) {}

    public record AdminUpdateProfileRequest(
            String workerId,
            Boolean disabled,               // true = forcer en ligne, false = forcer hors-ligne
            Integer remainingDaysCredit,  // Nouvelle valeur de jours si modifiée
            String certificationStatus,   // "CERTIFIED", "REJECTED", etc.
            String reason                 // Justification obligatoire pour l'audit
    ) {}
}
