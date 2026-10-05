package com.serv.database.repositories;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Worker;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    Optional<Photo> findByWorker(Worker worker);

    void deleteByWorkerId(UUID workerId);

    @Nonnull
    Optional<Photo> findById(UUID photoId);

}