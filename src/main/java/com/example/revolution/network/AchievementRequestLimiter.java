package com.example.revolution.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Monotonic replay guard for Achievement Claim/Share actions. Sync remains read-only and nonce-free. */
public final class AchievementRequestLimiter {
    private static final Map<UUID, Long> LAST_NONCE = new ConcurrentHashMap<>();
    private AchievementRequestLimiter() {}

    public static boolean acceptAction(UUID playerUuid, long requestNonce) {
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
