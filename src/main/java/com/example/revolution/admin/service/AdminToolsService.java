package com.example.revolution.admin.service;

import com.example.revolution.admin.definition.AdminStageSnapshot;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.service.StageMobBootstrapService;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.service.MobProgressionService;
import com.example.revolution.number.LargeNumbers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Typed stage/progression reads; callers execute on the server thread. */
public final class AdminToolsService {
    private Runnable configurationGuard = () -> { };
    public void setConfigurationGuard(Runnable guard) { configurationGuard = guard; }
    private final LocationRepository locations;
    private final MobRepository mobs;
    private final MobProgressionService progression;
    public AdminToolsService(LocationRepository locations, MobRepository mobs, MobProgressionService progression) {
        this.locations = locations; this.mobs = mobs; this.progression = progression;
    }

    public List<AdminStageSnapshot> stages() {
        configurationGuard.run();
        List<LocationDefinition> zones = new ArrayList<>();
        Map<Integer, Integer> finalSlots = new LinkedHashMap<>();
        for (LocationDefinition zone : locations.getLocations()) {
            if (!MobZoneIds.isMobZoneId(zone.getLocationId())) continue;
            zones.add(zone);
            int world = MobZoneIds.worldLocationId(zone.getLocationId());
            int slot = MobZoneIds.mobSlotId(zone.getLocationId());
            finalSlots.put(world, Math.max(finalSlots.getOrDefault(world, 0), slot));
        }
        zones.sort(Comparator.comparingInt(LocationDefinition::getLocationId));
        List<StoredMobSpawn> spawns = locations.getMobSpawns();
        List<AdminStageSnapshot> rows = new ArrayList<>();
        for (LocationDefinition zone : zones) {
            int world = MobZoneIds.worldLocationId(zone.getLocationId());
            int slot = MobZoneIds.mobSlotId(zone.getLocationId());
            String canonical = StageMobBootstrapService.canonicalMobName(world, slot);
            String name = canonical;
            int count = 0;
            String problem = "";
            for (StoredMobSpawn spawn : spawns) {
                if (spawn.getLocationId() != zone.getLocationId() || !spawn.isEnabled()) continue;
                count++;
                if (!zone.contains(spawn.getSpawnX(), spawn.getSpawnZ())) problem = "Spawn fallback is outside its configured zone";
                if (!canonical.equals(spawn.getMobName())) problem = "Non-canonical/shared stage mob; editing is unavailable";
                name = spawn.getMobName();
            }
            MobDefinition definition = mobs.getMob(name);
            if (definition == null) problem = "Stage mob definition is missing";
            for (StoredMobSpawn spawn : spawns) {
                if (spawn.getLocationId() != zone.getLocationId() && name.equals(spawn.getMobName()))
                    problem = "Mob is shared with another stage; editing is unavailable";
            }
            boolean boss = finalSlots.get(world) == slot;
            if (boss != zone.isBossZone()) problem = "Persisted boss layout differs from configured final stage";
            rows.add(new AdminStageSnapshot(world, slot, zone.getLocationId(), count, name,
                    definition == null ? "0" : definition.hp.toString(),
                    definition == null ? "" : mobs.getMobResource(name),
                    definition == null ? "0" : mobs.getMobAmountValue(name).toString(),
                    zone.getKind().name(), problem, boss, zone.isBossZone(),
                    zone.getCenterX(), zone.hasTransitionPoint() ? zone.getTransitionY() : 0,
                    zone.getCenterZ(), zone.getRadius()));
        }
        return rows;
    }

    public List<ProgressionRow> progression(UUID uuid, double x, double z) {
        List<ProgressionRow> rows = new ArrayList<>();
        for (AdminStageSnapshot stage : stages()) {
            LocationDefinition zone = locations.getLocation(stage.internalId);
            rows.add(new ProgressionRow(stage.location, stage.stage, stage.boss,
                    progression.isUnlocked(uuid, stage.internalId), zone.contains(x, z)));
        }
        return rows;
    }

    public boolean grant(UUID uuid, int location, int stage) { return progression.grantStage(uuid, location, stage); }
    public boolean resetLocation(UUID uuid, int location) { return progression.resetLocation(uuid, location); }
    public boolean resetAll(UUID uuid) { return progression.resetFromLocation(uuid, 0); }
    public boolean resetCurrent(UUID uuid, double x, double z) {
        LocationDefinition current = null;
        for (LocationDefinition zone : locations.getLocations()) {
            if (!MobZoneIds.isMobZoneId(zone.getLocationId()) || !zone.contains(x, z)) continue;
            if (current != null) throw new IllegalArgumentException("Ambiguous current stage");
            current = zone;
        }
        if (current == null) throw new IllegalArgumentException("Player is outside configured stages");
        return progression.revokeFrom(uuid, MobZoneIds.worldLocationId(current.getLocationId()), MobZoneIds.mobSlotId(current.getLocationId()));
    }

    public static final class ProgressionRow {
        public final int location, stage;
        public final boolean boss, unlocked, current;
        public ProgressionRow(int location, int stage, boolean boss, boolean unlocked, boolean current) {
            this.location = location; this.stage = stage; this.boss = boss; this.unlocked = unlocked; this.current = current;
        }
    }
}
