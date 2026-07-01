package com.serv.controller;

import com.serv.common.PaymentType;
import com.serv.database.entities.Payment;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PaymentRepository;

import com.serv.database.repositories.WorkerRepository;
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

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WorkerRepository workerRepository; // Pour appliquer les changements de statut/boost

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret; // 🎯 Clé whsec_ générée par Stripe CLI ou ton dashboard

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        return ResponseEntity.ok(Map.of("publicKey", stripePublicKey));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Object> requestData) {
        try {
            Long workerId = Long.valueOf(requestData.get("workerId").toString());
            int amount = Integer.parseInt(requestData.get("amount").toString());
            String currency = requestData.get("currency").toString();
            PaymentType type = PaymentType.valueOf(requestData.get("paymentType").toString());

            // 1. Configuration de l'intention chez Stripe avec Métadonnées sécurisées
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) amount)
                    .setCurrency(currency.toLowerCase())
                    .putMetadata("workerId", workerId.toString())
                    .putMetadata("paymentType", type.toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // 2. Enregistrement d'une trace locale en attente (PENDING)
            Payment payment = new Payment();
            payment.setWorkerId(workerId);
            payment.setBillValue(amount);
            payment.setCurrency(currency);
            payment.setPaymentType(type);
            payment.setStripePaymentIntentId(intent.getId());
            payment.setStatus("PENDING");
            paymentRepository.save(payment);

            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                fulfillOrder(paymentIntent);
            }
        }
        return ResponseEntity.ok("OK");
    }

    // 🎯 C'est ici qu'on applique tes deux règles métier !
    private void fulfillOrder(PaymentIntent intent) {

        // 1. Recherche du paiement local pour le passer à SUCCEEDED
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(intent.getId());
        Payment payment = paymentOpt.orElse(null);

        if (payment != null) {
            payment.setStatus("SUCCEEDED");
            paymentRepository.save(payment);
        }

        // 2. Extraction des métadonnées
        UUID workerId;
        try{
            workerId = UUID.fromString(intent.getMetadata().get("workerId"));
        }catch(IllegalArgumentException e){
            System.err.println("Invalid workerId in PaymentIntent metadata: " + intent.getMetadata().get("workerId"));
            return;
        }

        PaymentType type = PaymentType.valueOf(intent.getMetadata().get("paymentType"));

        Worker worker = workerRepository.findById(workerId).orElse(null);
        if (worker == null) return;

        if (type == PaymentType.BOOST) {
            // 🚀 RÈGLE 2 : Boost en haut de la liste (galleryPositionIndex au maximum actuel + 1)
            Integer currentMaxIndex = workerRepository.findMaxGalleryPositionIndex();
            int newMax = (currentMaxIndex != null) ? currentMaxIndex + 1 : 1;
            worker.setGalleryPositionIndex(newMax);
        } else if (type == PaymentType.DAYS) {
            // 📅 RÈGLE 1 : Ajout de jours au crédit (ex: calculé selon le montant payé)
            int daysPurchased = intent.getAmount().intValue() / 500; // exemple: 5€ par jour
            worker.setRemainingDaysCredit(worker.getRemainingDaysCredit() + daysPurchased);
        }

        workerRepository.save(worker);
    }
}