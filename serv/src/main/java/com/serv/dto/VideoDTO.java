package com.serv.dto;

import com.serv.database.entities.Video;

/**
 * @param status        READY, PROCESSING or FAILED (only READY videos reach visitors)
 * @param failureReason why processing failed (VIDEO_UNREADABLE, VIDEO_TOO_LONG…), null otherwise
 */
public record VideoDTO(String id, String url, String thumbnailUrl, Integer sortOrder, Integer duration,
                       String status, String failureReason) {

    public static VideoDTO from(Video v){
        return new VideoDTO(
                v.getId().toString(),
                v.getUrl(),
                v.getMainThumbUrl(),
                v.getSortOrder(),
                v.getDuration(),
                v.isReady() ? "READY" : v.getStatus().name(),
                v.getFailureReason()
        );
    }
}
