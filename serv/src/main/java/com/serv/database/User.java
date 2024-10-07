package com.serv.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class User {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    protected Long id;
    protected String pseudo;
    @Embedded
    protected Email email;
    @Basic(optional = true)
    @Embedded
    protected PhoneNumber phone;
    protected String passwordHash;

}
