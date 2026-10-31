package com.example.revolution.achievement;

import com.example.revolution.gameplay.GameplayEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AchievementDefinition {
    public final String achievementId;
    public final String name;
    public final AchievementCategory category;
    public final GameplayEventType eventType;
    public final Map<String, String> filters;
    public final boolean hidden;
    public final List<AchievementStage> stages;

    public AchievementDefinition(String achievementId, String name, AchievementCategory category,
                                 GameplayEventType eventType, Map<String, String> filters,
                                 boolean hidden, List<AchievementStage> stages) {
        if (achievementId == null || achievementId.trim().isEmpty()) throw new IllegalArgumentException("achievementId is required");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("achievement name is required");
        if (category == null || eventType == null) throw new IllegalArgumentException("category and eventType are required");
        if (stages == null || stages.isEmpty()) throw new IllegalArgumentException("at least one stage is required");
        int expected = 1;
        java.math.BigDecimal previous = null;
        for (AchievementStage stage : stages) {
            if (stage == null || stage.stageIndex != expected++) throw new IllegalArgumentException("stages must be sequential from 1");
            if (previous != null && stage.target.compareTo(previous) <= 0) throw new IllegalArgumentException("stage targets must strictly increase");
            previous = stage.target;
        }
        this.achievementId = achievementId;
        this.name = name;
        this.category = category;
        this.eventType = eventType;
        this.filters = Collections.unmodifiableMap(filters == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(filters));
        this.hidden = hidden;
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
    }

    public AchievementStage stage(int oneBasedIndex) {
        return oneBasedIndex < 1 || oneBasedIndex > stages.size() ? null : stages.get(oneBasedIndex - 1);
    }

    public boolean matches(Map<String, String> metadata) {
        if (filters.isEmpty()) return true;
        if (metadata == null) return false;
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            if (!filter.getValue().equals(metadata.get(filter.getKey()))) return false;
        }
        return true;
    }
}
