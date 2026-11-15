package com.example.revolution.client.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ClientJoinSnapshotGateTest {
    @Test void doesNotSyncUntilPlayerAndConnectionAreReady() {
        ClientJoinSnapshotGate gate = new ClientJoinSnapshotGate();
        assertFalse(gate.shouldRequest(false, false));
        assertFalse(gate.shouldRequest(true, false));
        assertFalse(gate.shouldRequest(false, true));
        assertTrue(gate.shouldRequest(true, true));
        assertFalse(gate.shouldRequest(true, true));
    }

    @Test void reconnectWithSamePlayerReissuesSnapshotsWithoutOpeningE() {
        ClientJoinSnapshotGate gate = new ClientJoinSnapshotGate();
        assertTrue(gate.shouldRequest(true, true));
        assertFalse(gate.shouldRequest(true, true));
        assertFalse(gate.shouldRequest(false, false));
        assertFalse(gate.shouldRequest(true, false));
        assertTrue(gate.shouldRequest(true, true));
        assertFalse(gate.shouldRequest(true, true));
    }

    @Test void playerBecomingUnavailableRearmsGate() {
        ClientJoinSnapshotGate gate = new ClientJoinSnapshotGate();
        assertTrue(gate.shouldRequest(true, true));
        assertFalse(gate.shouldRequest(true, false));
        assertTrue(gate.shouldRequest(true, true));
    }
}
