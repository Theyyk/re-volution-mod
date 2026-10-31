package com.example.revolution.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Monotonic nonce guard for Aura mutations. Sync requests do not consume a nonce. */
public final class AuraRequestLimiter {
    private static final Map<UUID, Long> LAST_NONCE = new ConcurrentHashMap<>();
    private AuraRequestLimiter() {}

    public static boolean acceptMutation(UUID playerUuid, long requestNonce) {
        if (playerUuid == null || requestNonce <= 0L) return false;
        synchronized (LAST_NONCE) {
            Long previous = LAST_NONCE.get(playerUuid);
            if (previous != null && requestNonce <= previous) return false;
            LAST_NONCE.put(playerUuid, requestNonce);
            return true;
        }
    }

    public static void forget(UUID playerUuid) { if (playerUuid != null) LAST_NONCE.remove(playerUuid); }
    public static void reset() { LAST_NONCE.clear(); }
}
