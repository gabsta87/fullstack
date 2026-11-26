package com.serv.database.entities;

import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("CLIENT")
@Table(name = TableNames.CLIENTS)
public class Client extends VenusUser {

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = TableNames.CLIENT_FAVORITES,
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id")
    )
    private Collection<Worker> favorites;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = TableNames.CLIENT_PREFERRED_LANGUAGES, joinColumns = @JoinColumn(name = "client_id"))
    @Enumerated(EnumType.STRING)
    private Collection<Language> preferredLanguages;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = TableNames.CLIENT_PREFERRED_SERVICES,
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Collection<Service> preferredServices;

    @Enumerated(EnumType.STRING)
    private BodyType preferredBodyType;

    @Enumerated(EnumType.STRING)
    private HairColor preferredHairColor;

    @Enumerated(EnumType.STRING)
    private EyeColor preferredEyeColor;

    @Enumerated(EnumType.STRING)
    private Gender preferredGender;

    private Integer preferredMinAge;
    private Integer preferredMaxAge;

    @OneToMany(mappedBy = "author", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Comment> comments = new ArrayList<>();

    private static final SecureRandom RANDOM = new SecureRandom();

    public Client(Email email, String password) {
        super(email, password, defaultPseudonym());
    }

    /**
     * Pseudonym given to a visitor who has not chosen a name ("visitor-0427"). Never derived from the email,
     * which often contains a real name.
     */
    public static String defaultPseudonym() {
        return "visitor-%04d".formatted(RANDOM.nextInt(10_000));
    }

    /** Gives a pseudonym to an account without a name (accounts created before pseudonyms existed). */
    public void ensureUsername() {
        if (username == null || username.isBlank()) {
            username = defaultPseudonym();
        }
    }

    public UserRole getRole(){
        return UserRole.CLIENT;
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
        comment.setAuthor(null);
    }
}