package com.serv.database.entities;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class PhysicalAddress {
    private String street;
    private String city;
    private String state;
    private String zip;

}
