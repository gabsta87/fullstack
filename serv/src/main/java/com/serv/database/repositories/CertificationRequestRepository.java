package com.serv.database.repositories;

import com.serv.database.entities.CertificationRequest;
import com.serv.database.entities.Worker;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificationRequestRepository extends JpaRepository<CertificationRequest, Long> {

    Optional<CertificationRequest> findByWorkerAndStatus(Worker worker, String status);

    Optional<CertificationRequest> findByWorker(Worker worker);

    @NonNull
    List<CertificationRequest> findAll();
}
