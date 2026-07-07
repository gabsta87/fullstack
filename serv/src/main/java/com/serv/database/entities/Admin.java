package com.serv.database.entities;

import com.serv.common.UserRole;
import jakarta.persistence.Entity;

@Entity
public class Admin extends VenusUser{

    public Admin(String username, Email email, String password) {
        super(email,password);
        this.username = username;
        this.role = UserRole.ADMIN;
    }

    public Admin() {
        this.role = UserRole.ADMIN;
    }

    // TODO in the future, pay with stripe
//    private String IBAN;
}
