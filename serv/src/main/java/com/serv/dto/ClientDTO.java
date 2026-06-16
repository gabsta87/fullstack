package com.serv.dto;

public record ClientDTO(
        String id,
        String username,
        String role,
        String email,
        String language,
        String[] favorites,
        GeographicZoneDTO geographicZone
) {
    public static ClientDTO from(com.serv.database.entities.Client c) {
        return new ClientDTO(
                c.getId().toString(),
                c.getUsername(),
                c.getRole().toString(),
                c.getEmail().toString(),
                c.getLanguage().toString(),
                c.getFavorites().stream().map(w -> w.getId().toString()).toList().toArray(String[]::new),
                GeographicZoneDTO.from(c.getGeographicZone())
        );
    }
}
