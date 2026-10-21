package com.example.revolution.location.repository;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.StoredMobSpawn;
import java.util.List;

/** Persistence port for Location geometry and physical mob spawn slots. */
public interface LocationRepository {
    List<LocationDefinition> getLocations();
    LocationDefinition getLocation(int locationId);
    void upsertLocation(LocationDefinition location);
    List<StoredMobSpawn> getMobSpawns();
    List<StoredMobSpawn> getMobSpawns(int locationId);
    StoredMobSpawn getMobSpawn(int locationId, int spawnId);
    void upsertMobSpawn(StoredMobSpawn spawn);
    void deleteMobSpawn(int locationId, int spawnId);
}
