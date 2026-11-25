package com.serv.services;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory sliding-window limiter: at most {@code maxEvents} per key within {@code window}.
 * State is per server instance and is lost on restart, which is acceptable for abuse throttling.
 */
public class RateLimiter {

    private final int maxEvents;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Long>> events = new ConcurrentHashMap<>();

    public RateLimiter(int maxEvents, Duration window, Clock clock) {
        this.maxEvents = maxEvents;
        this.window = window;
        this.clock = clock;
    }

    /** @return true if another event is still allowed for this key (does not record anything) */
    public boolean isAllowed(String key) {
        Deque<Long> deque = events.get(key);
        if (deque == null) return true;
        synchronized (deque) {
            prune(deque);
            return deque.size() < maxEvents;
        }
    }

    public void record(String key) {
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque);
            deque.addLast(clock.millis());
        }
    }

    /** Records the event if allowed. @return false when the limit is reached */
    public boolean tryAcquire(String key) {
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque);
            if (deque.size() >= maxEvents) return false;
            deque.addLast(clock.millis());
            return true;
        }
    }

    public void reset(String key) {
        events.remove(key);
    }

    public void clear() {
        events.clear();
    }

    /** Drops keys whose events are all outside the window, to bound memory usage. */
    public void evictExpired() {
        events.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                prune(deque);
                return deque.isEmpty();
            }
        });
    }

    private void prune(Deque<Long> deque) {
        long threshold = clock.millis() - window.toMillis();
        while (!deque.isEmpty() && deque.peekFirst() <= threshold) {
            deque.pollFirst();
        }
    }
}
