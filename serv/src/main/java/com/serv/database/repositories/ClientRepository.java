package com.serv.database.repositories;

import com.serv.database.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    @NonNull
    Optional<Client> findById(@NonNull UUID Id);

}
