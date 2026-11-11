package com.example.revolution.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One shared per-player prequeue cooldown for ordinary and Crystal purchases.
 * Runs on the network handler BEFORE addScheduledTask, so rejected autoclick
 * traffic never enters the Minecraft server queue or changes wallet/nonce.
 * This intentionally does not affect daily, awaken, enhance, share or sync.
 */
public final class RunePurchaseRequestLimiter {
    public static final long COOLDOWN_MILLIS = 200L;
    private static final long COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(COOLDOWN_MILLIS);
    private static final ConcurrentMap<UUID, Long> lastAcceptedAt = new ConcurrentHashMap<>();

    private RunePurchaseRequestLimiter() {}

    public static boolean tryAcquire(UUID playerId) {
        return tryAcquire(playerId, System.nanoTime());
    }

    static boolean tryAcquire(UUID playerId, long nowNanos) {
        if (playerId == null) return false;
        AtomicBoolean allowed = new AtomicBoolean();
        lastAcceptedAt.compute(playerId, (id, previous) -> {
            if (previous == null || nowNanos - previous >= COOLDOWN_NANOS) {
                allowed.set(true);
                return nowNanos;
            }
            return previous;
        });
        return allowed.get();
    }

    public static void reset() { lastAcceptedAt.clear(); }
}
