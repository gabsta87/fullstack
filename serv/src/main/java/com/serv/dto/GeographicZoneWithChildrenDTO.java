package com.serv.dto;

import com.serv.database.entities.GeographicZone;

import java.util.List;

public record GeographicZoneWithChildrenDTO(
        Integer id,
        String name,
        List<GeographicZoneWithChildrenDTO> subZones
) {
    public static GeographicZoneWithChildrenDTO from(GeographicZone zone) {
        return new GeographicZoneWithChildrenDTO(
                zone.getId(),
                zone.getName(),
                zone.getSubZones() != null ?
                zone.getSubZones().stream().map(GeographicZoneWithChildrenDTO::from).toList() : null
        );
    }
}
