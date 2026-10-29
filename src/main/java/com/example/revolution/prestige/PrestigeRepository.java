package com.example.revolution.prestige;

import java.util.UUID;

public interface PrestigeRepository {
    PrestigeState load(UUID playerUuid);
    void save(UUID playerUuid, PrestigeState state);
    ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state);

    enum ResetCommitResult { APPLIED, DUPLICATE, STALE }
}
