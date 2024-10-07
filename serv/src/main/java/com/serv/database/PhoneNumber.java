package com.serv.database;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class PhoneNumber {
    private String countryCode;
    private String number;
}
