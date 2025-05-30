package com.serv.database;

import com.serv.common.Currency;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "number")
    private String number;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "bill_value")
    private int billValue;

    @Enumerated(EnumType.STRING)
    @Column(name="currency")
    private Currency currency;

//    @Column(name = "card_number")
    @Transient
    private String cardNumber;

    @Column(name = "card_holder")
    private String cardHolder;

    @Column(name = "date_value")
    private String dateValue;

    @Column(name = "token")
    private String token;

    //    @Column(name = "cvc")
    @Transient
    private String cvc;

}