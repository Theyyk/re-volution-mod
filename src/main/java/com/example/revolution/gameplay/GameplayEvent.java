package com.example.revolution.gameplay;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable server-confirmed fact. It intentionally contains no Quest-specific semantics. */
public final class GameplayEvent {
    public final UUID playerUuid;
    public final GameplayEventType type;
    public final BigDecimal amount;
    public final Map<String, String> metadata;
    public final String source;

    public GameplayEvent(UUID playerUuid, GameplayEventType type, BigDecimal amount, Map<String, String> metadata, String source) {
        if (playerUuid == null || type == null) throw new IllegalArgumentException("player and event type are required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("event amount must be positive");
        this.playerUuid = playerUuid;
        this.type = type;
        this.amount = amount;
        this.metadata = metadata == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        this.source = source == null ? "gameplay" : source;
    }

    public GameplayEvent(UUID playerUuid, GameplayEventType type) {
        this(playerUuid, type, BigDecimal.ONE, null, "gameplay");
    }
}
