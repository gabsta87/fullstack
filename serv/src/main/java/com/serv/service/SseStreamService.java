package com.serv.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseStreamService {
    // Permet de stocker PLUSIEURS connexions actives pour un même UUID utilisateur
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createStream(UUID userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .reconnectTime(5000L)
                    .data("Connection established"));
        } catch (IOException ignored) {}

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError((e) -> {
            emitter.completeWithError(e);
            removeEmitter(userId, emitter);
        });

        return emitter;
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * Méthode générique pour envoyer n'importe quel type d'événement à un utilisateur
     */
    public void emitEvent(UUID userId, String eventName, Object data) {
        // 🎯 On vérifie si le thread actuel exécute une transaction (@Transactional)
        if (TransactionSynchronizationManager.isActualTransactionActive()) {

            // On enregistre un hook de synchronisation Spring
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Cette méthode ne s'exécutera QUE si la BDD a validé à 100% l'écriture
                    executeEmission(userId, eventName, data);
                }
            });

        } else {
            // S'il n'y a pas de transaction (ex: GET /me ou calculs), on envoie immédiatement
            executeEmission(userId, eventName, data);
        }
    }

    /**
     * Logique interne d'envoi physique aux sockets (encapsulée)
     */
    private void executeEmission(UUID userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            List<SseEmitter> emittersCopy = new ArrayList<>(userEmitters);

            for (SseEmitter emitter : emittersCopy) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    System.out.println("Nettoyage d'un emitter défaillant (" + e.getClass().getSimpleName() + ") pour l'utilisateur " + userId);
                    try {
                        emitter.complete();
                    } catch (Exception ignored) {}
                    removeEmitter(userId, emitter);
                }
            }
        }
    }
}
