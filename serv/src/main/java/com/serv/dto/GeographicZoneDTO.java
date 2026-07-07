package com.serv.dto;

import com.serv.database.entities.GeographicZone;
import lombok.Data;
import java.util.List;

public record GeographicZoneDTO (
        Integer id,
        String name,
        List<GeographicZoneDTO> subZones
) {
    public static GeographicZoneDTO from(GeographicZone zone) {
        return new GeographicZoneDTO(
                zone.getId(),
                zone.getName(),
                zone.getSubZones() != null ?
                zone.getSubZones().stream().map(GeographicZoneDTO::from).toList() : null
        );
    }
}
