package com.serv.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @NonNull
    Optional<Client> findById(@NonNull Long Id);
}
