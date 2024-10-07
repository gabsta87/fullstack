package com.serv.database;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@Entity
public class Client extends User{

    @OneToMany
    Collection<Worker> favorites;

    public Client() {}
}
