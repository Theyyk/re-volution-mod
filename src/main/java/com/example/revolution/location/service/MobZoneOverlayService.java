package com.example.revolution.location.service;

import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobZoneIds;
import com.example.revolution.location.repository.LocationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only snapshot provider for the admin-only client zone overlay. */
public final class MobZoneOverlayService {
    private final LocationRepository locations;

    public MobZoneOverlayService(LocationRepository locations) {
        this.locations = locations;
    }

    public List<LocationDefinition> snapshot() {
        List<LocationDefinition> zones = new ArrayList<>();
        for (LocationDefinition location : locations.getLocations()) {
            if (location != null && MobZoneIds.isMobZoneId(location.getLocationId())) zones.add(location);
        }
        zones.sort(Comparator.comparingInt(LocationDefinition::getLocationId));
        return zones;
    }
}
