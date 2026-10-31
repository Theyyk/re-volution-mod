package com.example.revolution.achievement;

import com.example.revolution.gameplay.GameplayEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable indexed definitions. High-frequency event routing never scans the full catalog. */
public final class AchievementCatalog {
    private final Map<String, AchievementDefinition> byId = new LinkedHashMap<>();
    private final Map<GameplayEventType, List<AchievementDefinition>> byEvent = new EnumMap<>(GameplayEventType.class);

    public AchievementCatalog(List<AchievementDefinition> definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions are required");
        for (AchievementDefinition definition : definitions) {
            if (definition == null) throw new IllegalArgumentException("null achievement definition");
            if (byId.put(definition.achievementId, definition) != null) throw new IllegalArgumentException("duplicate achievementId: " + definition.achievementId);
            List<AchievementDefinition> indexed = byEvent.get(definition.eventType);
            if (indexed == null) {
                indexed = new ArrayList<>();
                byEvent.put(definition.eventType, indexed);
            }
            indexed.add(definition);
        }
        for (Map.Entry<GameplayEventType, List<AchievementDefinition>> entry : byEvent.entrySet()) {
            entry.setValue(Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
    }

    public AchievementDefinition get(String achievementId) { return byId.get(achievementId); }
    public List<AchievementDefinition> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }
    public List<AchievementDefinition> interested(GameplayEventType type) {
        List<AchievementDefinition> result = byEvent.get(type);
        return result == null ? Collections.<AchievementDefinition>emptyList() : result;
    }
}
