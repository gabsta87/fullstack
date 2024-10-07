package com.serv.controller;

import com.serv.database.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class PageController {
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    WorkerRepository workerRepository;

    @PostConstruct
    public void init() {
        System.out.println("MyController initialized");
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @PostMapping(path="/addClient") // Map ONLY POST Requests
    public @ResponseBody Response addNewClient (@RequestBody ClientDTO cl) {

        try {
            Client n = new Client();
            n.setPseudo(cl.getPseudo());
            n.setEmail(new Email(cl.getEmail()));
            clientRepository.save(n);

            return new Response(Response.Status.ACCEPTED, "Client added successfully");
        }catch (IllegalArgumentException e) {
            return new Response(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping(path="/addWorker") // Map ONLY POST Requests
    public @ResponseBody Response addNewWorker (@RequestBody WorkerDTO wo) {

        try {
            Worker n = new Worker();
            n.setPseudo(wo.getPseudo());
            n.setEmail(new Email(wo.getEmail()));
            workerRepository.save(n);

            return new Response(Response.Status.ACCEPTED, "Worker added successfully");
        }catch (IllegalArgumentException e) {
            return new Response(Response.Status.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Worker> getAllWorkers() {
        // This returns a JSON or XML with the users
        return workerRepository.findAll();
    }
}

@Getter
class ClientDTO{
    private String pseudo;
    private String email;
}

@Getter
class WorkerDTO{
    private String pseudo;
    private String email;
}