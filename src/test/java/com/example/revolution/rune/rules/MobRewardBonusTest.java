package com.example.revolution.rune.rules;

import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.definition.RuneStatType;

public final class MobRewardBonusTest {
    public static void run() {
        check(MobRewardBonus.statTypeFor("coins") == RuneStatType.COIN_REWARD_PERCENT,
                "Coins from mobs must use the coin reward stat");
        check(MobRewardBonus.statTypeFor("crystals") == null,
                "Crystals must not receive any rune reward bonus");
        check(MobRewardBonus.statTypeFor(ResourceDefinitions.RUNE_DUST) == RuneStatType.RESOURCE_REWARD_PERCENT,
                "Rune dust must use the rune-resource reward stat");
        check(MobRewardBonus.statTypeFor(ResourceDefinitions.RUNE_SHARDS) == RuneStatType.RESOURCE_REWARD_PERCENT,
                "Rune shards must use the rune-resource reward stat");
        check(MobRewardBonus.statTypeFor(ResourceDefinitions.RUNE_CORES) == RuneStatType.RESOURCE_REWARD_PERCENT,
                "Rune cores must use the rune-resource reward stat");
        check(MobRewardBonus.statTypeFor("lightnings") == null,
                "Retired or unrelated resources must not receive a reward bonus");

        check(MobRewardBonus.apply(100, 25.0D) == 125,
                "A 25 percent mob bonus should increase 100 to 125");
        check(MobRewardBonus.apply(3, 50.0D) == 4,
                "Integer mob rewards should floor fractional results");
        check(MobRewardBonus.apply(100, 0.0D) == 100,
                "Zero bonus must preserve the base reward");
        check(MobRewardBonus.apply(100, Double.NaN) == 100,
                "Invalid bonus input must preserve the base reward");

        System.out.println("Mob reward rune bonus policy regression checks passed");
    }

    public static void main(String[] args) {
        run();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
