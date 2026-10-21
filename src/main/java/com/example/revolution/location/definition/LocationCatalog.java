package com.example.revolution.location.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable Location and Mob Spawn catalog. Persistence is deliberately outside the v1.3.5 foundation. */
public final class LocationCatalog {
    private final List<LocationDefinition> locations;
    private final Map<Integer, LocationDefinition> locationsById;
    private final Map<Integer, List<MobSpawnDefinition>> spawnsByLocation;
    private final Map<String, MobSpawnDefinition> spawnsByKey;

    public LocationCatalog(List<LocationDefinition> locations, List<MobSpawnDefinition> spawns) {
        if (locations == null) throw new IllegalArgumentException("locations are required");
        if (spawns == null) throw new IllegalArgumentException("spawns are required");

        List<LocationDefinition> locationCopy = new ArrayList<>(locations.size());
        Map<Integer, LocationDefinition> byId = new HashMap<>();
        for (LocationDefinition location : locations) {
            if (location == null) throw new IllegalArgumentException("location must not be null");
            if (byId.put(location.getLocationId(), location) != null) {
                throw new IllegalArgumentException("Duplicate location ID: " + location.getLocationId());
            }
            locationCopy.add(location);
        }

        Map<Integer, List<MobSpawnDefinition>> grouped = new HashMap<>();
        Map<String, MobSpawnDefinition> byKey = new HashMap<>();
        for (MobSpawnDefinition spawn : spawns) {
            if (spawn == null) throw new IllegalArgumentException("spawn must not be null");
            if (!byId.containsKey(spawn.getLocationId())) {
                throw new IllegalArgumentException("Unknown location ID for spawn: " + spawn.getLocationId());
            }
            String key = key(spawn.getLocationId(), spawn.getSpawnId());
            if (byKey.put(key, spawn) != null) {
                throw new IllegalArgumentException(
                        "Duplicate spawn ID " + spawn.getSpawnId() + " in location " + spawn.getLocationId());
            }
            List<MobSpawnDefinition> locationSpawns = grouped.get(spawn.getLocationId());
            if (locationSpawns == null) {
                locationSpawns = new ArrayList<>();
                grouped.put(spawn.getLocationId(), locationSpawns);
            }
            locationSpawns.add(spawn);
        }

        Map<Integer, List<MobSpawnDefinition>> immutableGrouped = new HashMap<>();
        for (Map.Entry<Integer, List<MobSpawnDefinition>> entry : grouped.entrySet()) {
            immutableGrouped.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.locations = Collections.unmodifiableList(locationCopy);
        this.locationsById = Collections.unmodifiableMap(byId);
        this.spawnsByLocation = Collections.unmodifiableMap(immutableGrouped);
        this.spawnsByKey = Collections.unmodifiableMap(byKey);
    }

    public List<LocationDefinition> allLocations() { return locations; }
    public LocationDefinition byLocationId(int locationId) { return locationsById.get(locationId); }

    public List<MobSpawnDefinition> spawnsForLocation(int locationId) {
        List<MobSpawnDefinition> spawns = spawnsByLocation.get(locationId);
        return spawns == null ? Collections.<MobSpawnDefinition>emptyList() : spawns;
    }

    public MobSpawnDefinition spawn(int locationId, int spawnId) {
        return spawnsByKey.get(key(locationId, spawnId));
    }

    private static String key(int locationId, int spawnId) {
        return locationId + ":" + spawnId;
    }
}
