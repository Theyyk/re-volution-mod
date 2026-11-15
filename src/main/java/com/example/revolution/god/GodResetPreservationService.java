package com.example.revolution.god;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Applies God Run State preservation after a successfully committed ordinary or
 * Mastery Reset. God Persistent State is deliberately never reset here.
 *
 * Both the Balance Core feature flag and the completed Mechanic Unlock are
 * required; reaching the rune-progress threshold alone is insufficient.
 */
public final class GodResetPreservationService {
    private final GodWorshipRepository repository;
    private final BooleanSupplier featureEnabled;
    private final Predicate<UUID> mechanicUnlocked;

    public GodResetPreservationService(GodWorshipRepository repository,
                                       BooleanSupplier featureEnabled,
                                       Predicate<UUID> mechanicUnlocked) {
        if (repository == null || featureEnabled == null || mechanicUnlocked == null) {
            throw new IllegalArgumentException("God Reset preservation dependencies are required");
        }
        this.repository = repository;
        this.featureEnabled = featureEnabled;
        this.mechanicUnlocked = mechanicUnlocked;
    }

    public void afterAppliedReset(UUID playerUuid) {
        if (playerUuid == null) return;
        if (featureEnabled.getAsBoolean() && mechanicUnlocked.test(playerUuid)) return;
        repository.resetRunState(playerUuid);
    }
}
