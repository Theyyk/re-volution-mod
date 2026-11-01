package com.example.revolution.unique;

import java.util.UUID;

/** Persistence port for permanent Unique Rune state. */
public interface UniqueRuneRepository {
    UniqueRuneModel.PlayerState load(UUID playerUuid);
    void save(UUID playerUuid, UniqueRuneModel.PlayerState state);
}
