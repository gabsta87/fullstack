package com.serv.service;

import org.springframework.stereotype.Service;
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
        // Timeout de 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 🎯 BONUS RECONNEXION : On envoie un premier événement "init" vide
        // qui configure le délai de reconnexion automatique du navigateur à 5 secondes (5000ms)
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .reconnectTime(5000L)
                    .data("Connection established"));
        } catch (IOException e) {
            // Si le client s'est déjà déconnecté, on ne fait rien
        }

        // Ajoute l'émetteur à la liste de l'utilisateur
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Nettoyage lors de la déconnexion
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
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {

            // 💡 Astuce de sécurité : on travaille sur une copie ou un itérateur
            // pour éviter une ConcurrentModificationException si removeEmitter modifie la liste en plein milieu du for.
            List<SseEmitter> emittersCopy = new ArrayList<>(userEmitters);

            for (SseEmitter emitter : emittersCopy) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) { // 🎯 CHANGEMENT ICI : On attrape TOUTES les exceptions (IOException, IllegalStateException, etc.)
                    System.out.println("Nettoyage d'un emitter défaillant (" + e.getClass().getSimpleName() + ") pour l'utilisateur " + userId);

                    try {
                        emitter.complete();
                    } catch (Exception ignored) {
                        // 🛡️ Si Tomcat est déjà à l'agonie sur cet émetteur,
                        // complete() peut lui-même lever une erreur. On l'ignore sagement.
                    }

                    removeEmitter(userId, emitter);
                }
            }
        }
    }
}
