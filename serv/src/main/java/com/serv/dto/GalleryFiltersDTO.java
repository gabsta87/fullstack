package com.serv.dto;

import com.serv.database.entities.Client;

import java.util.List;
import java.util.Objects;

public record GalleryFiltersDTO (
        Integer zoneId,
        String bodyType,
        String eyeColor,
        String hairColor,
        Integer minAge,
        Integer maxAge,
        List<String> services,
        List<String> languages
) {

    public static GalleryFiltersDTO from(Client c) {
        return new GalleryFiltersDTO(
                c.getGeographicZone() != null ? c.getGeographicZone().getId() : null,
                c.getPreferredBodyType() != null ?  c.getPreferredBodyType().toString() : null,
                c.getPreferredEyeColor() != null ? c.getPreferredEyeColor().toString() : null,
                c.getPreferredHairColor() != null ? c.getPreferredHairColor().toString() :null,
                c.getPreferredMinAge() != null ?  c.getPreferredMinAge() : null,
                c.getPreferredMaxAge() != null ? c.getPreferredMaxAge() : null,
                c.getPreferredServices() != null && !c.getPreferredServices().isEmpty() ? c.getPreferredServices().stream().map(Objects::toString).toList() : null,
                c.getPreferredLanguages() != null && !c.getPreferredLanguages().isEmpty() ? c.getPreferredLanguages().stream().map(Objects::toString).toList() : null
        );
    }
}
