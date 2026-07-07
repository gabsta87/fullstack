package com.serv.database.repositories;

import com.serv.database.entities.LegalText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LegalTextRepository extends JpaRepository<LegalText, Integer> {
    LegalText findByName(String name);
}
