package com.serv.database.entities;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
public class Admin extends VenusUser{

    public Admin(String username, Email email, String password) {
        super(email,password);
        this.username = username;
    }

    public boolean isAdmin(){
        return true;
    }
}
