package com.example.revolution.network;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One shared per-player prequeue cooldown for ordinary, Crystal and Offerings purchases.
 * Crystal and Offerings use the same protected purchase budget. Release QA measured 265–402ms
 * server-thread execution, exceeding the previous shared 200ms admission rate.
 * Runs on the network handler BEFORE addScheduledTask, so rejected autoclick
 * traffic never enters the Minecraft server queue or changes wallet/nonce.
 * This intentionally does not affect daily, awaken, enhance, share or sync.
 */
public final class RunePurchaseRequestLimiter {
    public static final long COOLDOWN_MILLIS = 200L;
    /** Release-2.12.2 safety backpressure: at most one accepted Crystal roll per second per player. */
    public static final long CRYSTAL_COOLDOWN_MILLIS = 1000L;
    private static final long COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(COOLDOWN_MILLIS);
    private static final long CRYSTAL_COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(CRYSTAL_COOLDOWN_MILLIS);
    private static final ConcurrentMap<UUID, Long> lastAcceptedAt = new ConcurrentHashMap<>();

    private RunePurchaseRequestLimiter() {}

    public static boolean tryAcquire(UUID playerId) {
        return tryAcquire(playerId, System.nanoTime());
    }

    /** Enforce a longer unique-rune interval without changing ordinary purchase pacing. */
    public static boolean tryAcquireOfferings(UUID playerId) {
        return tryAcquireCrystal(playerId);
    }

    static boolean tryAcquireOfferings(UUID playerId, long nowNanos) {
        return tryAcquireCrystal(playerId, nowNanos);
    }

    /** Shared unique-rune gate, covering both Crystal and Offerings, with one accepted timestamp. */
    public static boolean tryAcquireCrystal(UUID playerId) {
        return tryAcquireCrystal(playerId, System.nanoTime());
    }

    static boolean tryAcquireCrystal(UUID playerId, long nowNanos) {
        return acquire(playerId, nowNanos, CRYSTAL_COOLDOWN_NANOS);
    }

    static boolean tryAcquire(UUID playerId, long nowNanos) {
        return acquire(playerId, nowNanos, COOLDOWN_NANOS);
    }

    private static boolean acquire(UUID playerId, long nowNanos, long cooldownNanos) {
        if (playerId == null) return false;
        AtomicBoolean allowed = new AtomicBoolean();
        lastAcceptedAt.compute(playerId, (id, previous) -> {
            if (previous == null || nowNanos - previous >= cooldownNanos) {
                allowed.set(true);
                return nowNanos;
            }
            return previous;
        });
        return allowed.get();
    }

    public static void reset() { lastAcceptedAt.clear(); }
}
