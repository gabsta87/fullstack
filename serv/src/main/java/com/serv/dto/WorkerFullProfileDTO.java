package com.serv.dto;

import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record WorkerFullProfileDTO(
        UUID         id,
        String       username,
        int          age,
        String       location,
        String       region,
        String       bodyType,
        String       role,
        int          height,
        int          weight,
        List<String> services,
        boolean      available,
        String       phone,
        String       description,
        String       mainThumbUrl,
        List<PhotoDTO> photos,
        List<VideoDTO> videos
) {
    public static WorkerFullProfileDTO from(Worker w) {
        String mainThumb = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl() : null;

        List<PhotoDTO> photos = w.getPhotos().stream()
                .map(p -> new PhotoDTO(
                        p.getId().toString(),
                        p.getOriginalUrl(),
                        p.getMainThumbUrl(),
                        p.getPreviewThumbUrl()))
                .toList();

        return new WorkerFullProfileDTO(
                w.getId(),
                w.getUsername(),
                calculateAge(w.getBirthday()),
                w.getLocation(),
                w.getRegion(),
                w.getBodyType() != null ? w.getBodyType().name() : null,
                w.getRole().name(),
                w.getHeight(),
                w.getWeight(),
                w.getServices().stream().map(Service::getName).toList(),
                w.isAvailable(),
                w.getPhone(),
                w.getDescription(),
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