package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;

import java.util.UUID;

public record AdminUserDTO(
        UUID id,
        boolean available,
        boolean banned,
        boolean disabled,
        boolean locked,
        String role,
        String username,
        String email,
        String language,
        String birthdate,
        String description,
        String phone,
        String certificationStatus,
        String verificationCode,
        String certifiedAt,
        String certificationRequestDate,
        Integer[] servicesId,
        GeographicZoneWithParentDTO geographicZone
) {
    public static AdminUserDTO from(VenusUser user, String certificationRequestDateParam) {
        boolean available = false;
        boolean disabled = false; // ou user.isDisabled() selon ton entité VenusUser
        String role = user.getRole().name();
        String language = null;
        String birthdate = null;
        String phone = null;

        String certStatus = null;
        String verifCode = null;
        String certifiedAt = null;
        Integer[] servicesId = null;
        GeographicZoneWithParentDTO zoneDto = null;
        String desc = null;

        if (user instanceof Worker w) {
            available = w.isAvailable();
            language = w.getSpokenLanguages().toString();
            birthdate = w.getBirthdate() != null ? w.getBirthdate().toString() : null;
            phone = w.getPhone();
            desc = w.getDescription();

            certStatus = w.getLastCertificationRequest() != null ? w.getLastCertificationRequest().getStatus().toString() : null;
            verifCode = w.getVerificationCode();
            certifiedAt = w.getCertifiedAt() != null ? w.getCertifiedAt().toString() : null;

            if (w.getServices() != null) {
                servicesId = w.getServices().stream().map(Service::getId).toArray(Integer[]::new);
            }

            if (w.getGeographicZone() != null) {
                zoneDto = GeographicZoneWithParentDTO.from(w.getGeographicZone());
            }
        }

        return new AdminUserDTO(
                user.getId(),
                available,
                user.isBanned(),
                disabled,
                user.isLocked(),
                role,
                user.getUsername(),
                user.getEmail() != null ? user.getEmail().toString() : null,
                language,
                birthdate,
                desc,
                phone,
                certStatus,
                verifCode,
                certifiedAt,
                certificationRequestDateParam,
                servicesId,
                zoneDto
        );
    }

    public static AdminUserDTO from(VenusUser user) {
        return from(user, null);
    }
}