package com.serv.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
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

    public VenusUser(String username, Email email, String password) {
        this.username = username;
        this.email = email;
        this.passwordHash = new BCryptPasswordEncoder().encode(password);
    }

    public void setPassword(String password) {
        this.passwordHash = new BCryptPasswordEncoder().encode(password);
    }

    public boolean checkPassword(String password) {
        return new BCryptPasswordEncoder().matches(password, this.passwordHash);
    }
}
