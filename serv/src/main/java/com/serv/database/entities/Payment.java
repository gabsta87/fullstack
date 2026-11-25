package com.serv.database.entities;

import com.serv.common.Currency;
import com.serv.common.PaymentType;
import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = TablesNames.PAYMENTS)
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Account id of the paying advertiser
    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Column(name = "bill_value")
    private int billValue; // In cents (e.g. 2500 for 25.00)

    @Column(name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "status")
    private String status; // PENDING, SUCCEEDED, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}