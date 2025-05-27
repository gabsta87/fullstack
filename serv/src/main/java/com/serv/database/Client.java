package com.serv.database;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("CLIENT")
@Table(name = "clients")
public class Client extends VenusUser {

    @OneToMany
    Collection<Seller> favorites;

    // TODO default location

    public Client(String name, Email email, String password) {
        super(name,email,password);
    }
}
