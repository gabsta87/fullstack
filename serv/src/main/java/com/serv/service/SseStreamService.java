package com.serv.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseStreamService {
    // Stocke les connexions actives par ID de Worker
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createStream(UUID workerId) {
        // Timeout de 30 minutes (ajustable)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        emitters.put(workerId, emitter);

        // Nettoyage en cas de déconnexion
        emitter.onCompletion(() -> emitters.remove(workerId));
        emitter.onTimeout(() -> emitters.remove(workerId));
        emitter.onError((e) -> emitters.remove(workerId));

        return emitter;
    }

    public void emitAccountUpdate(UUID workerId, Object data) {
        SseEmitter emitter = emitters.get(workerId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("account-update").data(data));
            } catch (IOException e) {
                emitters.remove(workerId);
            }
        }
    }
}
