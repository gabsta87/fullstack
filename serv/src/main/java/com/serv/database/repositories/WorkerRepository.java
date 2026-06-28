package com.serv.database.repositories;

import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, UUID>, JpaSpecificationExecutor<Worker> {

    @NonNull
    Optional<Worker> findById(@NonNull UUID id);

    @Query("SELECT w FROM Worker w LEFT JOIN FETCH w.photos WHERE w.id = :id")
    Optional<Worker> findByIdWithPhotos(@Param("id") UUID id);

    @Query("SELECT w FROM Worker w LEFT JOIN FETCH w.photos WHERE w.email = :email")
    Optional<Worker> findByEmailWithPhotos(@Param("email") Email email);

    List<Worker> findByServicesId(Integer serviceId);
}
