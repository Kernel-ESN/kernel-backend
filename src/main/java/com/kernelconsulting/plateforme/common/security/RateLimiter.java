package com.kernelconsulting.plateforme.common.security;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fenêtre fixe d'une minute, en mémoire : suffisant pour un seul serveur. Avec plusieurs instances
 * ou derrière un proxy, il faudra un compteur partagé et l'adresse réelle du client.
 */
@Component
public class RateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final int limit;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimiter(@Value("${app.rate-limit.auth-per-minute}") int limit) {
        this(limit, Clock.systemUTC());
    }

    RateLimiter(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    /** @return vrai si cette clé peut encore faire un essai dans la minute en cours */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= WINDOW_MS);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.start() >= WINDOW_MS
                        ? new Window(now, 1)
                        : new Window(current.start(), current.count() + 1));
        return window.count() <= limit;
    }

    private record Window(long start, int count) {
    }
}
