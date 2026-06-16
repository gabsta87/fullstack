package com.serv.dto;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;
import com.serv.service.WorkerService;

import java.util.List;

import static com.serv.service.WorkerService.MAX_PREVIEW_THUMBS;

public record WorkerMinimalProfileDTO(
        String id,
        String username,
        int    age,
        GeographicZoneDTO geographicZone,
        String bodyType,
        int galleryIndex,
        List<String>services,
        boolean available,
        String mainThumbUrl,
        List<String> previewThumbUrls
) implements Comparable<WorkerMinimalProfileDTO>{

    public static WorkerMinimalProfileDTO from(Worker w){
        return new WorkerMinimalProfileDTO(
                w.getId().toString(), w.getUsername(), WorkerService.calculateAge(w.getBirthday()),
                GeographicZoneDTO.from(w.getGeographicZone()),
                w.getBodyType() != null ? w.getBodyType().toString() : null,
                w.getGalleryPositionIndex(),
                w.getServices().stream().map(Service::getName).toList(),
                w.isAvailable(),
                w.getMainPhoto() != null ? w.getMainPhoto().getMainThumbUrl() : null,
                w.getPhotos().stream().map(Photo::getPreviewThumbUrl).limit(MAX_PREVIEW_THUMBS).toList()
        );
    }

    @Override
    public int compareTo(WorkerMinimalProfileDTO o) {
        return o.galleryIndex - this.galleryIndex;
    }
}
