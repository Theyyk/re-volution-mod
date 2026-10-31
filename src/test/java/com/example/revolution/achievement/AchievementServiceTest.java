package com.example.revolution.achievement;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.gameplay.GameplayEvent;
import com.example.revolution.gameplay.GameplayEventType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Standalone regression runner: no Minecraft/Mongo startup. */
public final class AchievementServiceTest {
    public static void main(String[] args) {
        InMemoryRepository repository = new InMemoryRepository();
        RecordingRewards rewards = new RecordingRewards();
        AchievementCatalog catalog = new AchievementCatalog(Arrays.asList(
                definition("damage_total", GameplayEventType.TOTAL_DAMAGE, false, null, 100, 500, 1000),
                definition("damage_click", GameplayEventType.DAMAGE_BY_CHANNEL, false,
                        Collections.singletonMap("channel", "CLICK"), 100, 500),
                definition("mastery", GameplayEventType.MASTERY_BRANCH_SELECTED, false, null, 1, 3, 5),
                definition("pets", GameplayEventType.PET_PURCHASE_SPECIFIC, false, null, 1, 2, 3),
                definition("locations", GameplayEventType.LOCATION_REACHED, false, null, 1, 2, 3),
                definition("hidden", GameplayEventType.RUNE_AWAKEN_ANY, true, null, 1, 5)
        ));
        AchievementService service = new AchievementService(repository, catalog, rewards);
        List<GameplayEvent> lifecycle = new ArrayList<>();
        service.setLifecycleEventSink(new com.example.revolution.gameplay.GameplayEventSink() {
            @Override public void factualEvent(GameplayEvent event) { lifecycle.add(event); }
        });
        UUID player = UUID.randomUUID();

        service.confirmedDamage(player, GameplayDamageSource.CLICK, new BigDecimal("1500"));
        check(service.snapshot(player).progress("damage_total").equals(new BigDecimal("1500")), "total damage counted once");
        check(service.snapshot(player).progress("damage_click").equals(new BigDecimal("1500")), "channel damage counted once");
        check(service.highestCompletedStage(player, "damage_total") == 3, "multiple stages factual-complete before claim");
        check(service.snapshot(player).claimedStage("damage_total") == 0, "completion never auto-claims");
        check(service.snapshot(player).achievementPoints == 0L, "points are claim-only");

        check(service.claim(player, "damage_total", 2) == AchievementService.ClaimResult.OUT_OF_ORDER, "cannot skip stage I");
        check(service.claim(player, "damage_total", 1) == AchievementService.ClaimResult.APPLIED, "claim I applies");
        check(service.snapshot(player).achievementPoints == 10L, "claim awards stage points");
        check(service.claim(player, "damage_total", 1) == AchievementService.ClaimResult.OUT_OF_ORDER, "replayed Claim I cannot advance II");
        check(service.claim(player, "damage_total", 2) == AchievementService.ClaimResult.APPLIED, "claim II applies after I");
        check(service.claim(player, "damage_total", 3) == AchievementService.ClaimResult.APPLIED, "claim III applies after II");
        check(rewards.claimKeys.equals(Arrays.asList("damage_total:1", "damage_total:2", "damage_total:3")), "one reward grant per stage");

        mastery(service, player, "CLICK"); mastery(service, player, "CLICK"); mastery(service, player, "POISON");
        check(service.snapshot(player).progress("mastery").compareTo(new BigDecimal("2")) == 0, "mastery lifetime unique set");
        pet(service, player, "pet_a"); pet(service, player, "pet_a"); pet(service, player, "pet_b");
        check(service.snapshot(player).progress("pets").compareTo(new BigDecimal("2")) == 0, "pet lifetime unique set");
        location(service, player, "101"); location(service, player, "101"); location(service, player, "102");
        check(service.snapshot(player).progress("locations").compareTo(new BigDecimal("2")) == 0, "location lifetime unique set");

        service.factualEvent(new GameplayEvent(player, GameplayEventType.RUNE_AWAKEN_ANY));
        check(service.snapshot(player).revealedHiddenAchievements.contains("hidden"), "hidden reveals on factual completion");
        service.flush(player);
        service.unload(player);
        check(service.snapshot(player).revealedHiddenAchievements.contains("hidden"), "hidden reveal persists after reload");
        check(service.snapshot(player).progress("damage_total").compareTo(new BigDecimal("1500")) == 0, "progress persists after unload/reload");

        long completedEvents = lifecycle.stream().filter(e -> e.type == GameplayEventType.ACHIEVEMENT_STAGE_COMPLETED).count();
        long claimedEvents = lifecycle.stream().filter(e -> e.type == GameplayEventType.ACHIEVEMENT_STAGE_CLAIMED).count();
        long revealEvents = lifecycle.stream().filter(e -> e.type == GameplayEventType.ACHIEVEMENT_REVEALED).count();
        check(completedEvents >= 4L, "first crossings publish completion facts");
        check(claimedEvents == 3L, "successful claims publish exactly one claim fact");
        check(revealEvents == 1L, "hidden reveal fact is one-shot");
        System.out.println("Achievement permanent progression, sequential claim and lifecycle regressions passed");
    }

    private static AchievementDefinition definition(String id, GameplayEventType type, boolean hidden,
                                                    Map<String, String> filters, int... targets) {
        List<AchievementStage> stages = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) {
            stages.add(new AchievementStage(i + 1, BigDecimal.valueOf(targets[i]),
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, (i + 1) * 10L));
        }
        return new AchievementDefinition(id, id, AchievementCategory.DAMAGE, type,
                filters == null ? Collections.<String, String>emptyMap() : filters, hidden, stages);
    }

    private static void mastery(AchievementService service, UUID player, String branch) {
        service.factualEvent(new GameplayEvent(player, GameplayEventType.MASTERY_BRANCH_SELECTED, BigDecimal.ONE,
                Collections.singletonMap("branch", branch), "test"));
    }
    private static void pet(AchievementService service, UUID player, String pet) {
        service.factualEvent(new GameplayEvent(player, GameplayEventType.PET_PURCHASE_SPECIFIC, BigDecimal.ONE,
                Collections.singletonMap("petId", pet), "test"));
    }
    private static void location(AchievementService service, UUID player, String location) {
        service.factualEvent(new GameplayEvent(player, GameplayEventType.LOCATION_REACHED, BigDecimal.ONE,
                Collections.singletonMap("locationId", location), "test"));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private static final class InMemoryRepository implements AchievementRepository {
        private final Map<UUID, AchievementPlayerState> states = new HashMap<>();
        @Override public AchievementPlayerState load(UUID playerUuid) {
            AchievementPlayerState state = states.get(playerUuid); return state == null ? new AchievementPlayerState() : state.copy();
        }
        @Override public void save(UUID playerUuid, AchievementPlayerState state) { states.put(playerUuid, state.copy()); }
    }
    private static final class RecordingRewards implements AchievementRewardGateway {
        final List<String> claimKeys = new ArrayList<>();
        @Override public boolean grantStageRewards(UUID playerUuid, String claimKey, AchievementStage stage) {
            if (claimKeys.contains(claimKey)) return false;
            claimKeys.add(claimKey); return true;
        }
    }
}
