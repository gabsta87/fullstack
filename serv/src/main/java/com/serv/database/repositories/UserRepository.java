package com.serv.database.repositories;

import com.serv.database.entities.Email;
import com.serv.database.entities.VenusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<VenusUser, UUID> {
    @NonNull
    Optional<VenusUser> findById(@NonNull UUID id);
    Optional<VenusUser> findByUsername(@NonNull String username);

    @Query("SELECT u FROM VenusUser u WHERE u.email.value = :email")
    Optional<VenusUser> findByEmail(@Param("email") String email);}
