package com.serv.dto;

import com.serv.database.entities.GeographicZone;

public record GeographicZoneWithParentDTO(Integer id, String name, Integer parentId) {

    public static GeographicZoneWithParentDTO from(GeographicZone zone) {
        return new GeographicZoneWithParentDTO(zone.getId(),zone.getName(), zone.getParent() != null ? zone.getParent().getId() : null);
    }
}
