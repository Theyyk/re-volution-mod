package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GodWorshipServiceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000031");

    @Test
    void unlockAndUpgradeUsePolicyResourcesAndCosts() {
        FakeGodRepository gods = new FakeGodRepository();
        FakeResourceRepository balances = new FakeResourceRepository();
        balances.put("rune_dust", new BigDecimal("100"));
        balances.put("rune_shards", new BigDecimal("40"));
        FakePolicy policy = new FakePolicy();
        GodWorshipService service = new GodWorshipService(gods, new ResourceService(balances), policy);

        assertEquals(GodWorshipService.MutationResult.APPLIED, service.unlock(PLAYER, GodId.GOD_1));
        State state = gods.run.gods.get(GodId.GOD_1);
        assertEquals(1L, state.level);
        assertEquals(Phase.CHARGE, state.phase);
        assertEquals(0, balances.getPlayerResourceValue(PLAYER, "rune_dust").compareTo(new BigDecimal("75")));

        assertEquals(GodWorshipService.MutationResult.APPLIED, service.upgrade(PLAYER, GodId.GOD_1));
        assertEquals(2L, state.level);
        assertEquals(0, balances.getPlayerResourceValue(PLAYER, "rune_shards").compareTo(new BigDecimal("30")));
    }

    @Test
    void finalManualChargeClickActivatesAndIncludesPermanentDuration() {
        FakeGodRepository gods = new FakeGodRepository();
        FakeResourceRepository balances = new FakeResourceRepository();
        FakePolicy policy = new FakePolicy();
        State state = gods.run.gods.get(GodId.GOD_1);
        state.unlocked = true;
        state.level = 1L;
        state.phase = Phase.CHARGE;
        state.chargeProgress = 9L;
        gods.persistent.permanentDurationLevel = 3L;
        GodWorshipService service = new GodWorshipService(gods, new ResourceService(balances), policy);

        assertEquals(GodWorshipService.MutationResult.APPLIED, service.click(PLAYER, GodId.GOD_1, 50_000L));
        assertEquals(Phase.BUFF_ACTIVE, state.phase);
        assertEquals(63_000L, state.buffActiveUntilMillis);
        assertEquals(50_000L, state.offeringSettledUntilMillis);
        assertEquals(0L, state.chargeProgress);
    }

    @Test
    void insufficientUnlockResourceLeavesGodLocked() {
        FakeGodRepository gods = new FakeGodRepository();
        FakeResourceRepository balances = new FakeResourceRepository();
        balances.put("rune_dust", new BigDecimal("24"));
        GodWorshipService service = new GodWorshipService(gods, new ResourceService(balances), new FakePolicy());

        assertEquals(GodWorshipService.MutationResult.INSUFFICIENT_RESOURCE, service.unlock(PLAYER, GodId.GOD_1));
        assertEquals(false, gods.run.gods.get(GodId.GOD_1).unlocked);
        assertEquals(0L, gods.run.gods.get(GodId.GOD_1).level);
    }

    private static final class FakeGodRepository implements GodWorshipRepository {
        private RunState run = new RunState();
        private PersistentState persistent = new PersistentState();
        @Override public RunState loadRunState(UUID playerUuid) { return run; }
        @Override public PersistentState loadPersistentState(UUID playerUuid) { return persistent; }
        @Override public void saveRunState(UUID playerUuid, RunState state) { run = state; }
        @Override public void savePersistentState(UUID playerUuid, PersistentState state) { persistent = state; }
        @Override public void resetRunState(UUID playerUuid) { run = new RunState(); }
    }

    private static final class FakeResourceRepository implements ResourceRepository {
        private final Map<String, BigDecimal> values = new HashMap<>();
        void put(String resource, BigDecimal value) { values.put(resource, value); }
        @Override public void purgeDeprecatedLightnings() { }
        @Override public void purgeDeprecatedLightnings(UUID uuid) { }
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            BigDecimal value = values.get(resource);
            return value == null ? BigDecimal.ZERO : value;
        }
        @Override public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) { values.put(canonical, value); }
    }

    private static final class FakePolicy implements GodWorshipService.Policy {
        @Override public boolean enabled(GodId god) { return true; }
        @Override public String unlockResource(GodId god) { return "rune_dust"; }
        @Override public String upgradeResource(GodId god) { return "rune_shards"; }
        @Override public EffectResolver.Stat primaryChannel(GodId god) { return EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER; }
        @Override public EffectResolver.Stat secondaryChannel(GodId god) { return EffectResolver.Stat.CLICK_DAMAGE_MULTIPLIER; }
        @Override public BigDecimal unlockCost(GodId god) { return new BigDecimal("25"); }
        @Override public BigDecimal upgradeCost(GodId god, long currentLevel) { return new BigDecimal("10"); }
        @Override public long chargeRequired(GodId god) { return 10L; }
        @Override public long chargePerClick(GodId god) { return 1L; }
        @Override public long buffDurationMillis(GodId god) { return 10_000L; }
        @Override public long cooldownDurationMillis(GodId god) { return 5_000L; }
        @Override public long cooldownReductionPerClickMillis(GodId god) { return 1_000L; }
        @Override public long permanentDurationMillis(long permanentDurationLevel) { return permanentDurationLevel * 1_000L; }
    }
}
