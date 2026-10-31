package com.example.revolution.quest.repository;

import com.example.revolution.quest.state.QuestPlayerState;
import java.util.UUID;

public interface QuestRepository {
    QuestPlayerState load(UUID playerUuid);
    void save(UUID playerUuid, QuestPlayerState state);
}
