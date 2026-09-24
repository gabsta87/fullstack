package com.serv.database.repositories;

import com.serv.database.entities.Comment;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository  extends JpaRepository<Comment, Long> {

    @NonNull
    Optional<Comment> findById(@NonNull Long id);
}
