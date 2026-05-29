package com.serv.dto;

import com.serv.database.entities.VenusUser;

public record VenusUserDTO(
    String id,
    String username,
    String location,
    String role,
    String language,
    String region
) {
    public static VenusUserDTO from(VenusUser u) {
        return new VenusUserDTO(
                u.getId().toString(),
                u.getUsername(),
                u.getLocation(),
                u.getRole().toString(),
                u.getLanguage() != null ? u.getLanguage().toString() : "",
                u.getRegion()
        );
    }

}
