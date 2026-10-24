package com.example.revolution.location.repository;

import java.util.UUID;

/** Persistent player preference for immediate post-unlock fast-auto travel. */
public interface FastAutoPreferenceRepository {
    boolean isFastAutoEnabled(UUID playerUuid);
    void setFastAutoEnabled(UUID playerUuid, boolean enabled);
}
