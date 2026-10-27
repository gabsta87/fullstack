package com.serv.database.entities;

import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("CLIENT")
@Table(name = TablesNames.CLIENTS)
public class Client extends VenusUser {

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "client_favorites",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id")
    )
    private Collection<Worker> favorites;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_preferred_languages", joinColumns = @JoinColumn(name = "client_id"))
    @Enumerated(EnumType.STRING)
    private Collection<Language> preferredLanguages;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "client_preferred_services", // Nom de ta table de jointure
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

    public Client(Email email, String password) {
        super(email, password, null);
    }

    public UserRole getRole(){
        return UserRole.CLIENT;
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
        comment.setAuthor(null);
    }
}