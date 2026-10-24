package com.example.revolution.location.repository;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.StoredMobSpawn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Test fixture for service configuration; no world or database is created. */
public final class InMemoryLocationRepository implements LocationRepository {
    private final Map<Integer, LocationDefinition> locations = new LinkedHashMap<>();
    private final List<StoredMobSpawn> spawns = new ArrayList<>();
    public int locationWrites;

    public void seed(LocationDefinition... values) {
        for (LocationDefinition value : values) locations.put(value.getLocationId(), value);
    }

    @Override public List<LocationDefinition> getLocations() { return new ArrayList<>(locations.values()); }
    @Override public LocationDefinition getLocation(int id) { return locations.get(id); }
    @Override public void upsertLocation(LocationDefinition value) {
        locationWrites++;
        locations.put(value.getLocationId(), value);
    }
    @Override public void deleteLocation(int id) { locations.remove(id); }
    @Override public List<StoredMobSpawn> getMobSpawns() { return new ArrayList<>(spawns); }
    @Override public List<StoredMobSpawn> getMobSpawns(int id) {
        return spawns.stream().filter(value -> value.getLocationId() == id).collect(Collectors.toList());
    }
    @Override public StoredMobSpawn getMobSpawn(int id, int spawnId) {
        return spawns.stream().filter(value -> value.getLocationId() == id && value.getSpawnId() == spawnId)
                .findFirst().orElse(null);
    }
    @Override public void upsertMobSpawn(StoredMobSpawn value) {
        deleteMobSpawn(value.getLocationId(), value.getSpawnId());
        spawns.add(value);
    }
    @Override public void deleteMobSpawn(int id, int spawnId) {
        spawns.removeIf(value -> value.getLocationId() == id && value.getSpawnId() == spawnId);
    }
}
