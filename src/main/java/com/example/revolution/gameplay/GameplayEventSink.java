package com.example.revolution.gameplay;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gameplay-neutral boundary for facts that were already confirmed by authoritative server systems.
 * Implementations may observe them, but must not recalculate combat or rewards.
 */
public interface GameplayEventSink {
    GameplayEventSink NONE = new GameplayEventSink() {
        @Override public void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) { }
        @Override public void earnedResource(UUID playerUuid, String resource, BigDecimal amount) { }
    };

    void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount);
    void earnedResource(UUID playerUuid, String resource, BigDecimal amount);
}
