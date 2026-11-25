package com.serv.dto;

import com.serv.database.entities.Comment;
import com.serv.database.entities.Worker;

/**
 * Comment exposed to the front end.
 * workerUsername / workerThumbUrl: the commented profile, used by the client's
 * "My comments" page to display the advertiser's thumbnail.
 */
public record CommentDTO(
        Long id,
        String content,
        String authorId,
        String workerId,
        String authorUsername,
        String date,
        String workerUsername,
        String workerThumbUrl
) {

    public static CommentDTO from(Comment c) {
        Worker w = c.getWorker();
        return new CommentDTO(
                c.getId(),
                c.getContent(),
                c.getAuthor() != null ? c.getAuthor().getId().toString() : null,
                w != null ? w.getId().toString() : null,
                c.getAuthor() != null ? c.getAuthor().getUsername() : null,
                c.getDate() != null ? c.getDate().toString() : null,
                w != null ? w.getUsername() : null,
                w != null && w.getMainPhoto() != null ? w.getMainPhoto().getMainThumbUrl() : null
        );
    }
}
