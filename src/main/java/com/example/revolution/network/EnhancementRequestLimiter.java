package com.example.revolution.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-player cooldown for enhancement requests before they reach the server thread queue.
 */
final class EnhancementRequestLimiter {
    static final long COOLDOWN_MILLIS = 100L;
    private static final long COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(COOLDOWN_MILLIS);

    private final ConcurrentMap<UUID, Long> lastAcceptedAt = new ConcurrentHashMap<>();

    boolean tryAcquire(UUID playerId) {
        return tryAcquire(playerId, System.nanoTime());
    }

    boolean tryAcquire(UUID playerId, long nowNanos) {
        if (playerId == null) return false;

        AtomicBoolean accepted = new AtomicBoolean(false);
        lastAcceptedAt.compute(playerId, (id, previous) -> {
            if (previous == null || nowNanos - previous >= COOLDOWN_NANOS) {
                accepted.set(true);
                return nowNanos;
            }
            return previous;
        });
        return accepted.get();
    }
}
