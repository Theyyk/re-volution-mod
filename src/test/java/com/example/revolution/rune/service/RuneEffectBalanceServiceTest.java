package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneStatType;
import java.util.List;

/** Regression coverage for the current canonical ZBT rune effect definitions. */
public final class RuneEffectBalanceServiceTest {
    public static void main(String[] args) {
        bundledPoisonBaseline();
        activeSnapshotOverride();
        stableCollectionNamespace();
        System.out.println("Canonical rune effect groups, ranks, selectors and enhancement steps passed");
    }

    private static void bundledPoisonBaseline() {
        RuneEffectBalanceService service = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());
        check(service.group(RuneStatusConfig.STATUS_POISON, 0).equals("killer_dagger"), "dagger slot mapping");
        check(service.group(RuneCollectionId.COLLECTION_01, 0).equals("killer_dagger"), "C1 stable id keeps poison namespace");
        check(service.group(RuneStatusConfig.STATUS_POISON, 13).equals("snake_fang"), "fang slot mapping");

        RuneEffectBalanceService.ResolvedEffect dagger = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 0, 2, 2));
        check(dagger.type == RuneEffectBalanceService.EffectType.STAT, "dagger effect type");
        check(dagger.statType == RuneStatType.CLICK_DAMAGE_FLAT, "dagger stat type");
        check(dagger.acceptsTargetedPower, "dagger accepts sharpening power");
        check(close(dagger.primaryValue, 144.0D), "Improved dagger +2 enhancement = 120 * 1.2");
        check(close(dagger.targetedPowerScale, 1.0D), "targeted power is added directly without target-rank rescaling");

        RuneEffectBalanceService.ResolvedEffect proc = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 7, 3, 2));
        check(close(proc.primaryValue, 15.0D), "proc chance stays max-style and unscaled");
        check(close(proc.secondaryValue, 12.0D), "Advanced x10 proc receives +20% enhancement");

        RuneEffectBalanceService.ResolvedEffect rage1 = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 10, 1, 0));
        RuneEffectBalanceService.ResolvedEffect rage2 = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 10, 2, 0));
        RuneEffectBalanceService.ResolvedEffect rage3 = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 10, 3, 0));
        check(close(rage1.primaryValue, 1.35D) && close(rage1.durationSeconds, 4.0D), "Normal rage");
        check(close(rage2.primaryValue, 1.55D) && close(rage2.durationSeconds, 5.0D), "Improved rage");
        check(close(rage3.primaryValue, 1.85D) && close(rage3.durationSeconds, 6.0D), "Advanced rage");
        check(rage3.maxStacks == 1, "rage stack limit");

        List<RuneEffectBalanceService.ResolvedEffect> hybrid = service.effects(
                RuneStatusConfig.STATUS_POISON, 11, 2, 0);
        check(hybrid.size() == 2, "toxic shard has click and poison channels");
        check(close(hybrid.get(0).primaryValue, 1.45D), "Improved toxic shard click multiplier");
        check(close(hybrid.get(1).primaryValue, 1.60D), "Improved toxic shard poison multiplier");

        RuneEffectBalanceService.ResolvedEffect mark = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 22, 3, 0));
        check(mark.statType == RuneStatType.ALL_DAMAGE_VULNERABILITY, "infection mark affects click and poison");
        check(close(mark.primaryValue, 30.0D), "Advanced vulnerability +30%");

        RuneEffectBalanceService.ResolvedEffect relic = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 25, 3, 0));
        check(relic.statType == RuneStatType.COIN_REWARD_PERCENT, "hunter relic targets coin rewards");
        check(close(relic.primaryValue, 40.0D), "Advanced hunter relic +40% coins");
    }

    private static void activeSnapshotOverride() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{"
                + "\"balanceSchemaVersion\":1,"
                + "\"values\":{"
                + "\"runes.poison.slot.0.group\":\"test\","
                + "\"runes.poison.effect.test.count\":\"1\","
                + "\"runes.poison.effect.test.1.type\":\"STAT\","
                + "\"runes.poison.effect.test.1.tags\":\"poison,test\","
                + "\"runes.poison.effect.test.1.target_selector\":\"SELF\","
                + "\"runes.poison.effect.test.1.stat_type\":\"POISON_DAMAGE_FLAT\","
                + "\"runes.poison.effect.test.1.enhancement_step\":\"0.25\","
                + "\"runes.poison.effect.test.1.enhancement_field\":\"PRIMARY\","
                + "\"runes.poison.effect.test.1.rank.1.primary\":\"7\","
                + "\"runes.poison.effect.test.1.rank.2.primary\":\"11\","
                + "\"runes.poison.effect.test.1.rank.3.primary\":\"19\"},"
                + "\"curves\":{\"test\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");
        RuneEffectBalanceService service = new RuneEffectBalanceService(balance);
        RuneEffectBalanceService.ResolvedEffect effect = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 0, 2, 2));
        check(close(effect.primaryValue, 16.5D), "active snapshot overrides bundled data");
    }

    private static void stableCollectionNamespace() {
        BalanceService balance = new BalanceService(
                new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
        balance.activate("{"
                + "\"balanceSchemaVersion\":1,"
                + "\"values\":{"
                + "\"runes.collection_02.slot.0.group\":\"decay_flat_01\","
                + "\"runes.collection_02.effect.decay_flat_01.count\":\"1\","
                + "\"runes.collection_02.effect.decay_flat_01.1.type\":\"STAT\","
                + "\"runes.collection_02.effect.decay_flat_01.1.tags\":\"decay,damage,flat\","
                + "\"runes.collection_02.effect.decay_flat_01.1.target_selector\":\"SELF\","
                + "\"runes.collection_02.effect.decay_flat_01.1.stat_type\":\"DECAY_DAMAGE_FLAT\","
                + "\"runes.collection_02.effect.decay_flat_01.1.rank.1.primary\":\"5\","
                + "\"runes.collection_02.effect.decay_flat_01.1.rank.2.primary\":\"15\","
                + "\"runes.collection_02.effect.decay_flat_01.1.rank.3.primary\":\"45\","
                + "\"runes.collection_02.effect.decay_flat_01.1.rank.4.primary\":\"135\"},"
                + "\"curves\":{\"test\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}");
        RuneEffectBalanceService service = new RuneEffectBalanceService(balance);
        RuneEffectBalanceService.ResolvedEffect effect = only(service.effects(
                RuneCollectionId.COLLECTION_02, 0, 4, 0));
        check(effect.statType == RuneStatType.DECAY_DAMAGE_FLAT, "C2 stable namespace stat type");
        check(close(effect.primaryValue, 135.0D), "C2 Mythic value comes from collection namespace");
    }

    private static RuneEffectBalanceService.ResolvedEffect only(List<RuneEffectBalanceService.ResolvedEffect> values) {
        check(values.size() == 1, "expected one effect");
        return values.get(0);
    }

    private static boolean close(double left, double right) { return Math.abs(left - right) < 0.000001D; }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
