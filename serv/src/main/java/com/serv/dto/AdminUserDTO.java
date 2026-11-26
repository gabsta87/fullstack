package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;

import java.util.List;
import java.util.UUID;

public record AdminUserDTO(
        UUID id,
        boolean available,
        boolean banned,
        boolean locked,
        boolean hidden,
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
        GeographicZoneWithParentDTO geographicZone,
        /** Why a worker profile is not public (see {@link Worker#profileIssues()}); empty otherwise. */
        List<String> profileIssues
) {
    /**
     * @param online for admins: whether they are connected right now (shown in the "available" column);
     *               workers show their own availability, clients nothing
     */
    public static AdminUserDTO from(VenusUser user, String certificationRequestDateParam, boolean online) {
        boolean available = user.isAdmin() && online;
        boolean hidden = false;
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
        List<String> profileIssues = List.of();

        if (user instanceof Worker w) {
            available = w.isAvailable();
            hidden = w.isHidden();
            language = w.getSpokenLanguages().toString();
            birthdate = w.getBirthdate() != null ? w.getBirthdate().toString() : null;
            phone = w.getPhone();
            desc = w.getDescription();
            profileIssues = w.profileIssues();

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
                user.isLocked(),
                hidden,
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
                zoneDto,
                profileIssues
        );
    }

    public static AdminUserDTO from(VenusUser user, boolean online) {
        return from(user, null, online);
    }
}