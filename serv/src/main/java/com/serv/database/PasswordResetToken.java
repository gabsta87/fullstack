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

    public PasswordResetToken(UUID token,User user) {
        this.user = user;
        this.id = token;
        expiryDate = LocalDateTime.now().plusDays(1);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(targetEntity = User.class)
    @JoinColumn(nullable = false)
    private User user;

    private LocalDateTime expiryDate;

}