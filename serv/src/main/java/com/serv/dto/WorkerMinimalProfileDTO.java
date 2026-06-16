package com.serv.dto;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;
import com.serv.service.WorkerService;

import java.util.List;
public record WorkerMinimalProfileDTO (
        String id,
        String username,
        int    age,
        ZoneLightDTO geographicZone, // 🎯 On remet l'objet attendu par le Front
        String bodyType,
        int galleryIndex,
        List<String> services,
        boolean available,
        String mainThumbUrl,
        List<String> previewThumbUrls
) implements Comparable<WorkerMinimalProfileDTO>{

    public record ZoneLightDTO(Integer id, String name) {}

    public static WorkerMinimalProfileDTO from(Worker w, List<String> preFetchedPreviews) {
        return new WorkerMinimalProfileDTO(
                w.getId().toString(),
                w.getUsername(),
                WorkerService.calculateAge(w.getBirthday()),
                w.getGeographicZone() != null ? new ZoneLightDTO(w.getGeographicZone().getId(), w.getGeographicZone().getName()) : null,
                w.getBodyType() != null ? w.getBodyType().toString() : null,
                w.getGalleryPositionIndex(),
                w.getServices().stream().map(Service::getName).toList(),
                w.isAvailable(),
                w.getMainPhoto() != null ? w.getMainPhoto().getMainThumbUrl() : null,
                preFetchedPreviews != null ? preFetchedPreviews : List.of()
        );
    }

    public static WorkerMinimalProfileDTO from(Worker w) {
        List<String> fallbackPreviews = w.getPhotos() != null ?
                w.getPhotos().stream().map(Photo::getPreviewThumbUrl).limit(5).toList() : List.of();
        return from(w, fallbackPreviews);
    }

    @Override
    public int compareTo(WorkerMinimalProfileDTO o) {
        return Integer.compare(o.galleryIndex(), this.galleryIndex());
    }
}