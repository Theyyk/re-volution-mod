package com.example.revolution.client.network;

import com.example.revolution.network.MasterySnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ephemeral server projection. There is no second persisted Mastery state. */
public final class MasteryClientState {
    private static final List<MasterySnapshotPacket.BranchEntry> BRANCHES = new ArrayList<>();
    private static long requestSequence = System.currentTimeMillis();
    private static long latestIssued;
    private static MasterySnapshotPacket snapshot;
    private static boolean openRequested;
    private static boolean pending;
    private MasteryClientState() {}

    public static synchronized long nextRequestId() {
        long id = ++requestSequence;
        latestIssued = id;
        pending = true;
        return id;
    }
    /** Late replies from earlier operations must not replace newer state. */
    public static synchronized boolean apply(MasterySnapshotPacket next) {
        if (next == null || next.requestId < latestIssued) return false;
        snapshot = next;
        BRANCHES.clear();
        BRANCHES.addAll(next.getBranches());
        pending = false;
        return true;
    }
    public static synchronized MasterySnapshotPacket snapshot() { return snapshot; }
    public static synchronized List<MasterySnapshotPacket.BranchEntry> branches() {
        return Collections.unmodifiableList(new ArrayList<>(BRANCHES));
    }
    public static synchronized boolean pending() { return pending; }
    public static synchronized void requestOpen() { openRequested = true; }
    public static synchronized boolean consumeOpenRequested() {
        boolean requested = openRequested;
        openRequested = false;
        return requested;
    }
    public static synchronized void cancelOpen() { openRequested = false; }
    public static synchronized void reset() {
        snapshot = null; BRANCHES.clear(); openRequested = false;
        pending = false; latestIssued = requestSequence;
    }
}
