package com.example.revolution.quest.service;

import com.example.revolution.quest.definition.QuestTemplate;
import com.example.revolution.quest.state.QuestEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Weighted bounded generator with duplicate prevention against the complete current board. */
public final class QuestGenerator {
    private final QuestCatalog catalog;
    private final Random random;

    public QuestGenerator(QuestCatalog catalog) { this(catalog, new Random()); }

    QuestGenerator(QuestCatalog catalog, Random random) {
        if (catalog == null || random == null) throw new IllegalArgumentException("quest generator dependencies are required");
        this.catalog = catalog;
        this.random = random;
    }

    public QuestEntry generate(int difficultyTier, List<QuestEntry> currentBoard) {
        Set<String> occupied = new HashSet<>();
        if (currentBoard != null) for (QuestEntry entry : currentBoard) if (entry != null) occupied.add(entry.variantKey());

        List<QuestTemplate> eligible = new ArrayList<>();
        for (QuestTemplate template : catalog.templates()) {
            if (!template.eligible(difficultyTier)) continue;
            if (!occupied.contains(template.id + "|{}")) eligible.add(template);
        }
        if (eligible.isEmpty()) throw new IllegalStateException("no eligible unique Quest template for difficulty " + difficultyTier);

        long totalWeight = 0L;
        for (QuestTemplate template : eligible) totalWeight += Math.max(1, template.weight);
        long roll = Math.floorMod(random.nextLong(), totalWeight);
        QuestTemplate selected = eligible.get(eligible.size() - 1);
        for (QuestTemplate template : eligible) {
            roll -= Math.max(1, template.weight);
            if (roll < 0L) { selected = template; break; }
        }

        QuestEntry entry = new QuestEntry();
        entry.templateId = selected.id;
        entry.eventType = selected.eventType;
        entry.filters.putAll(selected.filters);
        entry.target = selected.target;
        entry.crystalReward = selected.crystalReward;
        entry.difficultyTier = difficultyTier;
        return entry;
    }
}
