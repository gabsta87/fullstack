package com.serv.database.repositories;

import com.serv.database.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findByWorkerIdOrderBySortOrderAscIdAsc(UUID workerId);

    void deleteByWorkerId(UUID workerId);

    List<Video> findByWorkerId(UUID workerId);

    @org.springframework.data.jpa.repository.Query("SELECT v.id FROM Video v WHERE v.status = :status")
    List<UUID> findIdsByStatus(com.serv.common.VideoStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT v.worker.id FROM Video v WHERE v.id = :id")
    UUID findWorkerIdById(UUID id);
}