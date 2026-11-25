package com.serv.database.repositories;

import com.serv.common.TableNames;

import com.serv.database.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    @NonNull
    Optional<Client> findById(@NonNull UUID Id);

    @Modifying
    @Query(value = "DELETE FROM " + TableNames.CLIENT_FAVORITES + " WHERE worker_id = :workerId", nativeQuery = true)
    void removeWorkerFromAllFavorites(@Param("workerId") UUID workerId);

}
