package com.serv.database;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class PasswordResetToken {

    public PasswordResetToken(UUID token, VenusUser user) {
        this.user = user;
        this.id = token;
        expiryDate = LocalDateTime.now().plusDays(1);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(targetEntity = VenusUser.class)
    @JoinColumn(nullable = false)
    private VenusUser user;

    private LocalDateTime expiryDate;

}