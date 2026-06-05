package com.serv.dto;

import java.util.UUID;

public record PhotoDTO(
        UUID id,
        String originalUrl,
        String mainThumbUrl,
        String previewThumbUrl
) {
    public static PhotoDTO from(com.serv.database.entities.Photo p) {
        return new PhotoDTO(
                p.getId(),
                p.getOriginalUrl(),
                p.getMainThumbUrl(),
                p.getPreviewThumbUrl()
        );
    }
}
