package com.serv.database.repositories;

import com.serv.database.entities.Client;
import com.serv.database.entities.Report;
import com.serv.database.entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByWorkerAndAuthor(Worker worker, Client author);
}
