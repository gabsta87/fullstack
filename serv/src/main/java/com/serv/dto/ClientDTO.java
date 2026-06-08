package com.serv.dto;

public record ClientDTO(
        String id,
        String username,
        String role,
        String location,
        String region,
        String email,
        String language,
        String[] favorites
) {
    public static ClientDTO from(com.serv.database.entities.Client c) {
        return new ClientDTO(
                c.getId().toString(),
                c.getUsername(),
                c.getRole().toString(),
                c.getLocation(),
                c.getRegion(),
                c.getEmail().toString(),
                c.getLanguage().toString(),
                c.getFavorites().stream().map(w -> w.getId().toString()).toList().toArray(String[]::new)
        );
    }
}
