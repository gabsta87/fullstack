package com.serv.dto;

import com.serv.database.entities.Comment;

import java.util.UUID;

public record CommentDTO (Long id, String content, UUID authorId, UUID workerId, String date) {

    public static CommentDTO from(Comment c){
        return new CommentDTO(
                c.getId(),
                c.getContent(),
                c.getUser().getId(),
                c.getWorker().getId(),
                c.getDate().toString()
        );
    }
}
