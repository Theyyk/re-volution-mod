package com.example.revolution.quest.state;

import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.quest.definition.QuestState;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persisted Quest snapshot. Generated balance never changes while this entry exists. */
public final class QuestEntry {
    public String entryId = UUID.randomUUID().toString();
    public String templateId;
    public GameplayEventType eventType;
    public Map<String, String> filters = new LinkedHashMap<>();
    public BigDecimal target = BigDecimal.ONE;
    public BigDecimal progress = BigDecimal.ZERO;
    public QuestState state = QuestState.ACTIVE;
    public int crystalReward;
    public int difficultyTier;

    public QuestEntry copy() {
        QuestEntry copy = new QuestEntry();
        copy.entryId = entryId;
        copy.templateId = templateId;
        copy.eventType = eventType;
        copy.filters = new LinkedHashMap<>(filters);
        copy.target = target;
        copy.progress = progress;
        copy.state = state;
        copy.crystalReward = crystalReward;
        copy.difficultyTier = difficultyTier;
        return copy;
    }

    public String variantKey() {
        return String.valueOf(templateId) + "|" + filters.toString();
    }
}
