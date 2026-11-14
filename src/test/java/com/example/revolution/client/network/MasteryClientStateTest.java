package com.example.revolution.client.network;

import static org.junit.jupiter.api.Assertions.*;

import com.example.revolution.network.MasterySnapshotPacket;
import org.junit.jupiter.api.Test;

public final class MasteryClientStateTest {
    @Test public void dropsStaleSnapshotsAndDoesNotInventProgress() {
        MasteryClientState.reset();
        long old = MasteryClientState.nextRequestId();
        long current = MasteryClientState.nextRequestId();
        MasterySnapshotPacket stale = new MasterySnapshotPacket();
        stale.requestId = old; stale.runId = "stale";
        assertFalse(MasteryClientState.apply(stale));
        assertNull(MasteryClientState.snapshot());
        MasterySnapshotPacket valid = new MasterySnapshotPacket();
        valid.requestId = current; valid.artsUnlocked = false;
        assertTrue(MasteryClientState.apply(valid));
        assertEquals("", MasteryClientState.snapshot().runId);
        assertFalse(MasteryClientState.pending());
        assertFalse(MasteryClientState.apply(stale));
        assertEquals("", MasteryClientState.snapshot().runId);
        MasteryClientState.reset();
        assertNull(MasteryClientState.snapshot());
    }

    @Test public void consumesOpenOnce() {
        MasteryClientState.reset();
        MasteryClientState.requestOpen();
        assertTrue(MasteryClientState.consumeOpenRequested());
        assertFalse(MasteryClientState.consumeOpenRequested());
        MasteryClientState.reset();
    }
}
