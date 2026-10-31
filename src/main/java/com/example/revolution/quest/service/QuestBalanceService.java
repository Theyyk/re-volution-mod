package com.example.revolution.quest.service;

import com.example.revolution.balance.service.BalanceService;
import java.math.BigDecimal;

/** Typed access to the v2.7.0 Quest balance overlay. */
public final class QuestBalanceService {
    private final BalanceService balance;

    public QuestBalanceService(BalanceService balance) {
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.balance = balance;
    }

    public int boardSize() { return integer("quest.board_size", 3); }
    public int dailyRerollLimit() { return integer("quest.reroll_daily_limit", 2); }
    public long cooldownMillis() { return Math.max(1L, integer("quest.cooldown_hours", 8)) * 60L * 60L * 1000L; }

    public int difficultyTier(long completed) {
        int tier = 0;
        for (int i = 1; i <= 8; i++) {
            String key = "quest.difficulty.tier_" + i + ".completed_required";
            try {
                long threshold = Long.parseLong(balance.getActiveValue(key));
                if (completed >= threshold) tier = i; else break;
            } catch (RuntimeException missing) {
                break;
            }
        }
        return tier;
    }

    public BigDecimal target(String id) { return decimal("quest.template." + id + ".target", BigDecimal.ONE); }
    public int reward(String id) { return integer("quest.template." + id + ".reward_crystals", 1); }
    public int weight(String id) { return integer("quest.template." + id + ".weight", 1); }
    public boolean enabled(String id) { return bool("quest.template." + id + ".enabled", true); }

    private int integer(String key, int fallback) {
        try { return Integer.parseInt(balance.getActiveValue(key)); } catch (RuntimeException ignored) { return fallback; }
    }

    private BigDecimal decimal(String key, BigDecimal fallback) {
        try { return new BigDecimal(balance.getActiveValue(key)); } catch (RuntimeException ignored) { return fallback; }
    }

    private boolean bool(String key, boolean fallback) {
        try { return Boolean.parseBoolean(balance.getActiveValue(key)); } catch (RuntimeException ignored) { return fallback; }
    }
}
