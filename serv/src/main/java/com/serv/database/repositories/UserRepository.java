package com.serv.database.repositories;

import com.serv.database.Email;
import com.serv.database.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @NonNull
    Optional<User> findById(@NonNull UUID id);
    Optional<User> findByUsername(@NonNull String username);
    Optional<User> findByEmail(@NonNull Email email);
}
