package com.example.revolution.achievement;

import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Pure Balance Core regression for ZBT definitions and meta progression beyond the seed table. */
public final class AchievementBalanceTest {
    public static void main(String[] args) {
        BalanceService balance = BalanceBootstrap.activateBundledDefaults();
        AchievementCatalog catalog = BalanceAchievementCatalogFactory.fromBalance(balance);
        BalanceAchievementPolicy policy = new BalanceAchievementPolicy(balance);

        check("draft".equals(balance.getActiveValue("achievement.zbt_status")), "achievement content remains ZBT Draft");
        check(policy.level(0L) == 1, "zero points starts at level 1");
        check(policy.level(16000L) == 9, "level crosses the last explicit threshold");
        check(policy.level(32000L) == 10, "level continues through table tail growth");
        check(policy.nextLevelThreshold(32000L).compareTo(new BigDecimal("64000")) == 0,
                "next threshold continues beyond the explicit table");
        check(policy.bonusPercent(32000L).compareTo(new BigDecimal("8.47")) == 0,
                "economy bonus continues beyond the explicit table");

        AchievementDefinition pets = catalog.get("pets_obtained");
        check(pets != null && pets.stages.get(pets.stages.size() - 1).target.compareTo(new BigDecimal("18")) == 0,
                "Pet final ZBT stage matches the factual 18-pet catalog");

        Set<String> damageIds = new HashSet<>(Arrays.asList(
                "damage_click", "damage_poison", "damage_burning", "damage_bleeding",
                "damage_curse", "damage_wither", "damage_decay"));
        for (String id : damageIds) check(catalog.get(id) != null, "missing damage channel definition " + id);
        check(catalog.get("damage_total") != null, "total damage definition exists");
        check(catalog.get("boss_kills") != null, "boss-kill foundation is configured");

        for (AchievementDefinition definition : catalog.all()) {
            check(definition.eventType != GameplayEventType.GOD_UNLOCK
                            && definition.eventType != GameplayEventType.GOD_UPGRADE
                            && definition.eventType != GameplayEventType.GOD_BUFF_ACTIVATE,
                    "God Worship achievements are out of scope");
        }
        check(catalog.get("forge") == null, "Forge gameplay achievement content is not implemented");
        check(GameplayEventType.FORGE_ITEM_CRAFTED != null, "future Forge factual event hook exists");
        System.out.println("Achievement Balance Core content and unlimited meta progression regressions passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
