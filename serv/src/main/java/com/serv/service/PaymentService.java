package com.serv.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

import com.serv.common.Currency;

public class PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentService() {
        Stripe.apiKey = stripeSecretKey;
    }

    public Charge chargeCustomer(String token, int amount, Currency currency) throws StripeException {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", amount); // Amount in cents
        chargeParams.put("currency", currency.toString());
        chargeParams.put("source", token); // Token returned from frontend

        return Charge.create(chargeParams);
    }
}