package com.serv.database;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
@Embeddable
public class Email {
    @Transient
    String pattern = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";

    public Email() {}

    public Email(String email) throws IllegalArgumentException {
        if(!email.matches(pattern))
            throw new IllegalArgumentException("Invalid email format");
        else{
            String[] parts = email.split("@");
            userName = parts[0];
            String[] parts2 = (parts[1]).split("\\.");
            mailServer = String.join(".",Arrays.copyOfRange(parts2,0,parts2.length-1));
            domain = parts2[parts2.length - 1];
        }
    }

    private String userName;
    private String mailServer;
    private String domain;

    @Override
    public String toString(){
        return userName + "@" + mailServer + "." + domain;
    }

}
