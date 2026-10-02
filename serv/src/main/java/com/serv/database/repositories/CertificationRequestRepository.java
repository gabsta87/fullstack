package com.serv.database.repositories;

import com.serv.common.CertificationStatus;
import com.serv.database.entities.CertificationRequest;
import com.serv.database.entities.Worker;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificationRequestRepository extends JpaRepository<CertificationRequest, Long> {

    Optional<CertificationRequest> findByWorker(Worker worker);

    List<CertificationRequest> findByStatus(CertificationStatus status);

    @NonNull
    List<CertificationRequest> findAll();
}
