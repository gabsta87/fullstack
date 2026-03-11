package com.serv.controller;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.ClientRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.repositories.UserRepository;
import com.serv.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private WorkerRepository sellerRepository;
    @Autowired
    private MailService mailService;

    @PostMapping(path="/addClient")
    public @ResponseBody ResponseEntity<String> addNewClient (@RequestBody Requests.RegisterRequest cl) {

        try {
            Client n = new Client(cl.getUsername(), new Email(cl.getEmail()), cl.getPassword());
            if (userRepository.findByUsername(n.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already taken.");
            }

            // TODO send mail to activate account
            String activationURL = "testString";
//            mailService.sendFormattedMessage(n.getEmail(), "Account activation",activationURL);

            n.setEnabled(false);
            n.setLocked(true);

            clientRepository.save(n);
            return ResponseEntity.ok("User registered successfully!");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path="/addWorker") // Map ONLY POST Requests
    public @ResponseBody ResponseEntity<String> addNewWorker (@RequestBody Requests.RegisterRequest wo) {
        try {
            Worker u = new Worker(wo.getUsername(), new Email(wo.getEmail()),wo.getPassword());
            if (userRepository.findByUsername(u.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already taken.");
            }

            // TODO send mail to activate account
            String activationURL = "testString";
//            mailService.sendFormattedMessage(u.getEmail(), "Account activation",activationURL);

            u.setEnabled(false);
            u.setLocked(true);
            u.setExpired(true);

            sellerRepository.save(u);
            return ResponseEntity.ok("User registered successfully!");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(path="/getSellers")
    public @ResponseBody Iterable<Worker> getAllWorkers() {
        return sellerRepository.findAll();
    }
}

