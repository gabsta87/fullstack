package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;

import java.util.List;

public record WorkerMinimalProfileDTO (
        String        id,
        String        username,
        Integer       age,
        ZoneLightDTO  geographicZone,
        String        bodyType,
        String        hairColor,
        String        eyeColor,
        Boolean       isCertified,
        String        shortDescription,
        Integer       galleryIndex,
        List<Integer> servicesId,
        Boolean       available,
        String        mainThumbUrl,
        String        certifiedAt
) implements Comparable<WorkerMinimalProfileDTO>{

    public record ZoneLightDTO(Integer id, String name) {}

    public static WorkerMinimalProfileDTO from(Worker w) {
        return new WorkerMinimalProfileDTO(
                w.getId().toString(),
                w.getUsername(),
                w.getAge(),
                w.getGeographicZone() != null ? new ZoneLightDTO(w.getGeographicZone().getId(), w.getGeographicZone().getName()) : null,
                w.getBodyType() != null ? w.getBodyType().toString() : null,
                w.getHairColor() != null ? w.getHairColor().toString() : null,
                w.getEyeColor() != null ? w.getEyeColor().toString() : null,
                w.isCertified(),
                w.getShortDescription(),
                w.getGalleryPositionPriority(),
                w.getServices().stream().map(Service::getId).toList(),
                w.isAvailable(),
                w.getMainPhoto() != null ? w.getMainPhoto().getMainThumbUrl() : null,
                w.getCertifiedAt() != null ? w.getCertifiedAt().toString() : null
        );
    }

    @Override
    public int compareTo(WorkerMinimalProfileDTO o) {
        return Integer.compare(o.galleryIndex(), this.galleryIndex());
    }
}