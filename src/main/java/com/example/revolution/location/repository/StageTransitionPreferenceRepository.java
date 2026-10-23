package com.example.revolution.location.repository;

import java.util.UUID;

/** Player-level preference for automatic stage transitions. */
public interface StageTransitionPreferenceRepository {
    boolean isAutoTransitionEnabled(UUID playerUuid);
    void setAutoTransitionEnabled(UUID playerUuid, boolean enabled);
}
