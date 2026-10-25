package com.example.revolution.mob.appearance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class MobVisualWireTest {
    @Test
    void roundTripsStableLocationSlotMarker() {
        String token = MobVisualWire.encode(1, 41);
        assertEquals("rvvis:1:41", token);
        assertArrayEquals(new int[] {1, 41}, MobVisualWire.decode(token));
        assertEquals("forest_ancient_guardian", MobVisualWire.resolve(token).getMobId());
    }

    @Test
    void rejectsMalformedMarkers() {
        assertNull(MobVisualWire.decode(null));
        assertNull(MobVisualWire.decode("rvvis:"));
        assertNull(MobVisualWire.decode("rvvis:1:0"));
        assertNull(MobVisualWire.decode("rvvis:x:2"));
        assertNull(MobVisualWire.decode("other:1:2"));
    }
}
