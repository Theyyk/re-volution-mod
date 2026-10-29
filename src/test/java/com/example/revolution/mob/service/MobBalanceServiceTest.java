package com.example.revolution.mob.service;

import com.example.revolution.balance.config.BalanceConfigCodec;
import com.example.revolution.balance.config.BalanceConfigParser;
import com.example.revolution.balance.curve.BalanceCurveEngine;
import com.example.revolution.balance.service.BalanceBootstrap;
import com.example.revolution.balance.service.BalanceService;
import com.example.revolution.balance.validation.BalanceConfigValidator;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Regression coverage for canonical MobL_N Balance Core overrides. */
public final class MobBalanceServiceTest {
    public static void main(String[] args) {
        bundledDefaultsApplyZbtLocationOne();
        activeSnapshotOverridesCanonicalMob();
        partialAndLegacyFallbacks();
        invalidBalanceValuesFailClosed();
        System.out.println("Mob Balance canonical overrides and persistence fallbacks passed");
    }

    private static void bundledDefaultsApplyZbtLocationOne() {
        FakeMobRepository stored = new FakeMobRepository();
        stored.mob = mob("Mob1_8", "10");
        stored.resource = "rune_dust";
        stored.reward = new BigDecimal("5");
        MobRepository repository = new BalanceAwareMobRepository(stored,
                new MobBalanceService(BalanceBootstrap.activateBundledDefaults()));

        check(repository.getMob("Mob1_8").hp.compareTo(new BigDecimal("2500")) == 0,
                "ZBT Location 1 HP must override persistence");
        check(repository.getMobResource("Mob1_8").equals("coins"),
                "ZBT Location 1 reward resource must override persistence");
        check(repository.getMobAmountValue("Mob1_8").compareTo(new BigDecimal("1000")) == 0,
                "ZBT Location 1 reward amount must override persistence");

        stored.mob = mob("Mob1_41", "1");
        check(repository.getMob("Mob1_41").hp.compareTo(new BigDecimal("30000000")) == 0,
                "Stage 41 boss uses fixed 30M HP");
        check(repository.getMobAmountValue("Mob1_41").compareTo(new BigDecimal("15000000")) == 0,
                "Stage 41 boss reward is fixed 15M coins");
    }

    private static void activeSnapshotOverridesCanonicalMob() {
        FakeMobRepository stored = new FakeMobRepository();
        stored.mob = mob("Mob1_8", "10");
        stored.resource = "coins";
        stored.reward = new BigDecimal("5");
        BalanceService balance = newBalance();
        balance.activate(config(
                "\"mobs.location.1.slot.8.hp\":\"2500\","
                        + "\"mobs.location.1.slot.8.resource\":\"rune_dust\","
                        + "\"mobs.location.1.slot.8.reward\":\"33.5\""));
        MobRepository repository = new BalanceAwareMobRepository(stored, new MobBalanceService(balance));

        check(repository.getMob("Mob1_8").hp.compareTo(new BigDecimal("2500")) == 0,
                "canonical HP must come from active balance");
        check(repository.getMobResource("Mob1_8").equals("rune_dust"),
                "canonical reward resource must come from active balance");
        check(repository.getMobAmountValue("Mob1_8").compareTo(new BigDecimal("33.5")) == 0,
                "canonical reward amount must come from active balance");
        check(repository.getMobAmount("Mob1_8") == 33,
                "legacy integer bridge must use resolved reward without changing its truncation contract");
    }

