package com.example.revolution.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded replay and per-second click guard for God Worship client intents. */
public final class GodWorshipRequestLimiter {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();
    private GodWorshipRequestLimiter() {}

    public static boolean acceptMutation(UUID playerUuid, long requestNonce) {
        if (playerUuid == null || requestNonce <= 0L) return false;
        State state = STATES.computeIfAbsent(playerUuid, ignored -> new State());
        synchronized (state) {
            if (requestNonce <= state.lastNonce) return false;
            state.lastNonce = requestNonce;
            return true;
        }
    }

    public static boolean acceptClick(UUID playerUuid, long nowMillis, int maxPerSecond) {
        if (playerUuid == null || maxPerSecond <= 0) return false;
        State state = STATES.computeIfAbsent(playerUuid, ignored -> new State());
        synchronized (state) {
            if (nowMillis < state.clickWindowStart || nowMillis - state.clickWindowStart >= 1000L) {
                state.clickWindowStart = nowMillis;
                state.clickCount = 0;
            }
            if (state.clickCount >= maxPerSecond) return false;
            state.clickCount++;
            return true;
        }
    }

    public static void forget(UUID playerUuid) { if (playerUuid != null) STATES.remove(playerUuid); }
    public static void reset() { STATES.clear(); }

    private static final class State {
        long lastNonce;
        long clickWindowStart;
        int clickCount;
    }
}
