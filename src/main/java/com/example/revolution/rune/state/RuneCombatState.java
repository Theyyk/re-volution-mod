package com.example.revolution.rune.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Temporary rage belongs to one running server; expired entries are removed on observation. */
public final class RuneCombatState {
    private final Map<UUID, Long> rageExpiryTicks = new ConcurrentHashMap<>();
    public void activate(UUID player, long expiresAt) { rageExpiryTicks.put(player, expiresAt); }
    public boolean isActive(UUID player, long worldTick) {
        Long expiry = rageExpiryTicks.get(player);
        if (expiry == null) return false;
        if (worldTick < expiry) return true;
        rageExpiryTicks.remove(player, expiry);
        return false;
    }
    public void clear() { rageExpiryTicks.clear(); }
}
