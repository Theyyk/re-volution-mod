package com.example.revolution.achievement;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Standalone contract regression for persistent atomic claim routing and stable-id set filters. */
public final class AchievementAtomicClaimTest {
    public static void main(String[] args) {
        AchievementStage first = new AchievementStage(1, BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 7L);
        AchievementDefinition definition = new AchievementDefinition(
                "atomic_claim", "Atomic Claim", AchievementCategory.DAMAGE,
                GameplayEventType.TOTAL_DAMAGE, Collections.<String, String>emptyMap(), false,
                Collections.singletonList(first));
        AchievementCatalog catalog = new AchievementCatalog(Collections.singletonList(definition));
        AtomicRepository repository = new AtomicRepository();
        FailingFallbackRewards fallback = new FailingFallbackRewards();
        AchievementService service = new AchievementService(repository, catalog, fallback);
        UUID player = UUID.randomUUID();

        service.confirmedDamage(player, GameplayDamageSource.CLICK, BigDecimal.TEN);
        check(service.claim(player, "atomic_claim", 1) == AchievementService.ClaimResult.APPLIED,
                "atomic claim applies");
        check(repository.commitCount == 1, "production atomic boundary used exactly once");
        check(fallback.calls == 0, "fallback reward gateway is bypassed when atomic store exists");
        check(service.snapshot(player).achievementPoints == 7L, "points committed with claim");
        check(service.claim(player, "atomic_claim", 1) == AchievementService.ClaimResult.OUT_OF_ORDER,
                "replayed stage cannot trigger another atomic commit");
        check(repository.commitCount == 1, "replay does not recommit");

        AchievementService reloaded = new AchievementService(repository, catalog, fallback);
        check(reloaded.snapshot(player).claimedStage("atomic_claim") == 1, "claimed stage survives reload");
        check(reloaded.snapshot(player).achievementPoints == 7L, "points survive reload");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("mobId", "oneof:boss_alpha;boss_beta");
        AchievementDefinition boss = new AchievementDefinition(
                "boss_kills", "Boss Kills", AchievementCategory.KILLS,
                GameplayEventType.MOB_KILL_SPECIFIC, filters, false,
                Arrays.asList(first));
        check(boss.matches(Collections.singletonMap("mobId", "boss_alpha")), "first stable boss id matches");
        check(boss.matches(Collections.singletonMap("mobId", "boss_beta")), "second stable boss id matches");
        check(!boss.matches(Collections.singletonMap("mobId", "ordinary_mob")), "ordinary stable mob id does not match boss filter");

        com.example.revolution.persistence.MongoAchievementRepositoryTest.main(new String[0]);
        AchievementBalanceTest.main(new String[0]);
        com.example.revolution.network.AchievementNetworkPacketTest.main(new String[0]);
        System.out.println("Achievement atomic claim and stable boss-id filter regressions passed");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class AtomicRepository implements AchievementRepository, AchievementAtomicClaimStore {
        private final Map<UUID, AchievementPlayerState> states = new LinkedHashMap<>();
        int commitCount;

        @Override public AchievementPlayerState load(UUID playerUuid) {
            AchievementPlayerState state = states.get(playerUuid);
            return state == null ? new AchievementPlayerState() : state.copy();
        }

        @Override public void save(UUID playerUuid, AchievementPlayerState state) {
            states.put(playerUuid, state.copy());
        }

        @Override public boolean commit(UUID playerUuid, String achievementId, int expectedPriorClaimedStage,
                                        AchievementStage stage, AchievementPlayerState nextState) {
            AchievementPlayerState current = load(playerUuid);
            if (current.claimedStage(achievementId) != expectedPriorClaimedStage) return false;
            commitCount++;
            states.put(playerUuid, nextState.copy());
            return true;
        }
    }

    private static final class FailingFallbackRewards implements AchievementRewardGateway {
        int calls;
        @Override public boolean grantStageRewards(UUID playerUuid, String claimKey, AchievementStage stage) {
            calls++;
            return false;
        }
    }
}
