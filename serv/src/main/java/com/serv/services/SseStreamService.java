package com.serv.services;

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
    // Several active connections per user UUID (one per tab/device)
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** A user (or admin) rarely has more tabs open; older connections are closed beyond this. */
    static final int MAX_STREAMS_PER_USER = 5;

    /** Idle streams are closed after 30 minutes; the browser's EventSource reconnects by itself. */
    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    public SseEmitter createStream(UUID userId) {
        return createStream(userId, new SseEmitter(STREAM_TIMEOUT_MS));
    }

    /** Package-private: lets tests observe what is sent. */
    SseEmitter createStream(UUID userId, SseEmitter emitter) {
        List<SseEmitter> existing = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        while (existing.size() >= MAX_STREAMS_PER_USER) {
            SseEmitter oldest = existing.remove(0);
            try { oldest.complete(); } catch (Exception ignored) {}
        }
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
        emitters.computeIfPresent(userId, (key, userEmitters) -> {
            userEmitters.remove(emitter);
            return userEmitters.isEmpty() ? null : userEmitters;
        });
    }

    /**
     * Runs the action once the current transaction has committed (right away outside a transaction).
     * Receivers reload data when notified: they must never read the state from before the change.
     */
    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**
     * Sends any kind of event to a user, after the commit of the current transaction.
     */
    public void emitEvent(UUID userId, String eventName, Object data) {
        afterCommit(() -> executeEmission(userId, eventName, data));
    }

    /**
     * Tells the user's open pages that the session is over (ban, deletion), then closes their streams.
     */
    public void endSession(UUID userId) {
        afterCommit(() -> {
            executeEmission(userId, "session-expired", "");
            List<SseEmitter> userEmitters = emitters.remove(userId);
            if (userEmitters != null) {
                for (SseEmitter emitter : userEmitters) {
                    try { emitter.complete(); } catch (Exception ignored) {}
                }
            }
        });
    }

    public void broadcastEvent(String eventName, Object data) {
        emitters.forEach((userId, userEmitters) -> {
            List<SseEmitter> emittersCopy = new ArrayList<>(userEmitters);
            for (SseEmitter emitter : emittersCopy) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    try {
                        emitter.complete();
                    } catch (Exception ignored) {}
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    /**
     * Writes the event to the user's open connections.
     */
    private void executeEmission(UUID userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            List<SseEmitter> emittersCopy = new ArrayList<>(userEmitters);

            for (SseEmitter emitter : emittersCopy) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    try {
                        emitter.complete();
                    } catch (Exception ignored) {}
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    // ADMINS

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<SseEmitter>> adminEmittersById = new ConcurrentHashMap<>();

    public SseEmitter createAdminStream(UUID adminId) {
        return createAdminStream(adminId, new SseEmitter(STREAM_TIMEOUT_MS));
    }

    SseEmitter createAdminStream(UUID adminId, SseEmitter emitter) {
        List<SseEmitter> own = adminEmittersById.computeIfAbsent(adminId, k -> new CopyOnWriteArrayList<>());
        while (own.size() >= MAX_STREAMS_PER_USER) {
            SseEmitter oldest = own.remove(0);
            adminEmitters.remove(oldest);
            try { oldest.complete(); } catch (Exception ignored) {}
        }
        own.add(emitter);
        adminEmitters.add(emitter);

        Runnable cleanup = () -> {
            adminEmitters.remove(emitter);
            own.remove(emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { emitter.complete(); cleanup.run(); });
        emitter.onError((e) -> { emitter.completeWithError(e); cleanup.run(); });

        return emitter;
    }

    int activeStreamCount(UUID userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list == null ? 0 : list.size();
    }

    /** Notifies every connected admin, after the commit of the current transaction. */
    public void broadcastAdminEvent(String eventName, Object data) {
        afterCommit(() -> sendToAdmins(eventName, data));
    }

    private void sendToAdmins(String eventName, Object data) {
        List<SseEmitter> emittersCopy = new ArrayList<>(adminEmitters);
        for (SseEmitter emitter : emittersCopy) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                try { emitter.complete(); } catch (Exception ignored) {}
                adminEmitters.remove(emitter);
            }
        }
    }

}
