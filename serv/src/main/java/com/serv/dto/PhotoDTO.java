package com.serv.dto;

import com.serv.database.entities.Photo;

import java.util.UUID;

public record PhotoDTO(
        UUID id,
        String originalUrl,
        String mainThumbUrl
) {
    public static PhotoDTO from(Photo p) {
        return new PhotoDTO(
                p.getId(),
                p.getUrl(),
                p.getMainThumbUrl()
        );
    }
}
