package com.serv.database.repositories;

import com.serv.database.entities.GeographicZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeographicZoneRepository extends JpaRepository<GeographicZone, Integer>{

    Optional<GeographicZone> findByName(String name);
}
