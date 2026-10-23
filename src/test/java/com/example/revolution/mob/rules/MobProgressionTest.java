package com.example.revolution.mob.rules;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.mob.repository.MobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MobProgressionTest {
    public static void main(String[] args) {
        pureOrderingAndGatingRules();
        adjacentStageNavigationRequiresUnlockAndStaysInWorldLocation();
        System.out.println("v1.3.9 mob progression ordering/gating/navigation regression checks passed");
    }

    private static void pureOrderingAndGatingRules() {
        List<Integer> configured = MobProgressionRules.normalizeConfiguredSlots(
                Arrays.asList(9, 2, 5, 5, -1, 100, null));
        check(configured.equals(Arrays.asList(2, 5, 9)), "Configured slots must be unique, valid and sorted");

        Set<Integer> unlocked = new HashSet<>();
        check(MobProgressionRules.isUnlocked(2, configured, unlocked), "First configured stage must be open by default");
        check(!MobProgressionRules.isUnlocked(5, configured, unlocked), "Second stage must start locked");
        check(!MobProgressionRules.isUnlocked(9, configured, unlocked), "Later stage must start locked");
        check(!MobProgressionRules.isUnlocked(3, configured, unlocked), "Unconfigured slot must not be treated as a stage by pure rules");

        unlocked.add(5);
        check(MobProgressionRules.isUnlocked(5, configured, unlocked), "Explicit unlock must open the stage");
        check(!MobProgressionRules.isUnlocked(9, configured, unlocked), "Unlocking one stage must not skip the next stage");

        check(Integer.valueOf(5).equals(MobProgressionRules.nextConfiguredSlot(2, configured)), "2 must advance to 5");
        check(Integer.valueOf(9).equals(MobProgressionRules.nextConfiguredSlot(5, configured)), "5 must advance to 9");
        check(MobProgressionRules.nextConfiguredSlot(9, configured) == null, "Last stage must have no next stage");
        check(MobProgressionRules.nextConfiguredSlot(3, configured).equals(5), "Ordering must tolerate non-contiguous configured IDs");

        check(MobProgressionRules.previousConfiguredSlot(2, configured) == null, "First stage must have no prerequisite slot");
        check(Integer.valueOf(2).equals(MobProgressionRules.previousConfiguredSlot(5, configured)), "5 must require 2");
        check(Integer.valueOf(5).equals(MobProgressionRules.previousConfiguredSlot(9, configured)), "9 must require 5");
        check(!MobProgressionRules.isUnlocked(1, Collections.<Integer>emptyList(), Collections.<Integer>emptySet()),
                "Empty pure-rule chain must not invent a stage");
    }

    private static void adjacentStageNavigationRequiresUnlockAndStaysInWorldLocation() {
        int worldLocationId = 17;
        int otherWorldLocationId = 18;
        int slot2 = MobZoneIds.encode(worldLocationId, 2);
        int slot5 = MobZoneIds.encode(worldLocationId, 5);
        int slot9 = MobZoneIds.encode(worldLocationId, 9);
        int otherSlot2 = MobZoneIds.encode(otherWorldLocationId, 2);

        FakeProgressionRepository repository = new FakeProgressionRepository();
        MobProgressionService service = new MobProgressionService(repository);
        service.reloadConfiguration(
                Arrays.asList(location(slot2), location(slot5), location(slot9), location(otherSlot2)),
                Arrays.asList(spawn(slot2, 1, "A"), spawn(slot5, 2, "B"), spawn(slot9, 3, "C"), spawn(otherSlot2, 4, "D")));

        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000139");
        check(service.nextUnlockedInternalLocationId(player, slot2) == null,
                "Locked next stage must not be exposed as a transition target");
        check(service.previousUnlockedInternalLocationId(player, slot2) == null,
                "First configured stage must have no previous transition target");

        check(service.unlockNext(player, slot2) != null, "Winning stage 2 must unlock stage 5");
        check(Integer.valueOf(slot5).equals(service.nextUnlockedInternalLocationId(player, slot2)),
                "Unlocked next configured stage must be resolved by internal Location ID");
        check(Integer.valueOf(slot2).equals(service.previousUnlockedInternalLocationId(player, slot5)),
                "Previous configured stage must remain accessible");
        check(service.nextUnlockedInternalLocationId(player, slot5) == null,
                "Stage 9 must remain unavailable until separately unlocked");

        check(service.unlockNext(player, slot5) != null, "Winning stage 5 must unlock stage 9");
        check(Integer.valueOf(slot9).equals(service.nextUnlockedInternalLocationId(player, slot5)),
                "Non-contiguous slot order must resolve the next unlocked stage");
        check(service.nextUnlockedInternalLocationId(player, slot9) == null,
                "Last stage must have no forward transition target");
        check(service.nextUnlockedInternalLocationId(player, otherSlot2) == null,
                "Transition resolution must never jump into a different world Location chain");
        check(service.nextUnlockedInternalLocationId(player, 17) == null,
                "Legacy/non-zone Location IDs must not become stage transition targets");
        check(service.nextUnlockedInternalLocationId(null, slot2) == null,
                "Transition navigation requires a player identity");
    }

    private static LocationDefinition location(int internalLocationId) {
        return new LocationDefinition(internalLocationId, "Zone " + internalLocationId, 0.0D, 0.0D, 10.0D);
    }

    private static StoredMobSpawn spawn(int internalLocationId, int spawnId, String mobName) {
        return new StoredMobSpawn(
                internalLocationId, spawnId, mobName,
                0.0D, 64.0D, 0.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, true);
    }

    private static final class FakeProgressionRepository implements MobProgressionRepository {
        private final Map<String, Set<Integer>> unlocked = new HashMap<>();

        @Override
        public Set<Integer> getUnlockedSlots(UUID playerUuid, int worldLocationId) {
            Set<Integer> values = unlocked.get(key(playerUuid, worldLocationId));
            return values == null ? Collections.<Integer>emptySet() : new HashSet<>(values);
        }

        @Override
        public boolean unlockSlot(UUID playerUuid, int worldLocationId, int mobSlotId) {
            String key = key(playerUuid, worldLocationId);
            Set<Integer> values = unlocked.computeIfAbsent(key, ignored -> new HashSet<Integer>());
            return values.add(mobSlotId);
        }

        private static String key(UUID playerUuid, int worldLocationId) {
            return playerUuid.toString() + ":" + worldLocationId;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
