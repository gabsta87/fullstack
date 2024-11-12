package com.serv.controller;

import com.serv.database.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;

    @Autowired
    public UserController(ClientRepository clientRepository, WorkerRepository workerRepository) {
        this.clientRepository = clientRepository;
        this.workerRepository = workerRepository;
    }

    @PostConstruct
    public void init() {
        System.out.println("MyController initialized");
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @GetMapping("/")
    public String index() {
        return "this is the index";
    }

    @PostMapping(path="/addClient") // Map ONLY POST Requests
    public @ResponseBody ResponseEntity<String> addNewClient (@RequestBody ClientDTO cl) {

        try {
            Client n = new Client();
            n.setPseudo(cl.getPseudo());
            n.setEmail(new Email(cl.getEmail()));
            clientRepository.save(n);

            return ResponseEntity.accepted().body("Client added successfully");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(path="/addWorker") // Map ONLY POST Requests
    public @ResponseBody ResponseEntity<String> addNewWorker (@RequestBody WorkerDTO wo) {

        try {
            Worker n = new Worker();
            n.setPseudo(wo.getPseudo());
            n.setEmail(new Email(wo.getEmail()));
            workerRepository.save(n);

            return ResponseEntity.ok().body("Worker added successfully");
        }catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Worker> getAllWorkers() {
        // This returns a JSON or XML with the users
        return workerRepository.findAll();
    }

    @Getter
    static class ClientDTO{
        private String pseudo;
        private String email;
    }

    @Getter
    static class WorkerDTO{
        private String pseudo;
        private String email;
    }
}

