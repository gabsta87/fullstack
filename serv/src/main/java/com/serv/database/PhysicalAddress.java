package com.serv.database;

import jakarta.persistence.Embeddable;

@Embeddable
public class PhysicalAddress {
    private String street;
    private String city;
    private String state;
    private String zip;

}
