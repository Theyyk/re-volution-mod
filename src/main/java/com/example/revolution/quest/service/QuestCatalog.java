package com.example.revolution.quest.service;

import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.quest.definition.QuestTemplate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** v2.7.0 catalog shape. Numeric balance and enable flags come from Balance Core. */
public final class QuestCatalog {
    private final QuestBalanceService balance;

    public QuestCatalog(QuestBalanceService balance) {
        this.balance = balance;
    }

    public List<QuestTemplate> templates() {
        List<QuestTemplate> result = new ArrayList<>();
        add(result, "clicks", GameplayEventType.CLICK_COUNT, 0, 8);
        add(result, "kill_any", GameplayEventType.MOB_KILL_ANY, 0, 8);
        add(result, "boss_kill", GameplayEventType.BOSS_KILL, 1, 8);
        add(result, "rune_purchase", GameplayEventType.RUNE_PURCHASE_ANY, 0, 8);
        add(result, "rune_enhance", GameplayEventType.RUNE_ENHANCE_ANY, 1, 8);
        add(result, "rune_awaken", GameplayEventType.RUNE_AWAKEN_ANY, 2, 8);
        add(result, "pet_purchase", GameplayEventType.PET_PURCHASE_ANY, 0, 8);
        add(result, "pet_level", GameplayEventType.PET_LEVEL_UP_ANY, 1, 8);
        add(result, "pet_awaken", GameplayEventType.PET_AWAKEN_ANY, 2, 8);
        add(result, "run_exchange", GameplayEventType.RUN_EXCHANGE_PURCHASE_ANY, 1, 8);
        add(result, "coins_earned", GameplayEventType.COINS_EARNED, 0, 8);
        add(result, "stages_completed", GameplayEventType.STAGES_COMPLETED, 0, 8);
        return Collections.unmodifiableList(result);
    }

    private void add(List<QuestTemplate> out, String id, GameplayEventType type, int minDifficulty, int maxDifficulty) {
        out.add(new QuestTemplate(id, balance.enabled(id), type, null, minDifficulty, maxDifficulty,
                balance.target(id), balance.reward(id), balance.weight(id)));
    }
}
