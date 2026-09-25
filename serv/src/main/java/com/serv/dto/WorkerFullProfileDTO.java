package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

public record WorkerFullProfileDTO(
        UUID         id,
        String       username,
        String       birthdate,
        GeographicZoneWithChildrenDTO geographicZone,
        String       bodyType,
        String       hairColor,
        String       eyeColor,
        String       email,
        String       role,
        List<String> services,
        Boolean      available,
        String       phone,
        String       description,
        String       mainThumbUrl,
        List<PhotoDTO> photos,
        List<VideoDTO> videos
) {
    public static WorkerFullProfileDTO from(Worker w) {
        String mainThumb = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl() : null;

        return new WorkerFullProfileDTO(
                w.getId(),
                w.getUsername(),
                w.getBirthdate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(w.getBirthdate()) : null,
                GeographicZoneWithChildrenDTO.from(w.getGeographicZone()),
                w.getBodyType() != null ? w.getBodyType().name() : null,
                w.getHairColor() != null ? w.getHairColor().toString() : null,
                w.getEyeColor() != null ? w.getEyeColor().toString() : null,
                w.getEmail() != null ? w.getEmail().toString() : null,
                w.getRole().name(),
                w.getServices().stream().map(Service::getName).toList(),
                w.isAvailable(),
                w.getPhone(),
                w.getDescription(),
                mainThumb,
                w.getPhotos().stream()
                        .map(PhotoDTO::from)
                        .toList(),
                List.of() // TODO: videos when Video entity is enabled
        );
    }

    public record VideoDTO(String id, String url, String duration) {}
}