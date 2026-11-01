package com.example.revolution.mechanic;

import java.util.UUID;

public interface MechanicUnlockRepository {
    MechanicUnlockModel.PlayerState load(UUID playerUuid);
    void save(UUID playerUuid, MechanicUnlockModel.PlayerState state);
}
