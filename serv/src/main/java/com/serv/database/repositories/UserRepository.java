package com.serv.database.repositories;

import com.serv.database.entities.VenusUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<VenusUser, UUID> {
    @NonNull
    Optional<VenusUser> findById(@NonNull UUID id);

    @Query("SELECT u FROM VenusUser u WHERE u.email.value = :email")
    Optional<VenusUser> findByNormalizedEmail(@Param("email") String email);

    /** Case-insensitive lookup: the email is normalized the same way it is stored. */
    default Optional<VenusUser> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return findByNormalizedEmail(com.serv.database.entities.Email.normalize(email));
    }

    @NonNull
    List<VenusUser> findAll();

    /** Another account already shows this name (case ignored): a visitor may not take it. */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);
}
