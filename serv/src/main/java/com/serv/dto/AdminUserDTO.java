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
        Integer remainingDaysCredit, // Spécifique worker (null sinon)
        String description           // Pour la recherche par mots-clés
) {
    public static AdminUserDTO from(VenusUser user) {
        String role = "CLIENT";
        Integer credits = null;
        String desc = null;

        if (user instanceof Admin) {
            role = "ADMIN";
        } else if (user instanceof Worker w) {
            role = "WORKER";
            credits = w.getRemainingDaysCredit();
            desc = w.getDescription();
        }

        return new AdminUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail() != null ? user.getEmail().toString() : null,
                role,
                user.isLocked(),
                user.isBanned(),
                credits,
                desc
        );
    }
}