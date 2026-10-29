package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.gameplay.GameplayDamageSource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class PrestigeBranchLifecycleTest {
    @Test
    public void poisonUsesGenericRanksEffectsAndResetLifecycle() {
        verifyDamageLifecycle(PrestigeBranch.POISON, GameplayDamageSource.POISON,
                EffectResolver.Stat.POISON_DAMAGE_MULTIPLIER, 401);
    }

    @Test
    public void allDamageUsesGenericRanksEffectsAndResetLifecycle() {
        verifyDamageLifecycle(PrestigeBranch.ALL_DAMAGE, GameplayDamageSource.FIRE,
                EffectResolver.Stat.ALL_DAMAGE_MULTIPLIER, 402);
    }

    @Test
    public void coinsUsesGenericRanksEffectsAndResetLifecycle() {
        Fixture fixture = fixture(PrestigeBranch.COINS, 403);
        PrestigeState.RunState run = fixture.prestige.snapshot(fixture.player).activeRun;
        fixture.router.earnedResource(fixture.player, "coins", new BigDecimal("100"));
        verifyReachedRankAndEffect(fixture, EffectResolver.Stat.COIN_REWARD_MULTIPLIER, "COINS_I");
        verifyReset(fixture, run.runId, PrestigeBranch.COINS, "COINS_I");
    }

    @Test
    public void runeResourcesUsesGenericRanksEffectsAndResetLifecycle() {
        Fixture fixture = fixture(PrestigeBranch.RUNE_RESOURCES, 404);
        PrestigeState.RunState run = fixture.prestige.snapshot(fixture.player).activeRun;
        fixture.router.earnedResource(fixture.player, "rune_cores", new BigDecimal("100"));
        verifyReachedRankAndEffect(fixture, EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER, "RUNE_RESOURCES_I");
        verifyReset(fixture, run.runId, PrestigeBranch.RUNE_RESOURCES, "RUNE_RESOURCES_I");
    }

    @Test
    public void historicalBestRemainsIndependentPerBranch() {
        MemoryRepository repository = new MemoryRepository();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService prestige = new PrestigeService(repository, registry);
        PrestigeProgressRouter router = new PrestigeProgressRouter(prestige, registry);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000405");

        PrestigeState.RunState poisonRun = prestige.startRun(player, PrestigeBranch.POISON);
        router.confirmedDamage(player, GameplayDamageSource.POISON, new BigDecimal("600"));
        assertEquals(PrestigeResetService.ResetResult.APPLIED, prestige.reset(player, poisonRun.runId));

        PrestigeState.RunState coinRun = prestige.startRun(player, PrestigeBranch.COINS);
        router.earnedResource(player, "coins", new BigDecimal("200"));
        assertEquals(PrestigeResetService.ResetResult.APPLIED, prestige.reset(player, coinRun.runId));

        PrestigeState state = prestige.snapshot(player);
        assertEquals(0, state.branch(PrestigeBranch.POISON).bestSavedProgress.compareTo(new BigDecimal("600")));
        assertEquals(0, state.branch(PrestigeBranch.COINS).bestSavedProgress.compareTo(new BigDecimal("200")));
        assertEquals(0, state.branch(PrestigeBranch.CLICK).bestSavedProgress.signum());
    }

    private static void verifyDamageLifecycle(PrestigeBranch branch, GameplayDamageSource source,
                                              EffectResolver.Stat effectStat, int suffix) {
        Fixture fixture = fixture(branch, suffix);
        PrestigeState.RunState run = fixture.prestige.snapshot(fixture.player).activeRun;
        fixture.router.confirmedDamage(fixture.player, source, new BigDecimal("100"));
        verifyReachedRankAndEffect(fixture, effectStat, branch.name() + "_I");
        verifyReset(fixture, run.runId, branch, branch.name() + "_I");
    }

    private static void verifyReachedRankAndEffect(Fixture fixture, EffectResolver.Stat effectStat, String rankId) {
        PrestigeState state = fixture.prestige.snapshot(fixture.player);
        assertEquals(new BigDecimal("100"), state.activeRun.branchProgress);
        assertEquals(1, state.activeRun.reachedRanks.size());
        assertEquals(rankId, state.activeRun.reachedRanks.get(0));
        assertEquals(0, fixture.effects.multiplier(fixture.player, effectStat).compareTo(new BigDecimal("1.05")));
    }

    private static void verifyReset(Fixture fixture, String runId, PrestigeBranch branch, String rankId) {
        assertEquals(PrestigeResetService.ResetResult.APPLIED, fixture.prestige.reset(fixture.player, runId));
        PrestigeState state = fixture.prestige.snapshot(fixture.player);
        assertNull(state.activeRun);
        assertEquals(0, state.branch(branch).bestSavedProgress.compareTo(new BigDecimal("100")));
        assertEquals(Integer.valueOf(1), state.branch(branch).rankMetaLevels.get(rankId));
        assertEquals(PrestigeResetService.ResetResult.DUPLICATE, fixture.prestige.reset(fixture.player, runId));
    }

    private static Fixture fixture(PrestigeBranch branch, int suffix) {
        MemoryRepository repository = new MemoryRepository();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService prestige = new PrestigeService(repository, registry);
        EffectResolver effects = new EffectResolver();
        effects.register(prestige);
        UUID player = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", suffix));
        prestige.startRun(player, branch);
        return new Fixture(player, prestige, effects, new PrestigeProgressRouter(prestige, registry));
    }

    private static final class Fixture {
        final UUID player;
        final PrestigeService prestige;
        final EffectResolver effects;
        final PrestigeProgressRouter router;
        Fixture(UUID player, PrestigeService prestige, EffectResolver effects, PrestigeProgressRouter router) {
            this.player = player;
            this.prestige = prestige;
            this.effects = effects;
            this.router = router;
        }
    }

    private static final class MemoryRepository implements PrestigeRepository {
        private final Map<UUID, PrestigeState> states = new HashMap<>();
        @Override public PrestigeState load(UUID playerUuid) {
            PrestigeState state = states.get(playerUuid);
            return state == null ? new PrestigeState() : state.copy();
        }
        @Override public void save(UUID playerUuid, PrestigeState state) { states.put(playerUuid, state.copy()); }
        @Override public ResetCommitResult commitReset(UUID playerUuid, String expectedRunId, PrestigeState state) {
            PrestigeState current = load(playerUuid);
            if (current.activeRun == null) {
                return expectedRunId != null && expectedRunId.equals(current.lastResetRunId)
                        ? ResetCommitResult.DUPLICATE : ResetCommitResult.STALE;
            }
            if (!current.activeRun.runId.equals(expectedRunId)) return ResetCommitResult.STALE;
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
