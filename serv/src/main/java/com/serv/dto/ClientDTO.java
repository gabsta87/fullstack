package com.serv.dto;

import java.util.ArrayList;
import java.util.List;

public record ClientDTO(
        String id,
        String username,
        String role,
        String email,
        List<WorkerMinimalProfileDTO> favorites,
        GeographicZoneWithChildrenDTO geographicZone
) {
    public static ClientDTO from(com.serv.database.entities.Client c) {
        return new ClientDTO(
                c.getId().toString(),
                c.getUsername(),
                c.getRole().toString(),
                c.getEmail().toString(),
                c.getFavorites() != null ?
                        c.getFavorites().stream()
                            .map(WorkerMinimalProfileDTO::from)
                            .toList() :
                        new ArrayList<>(),
                c.getGeographicZone() != null ?
                        GeographicZoneWithChildrenDTO.from(c.getGeographicZone()) :
                        null
        );
    }
}
