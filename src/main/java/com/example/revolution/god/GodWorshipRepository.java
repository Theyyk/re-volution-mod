package com.example.revolution.god;

import java.util.UUID;

/** Persistence boundary keeping reset-scoped God runtime separate from persistent meta/preferences. */
public interface GodWorshipRepository {
    GodWorshipModel.RunState loadRunState(UUID playerUuid);
    GodWorshipModel.PersistentState loadPersistentState(UUID playerUuid);
    void saveRunState(UUID playerUuid, GodWorshipModel.RunState state);
    void savePersistentState(UUID playerUuid, GodWorshipModel.PersistentState state);

    /** Default Mastery Reset clears only reset-scoped God progression. */
    void resetRunState(UUID playerUuid);
}
