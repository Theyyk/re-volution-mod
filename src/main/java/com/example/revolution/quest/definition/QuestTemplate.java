package com.example.revolution.quest.definition;

import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Data-driven definition used only when generating a new Quest snapshot. */
public final class QuestTemplate {
    public final String id;
    public final boolean enabled;
    public final GameplayEventType eventType;
    public final Map<String, String> filters;
    public final int minDifficulty;
    public final int maxDifficulty;
    public final BigDecimal target;
    public final int crystalReward;
    public final int weight;

    public QuestTemplate(String id, boolean enabled, GameplayEventType eventType, Map<String, String> filters,
                         int minDifficulty, int maxDifficulty, BigDecimal target, int crystalReward, int weight) {
        if (id == null || id.trim().isEmpty() || eventType == null) throw new IllegalArgumentException("quest template identity is required");
        if (target == null || target.signum() <= 0 || crystalReward < 0 || weight <= 0) throw new IllegalArgumentException("invalid quest template balance");
        this.id = id;
        this.enabled = enabled;
        this.eventType = eventType;
        this.filters = filters == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(filters));
        this.minDifficulty = Math.max(0, minDifficulty);
        this.maxDifficulty = Math.max(this.minDifficulty, maxDifficulty);
        this.target = target;
        this.crystalReward = crystalReward;
        this.weight = weight;
    }

    public boolean eligible(int difficulty) {
        return enabled && difficulty >= minDifficulty && difficulty <= maxDifficulty;
    }
}
