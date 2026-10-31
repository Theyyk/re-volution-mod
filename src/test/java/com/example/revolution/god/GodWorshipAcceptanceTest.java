package com.example.revolution.god;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventSink;
import com.example.revolution.gameplay.GameplayEventType;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Phase;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GodWorshipAcceptanceTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000033");

    @Test
    void linkedActivationSkipsNotReadyGodWithoutError() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        policy.gates.add(FeatureGate.GOD_AUTO_ACTIVATION);
        ready(repository.run.gods.get(GodId.GOD_1));
        State linked = repository.run.gods.get(GodId.GOD_2);
        linked.unlocked = true;
        linked.level = 1L;
        linked.phase = Phase.CHARGE;
        linked.chargeProgress = 4L;
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.AUTO_WHEN_READY;
        repository.persistent.preferences.get(GodId.GOD_2).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_2).linkedGodId = GodId.GOD_1;

        GodWorshipAutomationService.TickResult result = new GodWorshipAutomationService(repository, policy).tick(PLAYER, 1_000L);

        assertEquals(1, result.activations);
        assertEquals(Phase.BUFF_ACTIVE, repository.run.gods.get(GodId.GOD_1).phase);
        assertEquals(Phase.CHARGE, linked.phase);
        assertEquals(4L, linked.chargeProgress);
    }

    @Test
    void cyclicLinksAreBoundedAndDoNotActivateWithoutFactualRoot() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        policy.gates.add(FeatureGate.GOD_AUTO_ACTIVATION);
        ready(repository.run.gods.get(GodId.GOD_1));
        ready(repository.run.gods.get(GodId.GOD_2));
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_1).linkedGodId = GodId.GOD_2;
        repository.persistent.preferences.get(GodId.GOD_2).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_2).linkedGodId = GodId.GOD_1;

        GodWorshipAutomationService.TickResult result = new GodWorshipAutomationService(repository, policy).tick(PLAYER, 1_000L);

        assertEquals(0, result.activations);
        assertEquals(Phase.CHARGE, repository.run.gods.get(GodId.GOD_1).phase);
        assertEquals(Phase.CHARGE, repository.run.gods.get(GodId.GOD_2).phase);
    }

    @Test
    void durationPurchaseDoesNotRetroactivelyExtendActiveBuffButAffectsNextActivation() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        policy.gates.add(FeatureGate.GOD_PERMANENT_BUFF_DURATION);
        repository.persistent.offerings = new BigDecimal("100");
        State alreadyActive = repository.run.gods.get(GodId.GOD_1);
        alreadyActive.unlocked = true;
        alreadyActive.level = 1L;
        alreadyActive.phase = Phase.BUFF_ACTIVE;
        alreadyActive.buffActiveUntilMillis = 20_000L;
        GodWorshipAutomationService automation = new GodWorshipAutomationService(repository, policy);

        assertTrue(automation.purchasePermanentDuration(PLAYER));
        assertEquals(1L, repository.persistent.permanentDurationLevel);
        assertEquals(20_000L, alreadyActive.buffActiveUntilMillis);

        State future = repository.run.gods.get(GodId.GOD_2);
        future.unlocked = true;
        future.level = 1L;
        future.phase = Phase.CHARGE;
        future.chargeProgress = 9L;
        GodWorshipService service = new GodWorshipService(repository,
                new ResourceService(new FakeResourceRepository()), policy);
        service.click(PLAYER, GodId.GOD_2, 30_000L);
        assertEquals(Phase.BUFF_ACTIVE, future.phase);
        assertEquals(41_000L, future.buffActiveUntilMillis);
    }

    @Test
    void masteryResetClearsRunProgressionButPreservesPersistentMetaAndPreferences() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        State state = repository.run.gods.get(GodId.GOD_1);
        state.unlocked = true;
        state.level = 8L;
        state.phase = Phase.COOLDOWN;
        state.cooldownUntilMillis = 50_000L;
        repository.persistent.offerings = new BigDecimal("42.5");
        repository.persistent.permanentDurationLevel = 7L;
        repository.persistent.preferences.get(GodId.GOD_1).autoWorshipEnabled = true;
        repository.persistent.preferences.get(GodId.GOD_1).activationMode = ActivationMode.LINKED_TO_GOD;
        repository.persistent.preferences.get(GodId.GOD_1).linkedGodId = GodId.GOD_2;
        GodWorshipService service = new GodWorshipService(repository,
                new ResourceService(new FakeResourceRepository()), policy);

        service.onMasteryReset(PLAYER, false);

        assertEquals(false, repository.run.gods.get(GodId.GOD_1).unlocked);
        assertEquals(0L, repository.run.gods.get(GodId.GOD_1).level);
        assertEquals(0, repository.persistent.offerings.compareTo(new BigDecimal("42.5")));
        assertEquals(7L, repository.persistent.permanentDurationLevel);
        assertTrue(repository.persistent.preferences.get(GodId.GOD_1).autoWorshipEnabled);
        assertEquals(ActivationMode.LINKED_TO_GOD, repository.persistent.preferences.get(GodId.GOD_1).activationMode);
        assertEquals(GodId.GOD_2, repository.persistent.preferences.get(GodId.GOD_1).linkedGodId);
    }

    @Test
    void resetPreservationHookKeepsEntireGodRunProgression() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        State state = repository.run.gods.get(GodId.GOD_4);
        state.unlocked = true;
        state.level = 5L;
        state.phase = Phase.BUFF_ACTIVE;
        state.buffActiveUntilMillis = 90_000L;
        GodWorshipService service = new GodWorshipService(repository,
                new ResourceService(new FakeResourceRepository()), policy);

        service.onMasteryReset(PLAYER, true);

        State preserved = repository.run.gods.get(GodId.GOD_4);
        assertTrue(preserved.unlocked);
        assertEquals(5L, preserved.level);
        assertEquals(Phase.BUFF_ACTIVE, preserved.phase);
        assertEquals(90_000L, preserved.buffActiveUntilMillis);
    }

    @Test
    void factualActionsPublishExactlyOneGameplayEventEach() {
        FakeGodRepository repository = new FakeGodRepository();
        FakePolicy policy = new FakePolicy();
        FakeResourceRepository balances = new FakeResourceRepository();
        balances.put("rune_dust", new BigDecimal("100"));
        balances.put("rune_shards", new BigDecimal("100"));
        GodWorshipService service = new GodWorshipService(repository, new ResourceService(balances), policy);
        RecordingSink sink = new RecordingSink();
        service.setGameplayEventSink(sink);

        service.unlock(PLAYER, GodId.GOD_1);
        service.upgrade(PLAYER, GodId.GOD_1);
        State state = repository.run.gods.get(GodId.GOD_1);
        state.chargeProgress = 9L;
        service.click(PLAYER, GodId.GOD_1, 1_000L);

        assertEquals(1, sink.count(GameplayEventType.GOD_UNLOCK));
        assertEquals(1, sink.count(GameplayEventType.GOD_UPGRADE));
        assertEquals(1, sink.count(GameplayEventType.GOD_BUFF_ACTIVATE));
    }

    private static void ready(State state) {
        state.unlocked = true;
        state.level = 1L;
        state.phase = Phase.CHARGE;
        state.chargeProgress = 10L;
    }

    private static final class RecordingSink implements GameplayEventSink {
        private final List<GameplayEvent> events = new ArrayList<>();
        @Override public void confirmedDamage(UUID playerUuid, GameplayDamageSource source, BigDecimal amount) { }
        @Override public void earnedResource(UUID playerUuid, String resource, BigDecimal amount) { }
        @Override public void factualEvent(GameplayEvent event) { events.add(event); }
        int count(GameplayEventType type) {
            int count = 0;
            for (GameplayEvent event : events) if (event.type == type) count++;
            return count;
        }
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

    private static final class FakePolicy implements GodWorshipAutomationService.Policy {
        private final Set<FeatureGate> gates = EnumSet.noneOf(FeatureGate.class);
        @Override public boolean enabled(GodId god) { return true; }
        @Override public String unlockResource(GodId god) { return "rune_dust"; }
        @Override public String upgradeResource(GodId god) { return "rune_shards"; }
        @Override public EffectResolver.Stat primaryChannel(GodId god) { return EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER; }
        @Override public EffectResolver.Stat secondaryChannel(GodId god) { return null; }
        @Override public BigDecimal unlockCost(GodId god) { return BigDecimal.ONE; }
        @Override public BigDecimal upgradeCost(GodId god, long currentLevel) { return BigDecimal.ONE; }
        @Override public long chargeRequired(GodId god) { return 10L; }
        @Override public long chargePerClick(GodId god) { return 1L; }
        @Override public BigDecimal passiveChargePerSecond(GodId god) { return BigDecimal.ZERO; }
        @Override public long buffDurationMillis(GodId god) { return 10_000L; }
        @Override public long cooldownDurationMillis(GodId god) { return 5_000L; }
        @Override public long cooldownReductionPerClickMillis(GodId god) { return 1_000L; }
        @Override public int clickRateLimitPerSecond(GodId god) { return 25; }
        @Override public long permanentDurationMillis(long permanentDurationLevel) { return permanentDurationLevel * 1_000L; }
        @Override public boolean featureEnabled(FeatureGate gate) { return gates.contains(gate); }
        @Override public BigDecimal offeringGenerationPerSecond() { return BigDecimal.ONE; }
        @Override public long autoWorshipIntervalMillis() { return 1_000L; }
        @Override public BigDecimal autoWorshipCooldownCost() { return BigDecimal.ONE; }
        @Override public long autoWorshipCooldownReductionMillis() { return 1_000L; }
        @Override public BigDecimal autoWorshipChargeCost() { return BigDecimal.ONE; }
        @Override public long autoWorshipChargeGain() { return 1L; }
        @Override public BigDecimal durationUpgradeCost(long currentLevel) { return BigDecimal.ONE; }
    }
}
