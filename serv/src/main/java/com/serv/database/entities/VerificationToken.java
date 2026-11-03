package com.serv.database.entities;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "verification_tokens")
public class VerificationToken {

    private static final int EXPIRATION_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = VenusUser.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private VenusUser user;

    @Column(nullable = false)
    private Instant expiryDate;

    public VerificationToken(VenusUser user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = Instant.now().plusSeconds(EXPIRATION_HOURS * 3600);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}