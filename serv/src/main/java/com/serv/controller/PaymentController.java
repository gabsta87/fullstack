package com.serv.controller;

import com.serv.database.Client;
import com.serv.database.Email;
import com.serv.database.Payment;
import com.serv.database.PaymentRepository;
import com.serv.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    @Autowired
    PaymentRepository paymentRepository;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        // Process the webhook data from the payment gateway
        return ResponseEntity.ok("Webhook received");
    }

    @PostMapping(path="/create-payment-intent") // Map ONLY POST Requests
    public @ResponseBody Response createPayment (@RequestBody Payment payment) {

        paymentRepository.save(payment);

        return new Response(Response.Status.ACCEPTED, "Payment successfully created in DB");
    }
}