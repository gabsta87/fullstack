package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class PasswordResetToken {

    public PasswordResetToken(UUID token, VenusUser user) {
        this.user = user;
        this.id = token;
        expiryDate = Instant.now().plusSeconds(3600*24);
    }

    @Id
    private UUID id;

    @OneToOne(targetEntity = VenusUser.class)
    @JoinColumn(nullable = false)
    private VenusUser user;

    private Instant expiryDate;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}