package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationGenerationRepository;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.mob.repository.MobRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Owns the stable Location/stage -> MobL_N identity, initial population and final-stage BossZone contract. */
public final class StageMobBootstrapService {
    static final String DEFAULT_TYPE = "zombie";
    static final String DEFAULT_RESOURCE = "coins";
    static final BigDecimal DEFAULT_HP = BigDecimal.ONE;
    static final BigDecimal DEFAULT_REWARD = BigDecimal.ONE;

    private final MobRepository mobs;
    private final LocationRepository locations;
    private final LocationGenerationRepository generations;
    private final StageBossLayoutService bossLayout;

    public StageMobBootstrapService(
            MobRepository mobs,
            LocationRepository locations,
            LocationGenerationRepository generations) {
        if (mobs == null) throw new IllegalArgumentException("mobs is required");
        if (locations == null) throw new IllegalArgumentException("locations is required");
        if (generations == null) throw new IllegalArgumentException("generations is required");
        this.mobs = mobs;
        this.locations = locations;
        this.generations = generations;
        this.bossLayout = new StageBossLayoutService(locations);
    }

    public static String canonicalMobName(int worldLocationId, int mobSlotId) {
        MobZoneIds.encode(worldLocationId, mobSlotId);
        return "Mob" + worldLocationId + "_" + mobSlotId;
    }

    /**
     * Completes a freshly-created stage. This method compensates its own persistence writes on failure;
     * world/zone/generation compensation remains the responsibility of LocationProvisioningService.
     */
    public void bootstrapNewStage(int worldLocationId, int mobSlotId, double spawnY) {
        int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
        LocationDefinition zone = requireZone(internalId);
        String mobName = canonicalMobName(worldLocationId, mobSlotId);
        List<StoredMobSpawn> before = new ArrayList<>(locations.getMobSpawns(internalId));
        boolean createdMob = false;
        try {
            if (mobs.getMob(mobName) == null) {
                mobs.createMob(mobName, DEFAULT_TYPE, DEFAULT_HP, DEFAULT_REWARD.intValueExact());
                createdMob = true;
                mobs.setMobReward(mobName, DEFAULT_RESOURCE, DEFAULT_REWARD);
            }
            replaceWithInitialPopulation(zone, mobName, spawnY, before);
        } catch (RuntimeException failure) {
            restorePopulation(internalId, before, failure);
            if (createdMob) compensate(failure, () -> mobs.deleteMob(mobName));
            throw failure;
        }
    }

    /** Ensures that the final configured slot is persisted as the only Boss Zone for this Location. */
    public int reconcileBossLayout(int worldLocationId) {
        return bossLayout.reconcileLocation(worldLocationId);
    }

    /**
     * Reconciles already-created stages. Canonical population is the migration marker: once every spawn slot uses
     * MobL_N, later count edits are preserved and restarts become a no-op for that stage.
     */
    public int reconcileConfiguredRange(int worldLocationId, int firstMobSlotId, int lastMobSlotId) {
        if (firstMobSlotId < 1 || lastMobSlotId < firstMobSlotId) {
            throw new IllegalArgumentException("invalid mob slot range");
        }
        int changed = 0;
        for (int mobSlotId = firstMobSlotId; mobSlotId <= lastMobSlotId; mobSlotId++) {
            int internalId = MobZoneIds.encode(worldLocationId, mobSlotId);
            LocationDefinition zone = locations.getLocation(internalId);
            if (zone == null) continue;
            String mobName = canonicalMobName(worldLocationId, mobSlotId);
            boolean definitionCreated = ensureDefinition(mobName);
            List<StoredMobSpawn> existing = new ArrayList<>(locations.getMobSpawns(internalId));
            if (!isCanonicalPopulation(existing, mobName)) {
                replaceWithInitialPopulation(zone, mobName, migrationSpawnY(zone, existing), existing);
                changed++;
            } else if (definitionCreated) {
                changed++;
            }
        }
        reconcileBossLayout(worldLocationId);
        return changed;
    }

    private boolean ensureDefinition(String mobName) {
        if (mobs.getMob(mobName) != null) return false;
        boolean created = false;
        try {
            mobs.createMob(mobName, DEFAULT_TYPE, DEFAULT_HP, DEFAULT_REWARD.intValueExact());
            created = true;
            mobs.setMobReward(mobName, DEFAULT_RESOURCE, DEFAULT_REWARD);
            return true;
        } catch (RuntimeException failure) {
            if (created) compensate(failure, () -> mobs.deleteMob(mobName));
            throw failure;
        }
    }

    private LocationDefinition requireZone(int internalId) {
        LocationDefinition zone = locations.getLocation(internalId);
        if (zone == null) throw new IllegalStateException("Stage zone is missing: " + internalId);
        return zone;
    }

    private boolean isCanonicalPopulation(List<StoredMobSpawn> spawns, String mobName) {
        if (spawns.isEmpty()) return false;
        for (StoredMobSpawn spawn : spawns) {
            if (!mobName.equals(spawn.getMobName())) return false;
        }
        return true;
    }

    private void replaceWithInitialPopulation(
            LocationDefinition zone,
            String mobName,
            double spawnY,
            List<StoredMobSpawn> previous) {
        int internalId = zone.getLocationId();
        clearPopulation(internalId);
        try {
            locations.upsertMobSpawn(new StoredMobSpawn(
                    internalId,
                    1,
                    mobName,
                    zone.getCenterX(),
                    spawnY,
                    zone.getCenterZ(),
                    0.0F,
                    0.0F,
                    SpawnMode.FIXED_POINT,
                    MovementMode.ROAMING,
                    SpawnRealmScope.PLAYER_REALMS,
                    true));
        } catch (RuntimeException failure) {
            restorePopulation(internalId, previous, failure);
            throw failure;
        }
    }

    private double migrationSpawnY(LocationDefinition zone, List<StoredMobSpawn> existing) {
        Double generationY = generations.getAnchorY(zone.getLocationId());
        if (generationY != null && Double.isFinite(generationY)) return generationY;
        if (!existing.isEmpty() && Double.isFinite(existing.get(0).getSpawnY())) return existing.get(0).getSpawnY();
        if (zone.hasTransitionPoint() && Double.isFinite(zone.getTransitionY())) return zone.getTransitionY();
        return 64.0D;
    }

    private void clearPopulation(int internalId) {
        for (StoredMobSpawn spawn : new ArrayList<>(locations.getMobSpawns(internalId))) {
            locations.deleteMobSpawn(internalId, spawn.getSpawnId());
        }
    }

    private void restorePopulation(int internalId, List<StoredMobSpawn> previous, RuntimeException failure) {
        compensate(failure, () -> clearPopulation(internalId));
        for (StoredMobSpawn spawn : previous) compensate(failure, () -> locations.upsertMobSpawn(spawn));
    }

    private static void compensate(RuntimeException failure, Runnable action) {
        try { action.run(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
    }
}
