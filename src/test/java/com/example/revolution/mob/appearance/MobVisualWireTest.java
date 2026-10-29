package com.example.revolution.mob.appearance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void buildsFinalPlayerFacingPayloadWithoutTechnicalMobId() {
        MobAppearanceDefinition appearance = MobAppearanceCatalog.bySlot(1, 1);
        String payload = MobVisualWire.displayNamePayload(appearance, "7", "10", "coins", "4");

        assertEquals("§cМоховая черепаха|7|10|coins|4|rvvis:1:1", payload);
        assertFalse(payload.contains("Mob1_1"));
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
