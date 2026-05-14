package com.serv.database.repositories;

import com.serv.database.entities.Worker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    @NonNull
    Optional<Worker> findById(@NonNull UUID id);

    List<Worker> findByAvailableTrue(Pageable pageable);

    List<Worker> findByAvailableFalse(Pageable pageable);

    @Query("SELECT w FROM Worker w LEFT JOIN FETCH w.services")
    List<Worker> findAllWithServices();

    @NonNull
    @Override
    List<Worker> findAll();
}
