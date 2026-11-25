package com.serv.database.repositories;

import com.serv.database.entities.LegalText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LegalTextRepository extends JpaRepository<LegalText, Long> {

    /** Current version of a legal text in a language. */
    Optional<LegalText> findFirstByNameAndLanguageOrderByVersionDesc(String name, String language);

    boolean existsByName(String name);
}
