package com.example.revolution.client.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.revolution.network.GodWorshipSnapshotPacket;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class GodWorshipClientStateTest {
    @AfterEach void cleanup() { GodWorshipClientState.clear(); }

    @Test void disconnectClearsPendingOpenAndServerSnapshot() {
        GodWorshipClientState.requestOpen();
        GodWorshipClientState.update(new GodWorshipSnapshotPacket(
                Collections.singletonList(new GodWorshipSnapshotPacket.Entry(
                        "GOD_1", true, 2L, "CHARGE", 0L, 0L, 3L, 10L,
                        "100", "200", "coins", "CLICK_DAMAGE_MULTIPLIER", "",
                        "1.25", false, "MANUAL", "")),
                "50", 1L, 2L, "100", true, true, false));
        assertTrue(GodWorshipClientState.isOpenRequested());
        assertEquals(1, GodWorshipClientState.snapshot().entries.size());

        GodWorshipClientState.clear();

        assertFalse(GodWorshipClientState.isOpenRequested());
        assertTrue(GodWorshipClientState.snapshot().entries.isEmpty());
        assertEquals("0", GodWorshipClientState.snapshot().offerings);
        assertFalse(GodWorshipClientState.snapshot().autoActivationEnabled);
    }
}