    private static void partialAndLegacyFallbacks() {
        FakeMobRepository stored = new FakeMobRepository();
        stored.mob = mob("Mob2_3", "77");
        stored.resource = "coins";
        stored.reward = new BigDecimal("9");
        BalanceService balance = newBalance();
        balance.activate(config("\"mobs.location.2.slot.3.hp\":\"88\""));
        MobRepository repository = new BalanceAwareMobRepository(stored, new MobBalanceService(balance));

        check(repository.getMob("Mob2_3").hp.compareTo(new BigDecimal("88")) == 0,
                "partial HP override must apply");
        check(repository.getMobResource("Mob2_3").equals("coins"),
                "omitted resource must preserve persistence fallback");
        check(repository.getMobAmountValue("Mob2_3").compareTo(new BigDecimal("9")) == 0,
                "omitted reward must preserve persistence fallback");

        stored.mob = mob("LegacyWolf", "12");
        stored.reward = new BigDecimal("4");
        check(repository.getMob("LegacyWolf").hp.compareTo(new BigDecimal("12")) == 0,
                "non-canonical mob identity must remain persistence-driven");
        check(repository.getMobAmountValue("LegacyWolf").compareTo(new BigDecimal("4")) == 0,
                "non-canonical reward must remain persistence-driven");
    }

    private static void invalidBalanceValuesFailClosed() {
        FakeMobRepository stored = new FakeMobRepository();
        stored.mob = mob("Mob1_1", "10");
        stored.resource = "coins";
        stored.reward = BigDecimal.ONE;

        BalanceService invalidHp = newBalance();
        invalidHp.activate(config("\"mobs.location.1.slot.1.hp\":\"0\""));
        expectFailure(() -> new BalanceAwareMobRepository(stored, new MobBalanceService(invalidHp)).getMob("Mob1_1"),
                "non-positive HP override must be rejected");

        BalanceService invalidResource = newBalance();
        invalidResource.activate(config("\"mobs.location.1.slot.1.resource\":\"not_a_resource\""));
        expectFailure(() -> new BalanceAwareMobRepository(stored, new MobBalanceService(invalidResource)).getMobResource("Mob1_1"),
                "unknown reward resource must be rejected");

        BalanceService invalidReward = newBalance();
        invalidReward.activate(config("\"mobs.location.1.slot.1.reward\":\"-1\""));
        expectFailure(() -> new BalanceAwareMobRepository(stored, new MobBalanceService(invalidReward)).getMobAmountValue("Mob1_1"),
                "non-positive reward override must be rejected");
    }

    private static BalanceService newBalance() {
        return new BalanceService(new BalanceConfigParser(), new BalanceConfigValidator(),
                new BalanceConfigCodec(), new BalanceCurveEngine());
    }

    private static String config(String values) {
        return "{\"balanceSchemaVersion\":1,\"values\":{" + values
                + "},\"curves\":{\"test\":{\"mode\":\"FIXED\",\"value\":\"1\"}}}";
    }

    private static MobDefinition mob(String name, String hp) {
        return new MobDefinition(name, "zombie", new BigDecimal(hp), 1, 0.0D, 64.0D, 0.0D, true);
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeMobRepository implements MobRepository {
        MobDefinition mob;
        String resource = "coins";
        BigDecimal reward = BigDecimal.ONE;

        @Override public void createMob(String name, String type, BigDecimal hp, int money) { }
        @Override public void setMobSpawn(String name, double x, double y, double z) { }
        @Override public void unspawnMob(String name) { }
        @Override public void deleteMob(String name) { }
        @Override public List<MobDefinition> getAllMobs() {
            if (mob == null) return Collections.emptyList();
            List<MobDefinition> result = new ArrayList<>();
            result.add(mob);
            return result;
        }
        @Override public MobDefinition getMob(String name) {
            return mob != null && mob.name.equals(name) ? mob : null;
        }
        @Override public void setMobReward(String mobName, String resource, BigDecimal amount) {
            this.resource = resource;
            this.reward = amount;
        }
        @Override public void setMobReward(String mobName, String resource, int amount) {
            setMobReward(mobName, resource, BigDecimal.valueOf(amount));
        }
        @Override public String getMobResource(String mobName) { return resource; }
        @Override public BigDecimal getMobAmountValue(String mobName) { return reward; }
        @Override public int getMobAmount(String mobName) { return reward.intValue(); }
    }
}
