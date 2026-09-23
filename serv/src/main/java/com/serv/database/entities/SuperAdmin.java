package com.serv.database.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.NoArgsConstructor;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
public class SuperAdmin extends Admin{
    public SuperAdmin(String username, Email email, String password) {
        super(username, email, password);
    }

    public boolean isSuperAdmin() {return true;}
}
