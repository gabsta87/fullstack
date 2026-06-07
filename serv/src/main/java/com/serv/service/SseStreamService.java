package com.serv.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseStreamService {
    // Stocke les connexions actives par ID de User
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createStream(UUID userId) {
        // Timeout de 30 minutes (ajustable)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(userId, emitter);

        // Nettoyage en cas de déconnexion
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        return emitter;
    }

    public void emitAccountUpdate(UUID userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("account-update").data(data));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
