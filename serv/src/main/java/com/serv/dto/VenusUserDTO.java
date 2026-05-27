package com.serv.dto;

import com.serv.common.Language;
import com.serv.database.entities.VenusUser;

import java.util.UUID;

public record VenusUserDTO(
    UUID id,
    String name,
    String location,
    String role,
    Language lang
) {
    public static VenusUserDTO from(VenusUser u) {
        return new VenusUserDTO(
                u.getId(),
                u.getUsername(),
                u.getLocation(),
                u.getRole().toString(),
                u.getLanguage()
        );
    }

}
