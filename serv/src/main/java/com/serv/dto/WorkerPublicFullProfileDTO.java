package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;
import com.serv.database.entities.WorkerLanguage;

import java.util.List;
import java.util.UUID;

public record WorkerPublicFullProfileDTO(
        UUID              id,
        String            username,
        String            role,
        GeographicZoneWithChildrenDTO geographicZone,
        String            bodyType,
        String            eyeColor,
        String            hairColor,
        Boolean           available,
        List<Integer>     servicesId,
        String            phone,
        String            description,
        String            mainThumbUrl,
        Integer           age,
        List<PhotoDTO>    photos,
        List<VideoDTO>    videos,
        List<String>      languages,
        String            certifiedAt,
        List<CommentDTO>  comments
) {
    public static WorkerPublicFullProfileDTO from(Worker w) {
        String mainThumb = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl() : null;

        return new WorkerPublicFullProfileDTO(
                w.getId(),
                w.getUsername(),
                w.getRole().name(),
                GeographicZoneWithChildrenDTO.from(w.getGeographicZone()),
                w.getBodyType() != null ? w.getBodyType().name() : null,
                w.getEyeColor() != null ? w.getEyeColor().name() : null,
                w.getHairColor() != null ? w.getHairColor().name() : null,
                w.isAvailable(),
                w.getServices().stream().map(Service::getId).toList(),
                w.getPhone(),
                w.getDescription(),
                mainThumb,
                w.getAge(),
                w.getPhotos() != null ? w.getPhotos().stream().map(PhotoDTO::from).toList() : List.of(),
                w.getVideos() != null ? w.getVideos().stream().map(VideoDTO::from).toList() : List.of(),
                w.getSpokenLanguages().stream().map(WorkerLanguage::toString).toList(),
                w.getCertifiedAt() != null ? w.getCertifiedAt().toString() : null,
                w.getComments() != null ? w.getComments().stream().map(CommentDTO::from).toList() : List.of()
        );
    }
}
