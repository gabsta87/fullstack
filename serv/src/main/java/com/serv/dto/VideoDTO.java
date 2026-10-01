package com.serv.dto;

import com.serv.database.entities.Video;

public record VideoDTO(String id, String url, Integer sortOrder, Integer duration) {

    public static VideoDTO from(Video v){
        return new VideoDTO(
                v.getId().toString(),
                v.getUrl(),
                v.getSortOrder(),
                v.getDuration()
        );
    }
}