package com.example.revolution.pet.repository;

import com.example.revolution.pet.state.PetPlayerState;
import java.util.UUID;

/** Persistence boundary for the embedded player Pet state. */
public interface PetRepository {
    PetPlayerState load(UUID playerUuid);
    void save(UUID playerUuid, PetPlayerState state);
}
