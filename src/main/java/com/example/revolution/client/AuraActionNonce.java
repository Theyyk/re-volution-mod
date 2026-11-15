package com.example.revolution.client;

/**
 * Shared client nonce source for every Aura mutation screen.
 * AuraRequestLimiter tracks a single monotonic sequence per player, so
 * independently seeded GUI counters can invalidate otherwise valid actions.
 */
public final class AuraActionNonce {
    private static long lastNonce = Math.max(1L, System.currentTimeMillis());

    private AuraActionNonce() {}

    public static synchronized long next() {
        long now = Math.max(1L, System.currentTimeMillis());
        if (lastNonce < now) lastNonce = now;
        return ++lastNonce;
    }
}
