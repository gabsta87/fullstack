package com.serv.database.repositories;

import com.serv.database.Email;
import com.serv.database.VenusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<VenusUser, UUID> {
    @NonNull
    Optional<VenusUser> findById(@NonNull UUID id);
    Optional<VenusUser> findByUsername(@NonNull String username);
    Optional<VenusUser> findByEmail(@NonNull Email email);
}
