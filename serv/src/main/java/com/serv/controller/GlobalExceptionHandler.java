package com.serv.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String errorMessage = "This resource already exists.";

        // Optionnel : possibilité d'inspecter ex.getMessage() pour être plus précis sur la clé du doublon

        return ResponseEntity
                .status(HttpStatus.CONFLICT) // Code HTTP 409: Conflit de ressources
                .body(Map.of("error", errorMessage));
    }
}