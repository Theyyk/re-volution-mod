package com.example.revolution.prestige;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.resource.repository.ResourceRepository;
import com.example.revolution.resource.service.ResourceService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public final class PrestigeResourceBoundaryTest {
    @Test
    public void coinsCountOnlyConfirmedEarningsAndUseActualGrantedAmount() {
        Fixture fixture = fixture(PrestigeBranch.COINS, 301);

        assertTrue(fixture.resources.grantPlayerResource(fixture.player, "coins", 40));
        assertProgress(fixture, "0");

        assertTrue(fixture.resources.grantEarnedPlayerResource(fixture.player, "coins", 100));
        assertProgress(fixture, "100");
        assertBalance(fixture, "coins", "140");
        assertEquals(0, fixture.effects.multiplier(fixture.player, EffectResolver.Stat.COIN_REWARD_MULTIPLIER)
                .compareTo(new BigDecimal("1.05")));

        assertTrue(fixture.resources.grantEarnedPlayerResource(fixture.player, "coins", 100));
        assertProgress(fixture, "205");
        assertBalance(fixture, "coins", "245");

        assertTrue(fixture.resources.spendPlayerResource(fixture.player, "coins", 45));
        assertProgress(fixture, "205");
        assertBalance(fixture, "coins", "200");

        assertTrue(fixture.resources.addPlayerResourceValue(fixture.player, "coins", new BigDecimal("500")));
        assertProgress(fixture, "205");
        assertBalance(fixture, "coins", "700");
    }

    @Test
    public void runeRewardEffectFeedsBackThroughTheSameEarnedBoundary() {
        Fixture fixture = fixture(PrestigeBranch.RUNE_RESOURCES, 302);

        assertTrue(fixture.resources.grantEarnedPlayerResource(fixture.player, "rune_dust", 100));
        assertProgress(fixture, "100");
        assertEquals(0, fixture.effects.multiplier(fixture.player, EffectResolver.Stat.RUNE_RESOURCE_REWARD_MULTIPLIER)
                .compareTo(new BigDecimal("1.05")));

        assertTrue(fixture.resources.grantEarnedPlayerResource(fixture.player, "rune_shards", 20));
        assertProgress(fixture, "121");
        assertBalance(fixture, "rune_shards", "21");

        assertTrue(fixture.resources.grantEarnedPlayerResource(fixture.player, "crystals", 50));
        assertProgress(fixture, "121");
    }

    private static Fixture fixture(PrestigeBranch branch, int suffix) {
        MemoryPrestigeRepository prestigeRepository = new MemoryPrestigeRepository();
        PrestigeBranchRegistry registry = new PrestigeBranchRegistry();
        PrestigeService prestige = new PrestigeService(prestigeRepository, registry);
        EffectResolver effects = new EffectResolver();
        effects.register(prestige);
        MemoryResourceRepository resourceRepository = new MemoryResourceRepository();
        ResourceService resources = new ResourceService(resourceRepository, effects);
        resources.setGameplayEventSink(new PrestigeProgressRouter(prestige, registry));
        UUID player = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", suffix));
        prestige.startRun(player, branch);
        return new Fixture(player, prestige, effects, resources);
    }

    private static void assertProgress(Fixture fixture, String expected) {
        assertEquals(0, fixture.prestige.snapshot(fixture.player).activeRun.branchProgress.compareTo(new BigDecimal(expected)));
    }

    private static void assertBalance(Fixture fixture, String resource, String expected) {
        assertEquals(0, fixture.resources.getPlayerResourceValue(fixture.player, resource).compareTo(new BigDecimal(expected)));
    }

    private static final class Fixture {
        final UUID player;
        final PrestigeService prestige;
        final EffectResolver effects;
        final ResourceService resources;
        Fixture(UUID player, PrestigeService prestige, EffectResolver effects, ResourceService resources) {
            this.player = player;
            this.prestige = prestige;
            this.effects = effects;
            this.resources = resources;
        }
    }

    private static final class MemoryResourceRepository implements ResourceRepository {
        private final Map<String, BigDecimal> balances = new HashMap<>();
        @Override public void purgeDeprecatedLightnings() { }
        @Override public void purgeDeprecatedLightnings(UUID uuid) { }
        @Override public BigDecimal getPlayerResourceValue(UUID uuid, String resource) {
            return balances.getOrDefault(uuid + ":" + resource, BigDecimal.ZERO);
        }
        @Override public void writePlayerResource(UUID uuid, String canonical, BigDecimal value) {
            balances.put(uuid + ":" + canonical, value);
        }
    }

    private static final class MemoryPrestigeRepository implements PrestigeRepository {
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
