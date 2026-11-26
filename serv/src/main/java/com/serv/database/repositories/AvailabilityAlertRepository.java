package com.serv.database.repositories;

import com.serv.database.entities.AvailabilityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilityAlertRepository extends JpaRepository<AvailabilityAlert, Long> {

    Optional<AvailabilityAlert> findByClientIdAndWorkerId(UUID clientId, UUID workerId);

    long countByClientId(UUID clientId);

    @Query("SELECT a.worker.id FROM AvailabilityAlert a WHERE a.client.id = :clientId")
    List<UUID> findWorkerIdsByClientId(UUID clientId);

    @Query("SELECT a FROM AvailabilityAlert a JOIN FETCH a.client WHERE a.worker.id = :workerId")
    List<AvailabilityAlert> findAllWithClientByWorkerId(UUID workerId);
}
