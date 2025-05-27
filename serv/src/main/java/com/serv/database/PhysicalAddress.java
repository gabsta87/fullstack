package com.serv.database;

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
