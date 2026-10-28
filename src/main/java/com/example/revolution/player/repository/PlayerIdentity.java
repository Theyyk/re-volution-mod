package com.example.revolution.player.repository;

import java.util.UUID;

/** Minimal persisted identity used by administration when a player is offline. */
public final class PlayerIdentity {
    public final UUID uuid;
    public final String lastKnownName;

    public PlayerIdentity(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName == null ? "" : lastKnownName;
    }
}
