package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "users") // avoiding SQL keywords
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class VenusUser {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    protected UUID id;
    @Basic(optional = false)
    @Column(nullable = false)
    protected String username;
    @Embedded
    @Basic(optional = false)
    protected Email email;
    @Embedded
    protected PhoneNumber phone;
    @Basic(optional = false)
    @Column(nullable = false)
    protected String passwordHash;
    protected boolean enabled;
    protected boolean locked;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public VenusUser(String username, Email email, String password) {
        this.username = username;
        this.email = email;
        this.passwordHash = ENCODER.encode(password);
    }

    public void setPassword(String password)        { this.passwordHash = ENCODER.encode(password); }
    public boolean checkPassword(String password)   { return ENCODER.matches(password, this.passwordHash); }

}
