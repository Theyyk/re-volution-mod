package com.example.revolution.rune.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneStatType;
import java.util.List;

/** Regression coverage for v1.4.5 canonical rune effect definitions. */
public final class RuneEffectBalanceServiceTest {
    public static void main(String[] args) {
        bundledPoisonBaseline();
        activeSnapshotOverride();
        System.out.println("Canonical rune effect groups, ranks, selectors and enhancement steps passed");
    }

    private static void bundledPoisonBaseline() {
        RuneEffectBalanceService service = new RuneEffectBalanceService(BalanceBootstrap.activateBundledDefaults());
        check(service.group(RuneStatusConfig.STATUS_POISON, 0).equals("killer_dagger"),
                "slot mapping must come from canonical balance data");
        check(service.group(RuneStatusConfig.STATUS_POISON, 13).equals("snake_fang"),
                "poison flat group mapping must be canonical");

        RuneEffectBalanceService.ResolvedEffect dagger = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 0, 2, 2));
        check(dagger.type == RuneEffectBalanceService.EffectType.STAT, "dagger effect type");
        check(dagger.statType == RuneStatType.CLICK_DAMAGE_FLAT, "dagger stat type");
        check(dagger.tags.contains("click"), "tags are retained");
        check(dagger.acceptsTargetedPower, "dagger accepts sharpening power");
        check(close(dagger.primaryValue, 3.6D), "rank-specific value uses the slot enhancement step");
        check(close(dagger.targetedPowerScale, 3.6D), "targeted power keeps legacy pre-rank scaling semantics");

        RuneEffectBalanceService.ResolvedEffect proc = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 7, 3, 2));
        check(proc.type == RuneEffectBalanceService.EffectType.CLICK_DAMAGE_PROC, "proc type");
        check(proc.targetSelector.equals("HIT_MOB"), "proc selector");
        check(close(proc.primaryValue, 15.0D), "proc chance stays unscaled");
        check(close(proc.secondaryValue, 21.6D), "proc multiplier has individual enhancement scaling");

        RuneEffectBalanceService.ResolvedEffect rage = only(service.effects(
                RuneStatusConfig.STATUS_POISON, 10, 1, 0));
        check(rage.type == RuneEffectBalanceService.EffectType.TEMPORARY_ALL_DAMAGE_MULTIPLIER, "rage type");
        check(close(rage.primaryValue, 2.0D), "rage normal multiplier");
        check(close(rage.durationSeconds, 5.0D), "rage duration");
        check(rage.maxStacks == 1, "rage stack limit");

        List<RuneEffectBalanceService.ResolvedEffect> hybrid = service.effects(
                RuneStatusConfig.STATUS_POISON, 11, 2, 0);
        check(hybrid.size() == 2, "toxic shard keeps two explicit effects");
        check(hybrid.get(0).statType == RuneStatType.CLICK_DAMAGE_MULTIPLIER, "hybrid click effect");
        check(hybrid.get(1).statType == RuneStatType.POISON_DAMAGE_MULTIPLIER, "hybrid poison effect");
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
        check(close(effect.primaryValue, 16.5D),
                "active Balance Core values must drive runtime instead of Java constants");
    }

    private static RuneEffectBalanceService.ResolvedEffect only(List<RuneEffectBalanceService.ResolvedEffect> values) {
        check(values.size() == 1, "expected one effect");
        return values.get(0);
    }

    private static boolean close(double left, double right) {
        return Math.abs(left - right) < 0.000001D;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
