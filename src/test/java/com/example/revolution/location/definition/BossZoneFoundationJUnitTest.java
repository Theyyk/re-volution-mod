package com.example.revolution.location.definition;

import com.example.revolution.mob.repository.InMemoryMobProgressionRepository;
import com.example.revolution.mob.service.MobProgressionService;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BossZoneFoundationJUnitTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000001311");

    @Test void legacyConstructorsRemainNormalMobZones() {
        int internalId = MobZoneIds.encode(1, 1);
        LocationDefinition legacy = new LocationDefinition(internalId, "legacy", 0.0D, 0.0D, 5.0D);
        assertEquals(LocationKind.MOB_ZONE, legacy.getKind());
        assertTrue(legacy.isMobStageZone());
        assertFalse(legacy.isBossZone());
    }

    @Test void bossZoneIsExplicitStageLevelIdentity() {
        int internalId = MobZoneIds.encode(1, 9);
        LocationDefinition boss = new LocationDefinition(internalId, "boss", 0.0D, 0.0D, 5.0D, LocationKind.BOSS_ZONE);
        assertEquals(LocationKind.BOSS_ZONE, boss.getKind());
        assertTrue(boss.isMobStageZone());
        assertTrue(boss.isBossZone());
    }

    @Test void kindMustMatchLocationIdNamespace() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationDefinition(1, "world", 0.0D, 0.0D, 5.0D, LocationKind.BOSS_ZONE));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationDefinition(MobZoneIds.encode(1, 1), "zone", 0.0D, 0.0D, 5.0D, LocationKind.WORLD));
    }

    @Test void multipleBossZonesMakeLocationIncomplete() {
        MobProgressionService progression = new MobProgressionService(new InMemoryMobProgressionRepository());
        LocationDefinition normal = zone(1, 1, LocationKind.MOB_ZONE);
        LocationDefinition bossA = zone(1, 2, LocationKind.BOSS_ZONE);
        LocationDefinition bossB = zone(1, 3, LocationKind.BOSS_ZONE);
        LocationDefinition next = zone(2, 1, LocationKind.MOB_ZONE);
        progression.reloadConfiguration(
                Arrays.asList(normal, bossA, bossB, next),
                Arrays.asList(spawn(1, 1), spawn(1, 2), spawn(1, 3), spawn(2, 1)));

        assertFalse(progression.hasValidBossLayout(1));
        assertFalse(progression.isFinalStage(MobZoneIds.encode(1, 3)));
        assertNotNull(progression.unlockNext(PLAYER, MobZoneIds.encode(1, 1)));
        assertNotNull(progression.unlockNext(PLAYER, MobZoneIds.encode(1, 2)));
        assertNull(progression.unlockNext(PLAYER, MobZoneIds.encode(1, 3)));
        assertFalse(progression.isUnlocked(PLAYER, MobZoneIds.encode(2, 1)));
    }

    private static LocationDefinition zone(int location, int slot, LocationKind kind) {
        return new LocationDefinition(MobZoneIds.encode(location, slot), "zone", slot * 10.0D, location * 100.0D, 5.0D, kind);
    }

    private static StoredMobSpawn spawn(int location, int slot) {
        return new StoredMobSpawn(MobZoneIds.encode(location, slot), slot, "mob-" + location + "-" + slot,
                slot * 10.0D, 64.0D, location * 100.0D, 0.0F, 0.0F,
                SpawnMode.FIXED_POINT, MovementMode.STATIONARY, SpawnRealmScope.PLAYER_REALMS, true);
    }
}
