package com.example.revolution.rune.state;

import com.example.revolution.rune.definition.RuneDamageChannel;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Temporary rune effects belong to one running server and are never persisted. */
public final class RuneCombatState {
    private final Map<UUID, Long> allDamageExpiryTicks = new ConcurrentHashMap<>();
    private final Map<UUID, EnumMap<RuneDamageChannel, Long>> channelExpiryTicks = new ConcurrentHashMap<>();

    /** Legacy C1 rage API: all-damage temporary effect. */
    public void activate(UUID player, long expiresAt) {
        if (player == null) return;
        allDamageExpiryTicks.merge(player, expiresAt, Math::max);
    }

    /** Legacy C1 rage API: all-damage temporary effect. */
    public boolean isActive(UUID player, long worldTick) {
        if (player == null) return false;
        Long expiry = allDamageExpiryTicks.get(player);
        if (expiry == null) return false;
        if (worldTick < expiry) return true;
        allDamageExpiryTicks.remove(player, expiry);
        return false;
    }

    public void activate(UUID player, RuneDamageChannel channel, long expiresAt) {
        if (player == null || channel == null) return;
        channelExpiryTicks.compute(player, (uuid, current) -> {
            EnumMap<RuneDamageChannel, Long> values = current == null
                    ? new EnumMap<>(RuneDamageChannel.class) : current;
            Long existing = values.get(channel);
            if (existing == null || expiresAt > existing) values.put(channel, expiresAt);
            return values;
        });
    }

    public boolean isActive(UUID player, RuneDamageChannel channel, long worldTick) {
        if (player == null || channel == null) return false;
        EnumMap<RuneDamageChannel, Long> values = channelExpiryTicks.get(player);
        if (values == null) return false;
        Long expiry = values.get(channel);
        if (expiry == null) return false;
        if (worldTick < expiry) return true;
        values.remove(channel);
        if (values.isEmpty()) channelExpiryTicks.remove(player, values);
        return false;
    }

    public void clear() {
        allDamageExpiryTicks.clear();
        channelExpiryTicks.clear();
    }
}
