package com.example.revolution.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-side in-memory overlay snapshot without client-only class references. */
public final class MobZoneOverlayState {
    private static volatile List<MobZoneOverlayPacket.Zone> zones = Collections.emptyList();

    private MobZoneOverlayState() {}

    public static List<MobZoneOverlayPacket.Zone> get() { return zones; }

    public static void replace(List<MobZoneOverlayPacket.Zone> next) {
        zones = Collections.unmodifiableList(new ArrayList<>(next));
    }

    public static void clear() { zones = Collections.emptyList(); }
}
