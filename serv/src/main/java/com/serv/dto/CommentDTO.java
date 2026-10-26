package com.serv.dto;

import com.serv.database.entities.Comment;

import java.util.UUID;

public record CommentDTO (Long id, String content, String authorId, String workerId, String authorUsername, String date) {

    public static CommentDTO from(Comment c){
        return new CommentDTO(
                c.getId(),
                c.getContent(),
                c.getAuthor() != null ? c.getAuthor().getId().toString() : null,
                c.getWorker() != null ? c.getWorker().getId().toString() : null,
                c.getAuthor() != null ? c.getAuthor().getUsername() : null,
                c.getDate() != null ? c.getDate().toString() : null
        );
    }
}
