package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record WorkerProfileDTO(
        UUID         id,
        String       name,
        int          age,
        String       location,
        String       region,
        String       bodyType,
        int          height,
        int          weight,
        List<Service> services,
        boolean      available,
        String       responseTime,
        String       phone,
        String       description,
        String       mainThumbUrl,
        List<PhotoDTO> photos,
        List<VideoDTO> videos
) {
    public static WorkerProfileDTO from(Worker w) {
        String mainThumb = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl() : null;

        List<PhotoDTO> photos = w.getPhotos().stream()
                .map(p -> new PhotoDTO(
                        p.getId().toString(),
                        p.getOriginalUrl(),
                        p.getMainThumbUrl(),
                        p.getPreviewThumbUrl()))
                .toList();

        return new WorkerProfileDTO(
                w.getId(),
                w.getUsername(),
                calculateAge(w.getBirthday()),
                w.getLocation(),
                w.getRegion(),
                w.getBodyType() != null ? w.getBodyType().name() : null,
                w.getHeight(),
                w.getWeight(),
                w.getServices(),
                w.isAvailable(),
                "< 30 min",   // TODO: real response time
                "",           // TODO: expose phone only to logged-in users
                "",           // TODO: add description field to Worker
                mainThumb,
                photos,
                List.of()     // TODO: videos when Video entity is enabled
        );
    }

    private static int calculateAge(java.util.Date birthday) {
        if (birthday == null) return 0;

        LocalDate birthDate;

        // java.sql.Date has toLocalDate() which works correctly.
        // java.util.Date must go through Calendar instead.
        if (birthday instanceof java.sql.Date sqlDate) {
            birthDate = sqlDate.toLocalDate();
        } else {
            birthDate = birthday.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public record PhotoDTO(
            String id,
            String originalUrl,
            String mainThumbUrl,
            String previewThumbUrl
    ) {}

    public record VideoDTO(String id, String url, String duration) {}
}