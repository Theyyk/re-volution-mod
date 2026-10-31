package com.example.revolution.achievement;

import com.example.revolution.balance.service.BalanceService;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Boss identity is content data keyed by stable server-side mob spawn IDs, never display names. */
public final class AchievementMobClassifier {
    private final Set<String> bossIds;

    public AchievementMobClassifier(BalanceService balance) {
        if (balance == null || balance.getActiveSnapshot() == null) throw new IllegalArgumentException("active balance is required");
        String encoded = balance.getActiveSnapshot().getConfig().getValues().get("achievement.boss.ids");
        Set<String> parsed = new LinkedHashSet<>();
        if (encoded != null && !encoded.trim().isEmpty()) {
            for (String token : encoded.split(";")) {
                String id = token.trim();
                if (!id.isEmpty()) parsed.add(id);
            }
        }
        bossIds = Collections.unmodifiableSet(parsed);
    }

    public boolean isBoss(String stableMobId) {
        return stableMobId != null && bossIds.contains(stableMobId);
    }

    public Set<String> bossIds() { return bossIds; }
}
