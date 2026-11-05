package com.example.revolution.client.mechanic;

import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Snapshot-derived UI visibility; no separate player-controlled UI preference. */
public class ClientMechanicUnlocksUiTest {
    @Test
    void serverVisibilityIsAuthoritative() {
        ClientMechanicUnlocks.reset();
        assertEquals(UiAvailability.HIDDEN,
                ClientMechanicUnlocks.uiState("GOD_WORSHIP", UiAvailability.AVAILABLE));

        MechanicUnlockSnapshotPacket.Entry god = new MechanicUnlockSnapshotPacket.Entry();
        god.unlockId = "GOD_WORSHIP";
        god.unlocked = false;
        ClientMechanicUnlocks.apply(new MechanicUnlockSnapshotPacket(
                Collections.singletonList(god), false, "draft"));

        assertEquals(UiAvailability.VISIBLE_LOCKED,
                ClientMechanicUnlocks.uiState("GOD_WORSHIP", UiAvailability.AVAILABLE));
        assertEquals(UiAvailability.HIDDEN,
                ClientMechanicUnlocks.uiState("GOD_AUTO_WORSHIP", UiAvailability.AVAILABLE));

        god.unlocked = true;
        MechanicUnlockSnapshotPacket.Entry auto = new MechanicUnlockSnapshotPacket.Entry();
        auto.unlockId = "GOD_AUTO_WORSHIP";
        auto.unlocked = false;
        ClientMechanicUnlocks.apply(new MechanicUnlockSnapshotPacket(
                Arrays.asList(god, auto), false, "draft"));

        assertEquals(UiAvailability.AVAILABLE,
                ClientMechanicUnlocks.uiState("GOD_WORSHIP", UiAvailability.AVAILABLE));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                ClientMechanicUnlocks.uiState("GOD_AUTO_WORSHIP", UiAvailability.AVAILABLE));
        assertEquals(UiAvailability.VISIBLE_LOCKED,
                ClientMechanicUnlocks.uiState("GOD_AUTO_WORSHIP", UiAvailability.VISIBLE_LOCKED));
        assertEquals(UiAvailability.HIDDEN,
                ClientMechanicUnlocks.uiState("GOD_AUTO_WORSHIP", UiAvailability.HIDDEN));

        ClientMechanicUnlocks.reset();
        assertEquals(UiAvailability.HIDDEN,
                ClientMechanicUnlocks.uiState("GOD_WORSHIP", UiAvailability.AVAILABLE));
    }
}
