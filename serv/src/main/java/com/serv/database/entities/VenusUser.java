package com.serv.database.entities;

import com.serv.common.TablesNames;
import com.serv.common.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Table(name = TablesNames.USERS, indexes = {
        @Index(name = "idx_user_zone", columnList = "geographic_zone_id")
})
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class VenusUser {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @EqualsAndHashCode.Include
    protected UUID id;
    protected String username;
    @Embedded
    @Basic(optional = false)
    protected Email email;
    @Basic(optional = false)
    @Column(nullable = false)
    protected String passwordHash;

    // by ADMINS
    // Banned account cannot log in or be seen by visitors
    @Column(nullable = false)
    protected boolean isBanned = false;

    // Locked account cannot perform certain actions or be seen by visitors
    @Column(nullable = false)
    protected boolean isLocked = false;

    /**
     * Revocation counter embedded in every JWT. Incrementing it invalidates all the user's
     * existing tokens (see {@link #revokeTokens()}).
     */
    @Column(name = "token_version", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    protected int tokenVersion = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geographic_zone_id")
    @ToString.Exclude
    private GeographicZone geographicZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "user_type", insertable = false, updatable = false)
    private String userType;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public VenusUser(Email email, String password, String username) {
        this.email = email;
        this.username = username;
        this.passwordHash = ENCODER.encode(password);
    }

    /** Hashes and stores a new password, and revokes every token issued with the old one. */
    public void setPassword(String password) {
        this.passwordHash = ENCODER.encode(password);
        revokeTokens();
    }

    public boolean checkPassword(String password) {
        return password != null && this.passwordHash != null && ENCODER.matches(password, this.passwordHash);
    }

    /** Invalidates every JWT previously issued to this user. */
    public void revokeTokens() {
        this.tokenVersion++;
    }

    /** Banning a user also kills all of their sessions. */
    public void setBanned(boolean banned) {
        if (banned && !this.isBanned) {
            revokeTokens();
        }
        this.isBanned = banned;
    }
    public boolean isAdmin(){ return false; };
    public boolean isSuperAdmin() { return false; }

    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Rôle de base par exemple
        return authorities;
    }

    public abstract UserRole getRole();
}
