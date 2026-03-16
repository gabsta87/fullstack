package com.serv.database.repositories;

import com.serv.database.entities.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /** Only the main profile photo */
    Optional<Photo> findByWorkerId(UUID workerId);

    /** Does the worker already have a main photo? */
    boolean existsByWorkerId(UUID workerId);

    List<Photo> findByWorkerIdOrderBySortOrderAscIdAsc(UUID workerId);
    List<Photo> findByWorkerIdInOrderBySortOrderAsc(List<UUID> workerIds);
    void deleteByWorkerId(UUID workerId);

    Optional<Photo> findById(UUID photoId);

    /**
     * Returns only the preview thumbnails for a list of worker IDs.
     * Used by the gallery: loads card data without fetching original URLs.
     *
     * Returns rows as Object[] : [workerId, previewThumbUrl, sortOrder]
     */
    @Query("""
        SELECT p.worker.id, p.previewThumbUrl, p.sortOrder
        FROM Photo p
        WHERE p.worker.id IN :workerIds
        ORDER BY p.worker.id, p.sortOrder
        """)
    List<Object[]> findPreviewThumbsByWorkerIds(@Param("workerIds") List<UUID> workerIds);

}