package com.serv.database.repositories;

import com.serv.database.entities.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {

    Optional<Service> findByName(String serviceName);
}
