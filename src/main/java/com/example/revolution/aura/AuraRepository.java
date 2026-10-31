package com.example.revolution.aura;

import java.util.UUID;

public interface AuraRepository {
    AuraModel.PlayerState load(UUID playerUuid);
    void save(UUID playerUuid, AuraModel.PlayerState state);
}
