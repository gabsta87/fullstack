package com.serv.dto;

import com.serv.database.entities.Photo;

import java.util.UUID;

public record PhotoDTO(
        UUID id,
        String originalUrl,
        String mainThumbUrl,
        Integer sortOrder,
        /** Bytes on disk (original and thumbnail): helps the advertiser choose within the storage limit. */
        Long fileSize
) {
    public static PhotoDTO from(Photo p) {
        return new PhotoDTO(
                p.getId(),
                p.getUrl(),
                p.getMainThumbUrl(),
                p.getSortOrder(),
                p.getFileSize()
        );
    }
}
