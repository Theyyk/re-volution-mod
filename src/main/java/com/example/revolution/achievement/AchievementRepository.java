package com.example.revolution.achievement;

import java.util.UUID;

public interface AchievementRepository {
    AchievementPlayerState load(UUID playerUuid);
    void save(UUID playerUuid, AchievementPlayerState state);
}
