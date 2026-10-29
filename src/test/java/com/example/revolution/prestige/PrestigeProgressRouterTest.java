package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.resource.definition.ResourceDefinitions;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class PrestigeProgressRouterTest {
    @Test
    public void clickAndPoisonUseTheirOwnConfirmedDamageSources() {
        Fixture click = fixture(PrestigeBranch.CLICK, 201);
        click.router.confirmedDamage(click.player, GameplayDamageSource.CLICK, new BigDecimal("75"));
        click.router.confirmedDamage(click.player, GameplayDamageSource.POISON, new BigDecimal("50"));
        assertProgress(click, "75");

        Fixture poison = fixture(PrestigeBranch.POISON, 202);
        poison.router.confirmedDamage(poison.player, GameplayDamageSource.CLICK, new BigDecimal("75"));
        poison.router.confirmedDamage(poison.player, GameplayDamageSource.POISON, new BigDecimal("50"));
        assertProgress(poison, "50");
    }

    @Test
    public void allDamageCountsEachConfirmedDamageEventExactlyOnce() {
        Fixture all = fixture(PrestigeBranch.ALL_DAMAGE, 203);
        all.router.confirmedDamage(all.player, GameplayDamageSource.CLICK, new BigDecimal("80"));
        all.router.confirmedDamage(all.player, GameplayDamageSource.POISON, new BigDecimal("20"));
        assertProgress(all, "100");
    }

    @Test
    public void allDamageAutomaticallyAcceptsFutureDamageSources() {
        Fixture all = fixture(PrestigeBranch.ALL_DAMAGE, 207);
        all.router.confirmedDamage(all.player, GameplayDamageSource.FIRE, new BigDecimal("30"));
        all.router.confirmedDamage(all.player, GameplayDamageSource.PET, new BigDecimal("15"));
        assertProgress(all, "45");
    }

    @Test
    public void coinsProgressFromEarnedEventNotFromBalanceState() {
        Fixture coins = fixture(PrestigeBranch.COINS, 204);
        coins.router.earnedResource(coins.player, "coins", new BigDecimal("100"));
        coins.router.earnedResource(coins.player, "crystals", new BigDecimal("999"));
        assertProgress(coins, "100");
    }

    @Test
    public void runeResourcesUseDataDrivenWeightsAndIgnoreOtherResources() {
        Fixture runes = fixture(PrestigeBranch.RUNE_RESOURCES, 205);
        runes.router.earnedResource(runes.player, ResourceDefinitions.RUNE_DUST, new BigDecimal("10"));
        runes.router.earnedResource(runes.player, ResourceDefinitions.RUNE_SHARDS, new BigDecimal("20"));
        runes.router.earnedResource(runes.player, ResourceDefinitions.RUNE_CORES, new BigDecimal("30"));
        runes.router.earnedResource(runes.player, "crystals", new BigDecimal("1000"));
        assertProgress(runes, "60");
    }

    @Test
    public void inactiveBranchesRemainUntouched() {
        Fixture click = fixture(PrestigeBranch.CLICK, 206);
        click.router.earnedResource(click.player, "coins", new BigDecimal("500"));
        click.router.confirmedDamage(click.player, GameplayDamageSource.POISON, new BigDecimal("500"));
        assertProgress(click, "0");
        assertEquals(0, click.prestige.snapshot(click.player).branch(PrestigeBranch.COINS).bestSavedProgress.signum());
        assertEquals(0, click.prestige.snapshot(click.player).branch(PrestigeBranch.POISON).bestSavedProgress.signum());
    }

    private static Fixture fixture(PrestigeBranch branch, int suffix) {
        MemoryRepository repository = new MemoryRepository();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService prestige = new PrestigeService(repository, registry);
        UUID player = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", suffix));
        prestige.startRun(player, branch);
        return new Fixture(player, prestige, new PrestigeProgressRouter(prestige, registry));
    }

    private static void assertProgress(Fixture fixture, String expected) {
        assertEquals(0, fixture.prestige.snapshot(fixture.player).activeRun.branchProgress.compareTo(new BigDecimal(expected)));
    }

    private static final class Fixture {
        final UUID player;
        final PrestigeService prestige;
        final PrestigeProgressRouter router;
        Fixture(UUID player, PrestigeService prestige, PrestigeProgressRouter router) {
            this.player = player;
            this.prestige = prestige;
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
            if (current.activeRun == null || !current.activeRun.runId.equals(expectedRunId)) return ResetCommitResult.STALE;
            states.put(playerUuid, state.copy());
            return ResetCommitResult.APPLIED;
        }
    }
}
