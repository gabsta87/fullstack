package com.serv.database.repositories;

import com.serv.database.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByWorkerIdOrderBySortOrderAscIdAsc(UUID workerId);

    void deleteByWorkerId(UUID workerId);
}