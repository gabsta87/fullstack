package com.serv.database.entities;

import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Collection;

@Data
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

    public Client(Email email, String password) {
        super(email, password);
        this.role = UserRole.CLIENT;
    }

    public Client(){
        this.role = UserRole.CLIENT;
    }
}