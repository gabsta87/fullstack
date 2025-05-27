package com.serv.controller;

import com.serv.database.Payment;
import com.serv.database.repositories.PaymentRepository;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.stripe.exception.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @Value("${stripe.secret.key}")
    private String endpointSecret;

    @Value("${stripe.public.key}")
    private String endpointPublic;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostConstruct
    public void initializeStripeApiKey() {
        Stripe.apiKey = endpointSecret;
    }

    @GetMapping("/config")
    public ResponseEntity<String> config() {
        return ResponseEntity.ok(endpointPublic);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String request,
            @RequestHeader HttpHeaders headers) {
        System.out.println("webhook : payload: " + request);
        System.out.println("webhook : headers: " + headers);

//        String payload = request.body();
//        String sigHeader = request.headers("Stripe-Signature");

        System.out.println("Sig header : "+headers.get("Stripe-Signature").getFirst());

        Event event = null;

        try {
            event = Webhook.constructEvent(request, headers.get("Stripe-Signature").getFirst(), endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Could not verify signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                // Fulfill any orders, e-mail receipts, etc
                // To cancel the payment you will need to issue a Refund
                // (https://stripe.com/docs/api/refunds)
                System.out.println("💰Payment received!");
                break;
            case "payment_intent.payment_failed":
                System.out.println("❌ Payment failed.");
                break;
            default:
                // Unexpected event type
                return ResponseEntity.status(400).body("Unexpected event type: " + event.getType());

        }

        return ResponseEntity.ok().body("OK");
    }

    @PostMapping(path="/create-payment-intent")
    public @ResponseBody ResponseEntity<Map<String, String>> createPayment (@RequestBody Payment payment) {
        System.out.println("Create Payment Intent : " + payment);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .setCurrency(payment.getCurrency().toString())
                .setAmount((long) payment.getBillValue())
                .build();

        try {
            System.out.println("Stripe api key : " + Stripe.apiKey);
            PaymentIntent intent = PaymentIntent.create(params);

            paymentRepository.save(payment);

            // Wrapping clientSecret in a map to send as JSON response
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", intent.getClientSecret());

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", "Error while creating payment intent: " + e.getMessage()));
        }
    }
}