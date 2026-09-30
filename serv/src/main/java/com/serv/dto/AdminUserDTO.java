package com.serv.dto;

import com.serv.database.entities.Admin;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.Worker;

import java.util.UUID;
public record AdminUserDTO(
        UUID id,
        String username,
        String email,
        String role,           // "ADMIN", "CLIENT", "WORKER"
        boolean locked,
        boolean banned,
        Integer remainingDaysCredit,
        String description,
        String certificationStatus,
        String verificationCode,
        String certificationPhotoUrl
) {
    public static AdminUserDTO from(VenusUser user) {
        String role = "CLIENT";
        Integer credits = null;
        String desc = null;
        String certStatus = null;
        String verifCode = null;
        String photoUrl = null;

        if (user instanceof Admin) {
            role = "ADMIN";
        } else if (user instanceof Worker w) {
            role = "WORKER";
            credits = w.getRemainingDaysCredit();
            desc = w.getDescription();
            certStatus = w.getCertificationStatus() != null ? w.getCertificationStatus().name() : null;
            verifCode = w.getVerificationCode();
            if (w.getCertificationPhoto() != null) {
                photoUrl = w.getCertificationPhoto().getUrl();
            }
        }

        return new AdminUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail() != null ? user.getEmail().toString() : null,
                role,
                user.isLocked(),
                user.isBanned(),
                credits,
                desc,
                certStatus,
                verifCode,
                photoUrl
        );
    }
}