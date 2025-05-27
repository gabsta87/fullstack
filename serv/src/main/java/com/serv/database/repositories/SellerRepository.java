package com.serv.database.repositories;

import com.serv.database.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    @NonNull
    Optional<Seller> findById(@NonNull UUID id);

    @NonNull
    @Override
    List<Seller> findAll();
}
