package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;

import java.util.List;
import java.util.UUID;

public record WorkerPublicFullProfileDTO(
        UUID id,
        String            username,
        String            role,
        GeographicZoneDTO geographicZone,
        String            bodyType,
        Boolean           available,
        List<String>      services,
        String            phone,
        String            description,
        String            mainThumbUrl,
        Integer           age,
        List<PhotoDTO>    photos,
        List<VideoDTO>    videos
) {
    public static WorkerPublicFullProfileDTO from(Worker w) {
        String mainThumb = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl() : null;

        return new WorkerPublicFullProfileDTO(
                w.getId(),
                w.getUsername(),
                w.getRole().name(),
                GeographicZoneDTO.from(w.getGeographicZone()),
                w.getBodyType() != null ? w.getBodyType().name() : null,
                w.isAvailable(),
                w.getServices().stream().map(Service::getName).toList(),
                w.getPhone(),
                w.getDescription(),
                mainThumb,
                WorkerMinimalProfileDTO.calculateAge(w.getBirthdate()),
                w.getPhotos() != null ? w.getPhotos().stream().map(PhotoDTO::from).toList() : List.of(),
                List.of() // TODO: videos quand l'entité sera prête
        );
    }

    public record VideoDTO(String id, String url, String duration) {}
}
