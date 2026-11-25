package com.serv.database.repositories;
import com.serv.database.entities.VenusUser;
import com.serv.database.entities.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUser(VenusUser user);

    // Removes a user's previous tokens when they register again
    void deleteByUser(VenusUser user);

    void deleteAllByUserId(UUID id);

    void deleteAllByExpiryDateBefore(Instant now);
}
