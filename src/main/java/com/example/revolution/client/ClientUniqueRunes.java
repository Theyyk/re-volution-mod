package com.example.revolution.client;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.UniqueRuneActionPacket;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client mirror of factual server snapshots. It never predicts ownership/effects. */
public final class ClientUniqueRunes {
    private static UniqueRuneSnapshotPacket snapshot = new UniqueRuneSnapshotPacket();
    /** Epoch-based seed avoids reconnecting clients restarting below a nonce retained by the server. */
    private static long nonce = Math.max(1L, System.currentTimeMillis());
    private ClientUniqueRunes() {}

    public static synchronized void accept(UniqueRuneSnapshotPacket value) { snapshot = value == null ? new UniqueRuneSnapshotPacket() : value; }
    public static synchronized UniqueRuneSnapshotPacket snapshot() { return snapshot; }
    public static synchronized long nextNonce() {
        long now = System.currentTimeMillis();
        if (nonce < now) nonce = now;
        return nonce++;
    }
    public static void sync() { NetworkHandler.INSTANCE.sendToServer(new UniqueRuneActionPacket("sync", "", "", false, 0L, nextNonce())); }
    public static void action(String action, String collectionId, String runeId, boolean confirmed) {
        NetworkHandler.INSTANCE.sendToServer(new UniqueRuneActionPacket(action, collectionId, runeId, confirmed, 0L, nextNonce()));
    }
    public static List<UniqueRuneSnapshotPacket.Entry> entriesFor(String collectionId) {
        List<UniqueRuneSnapshotPacket.Entry> out = new ArrayList<>();
        for (UniqueRuneSnapshotPacket.Entry entry : snapshot.getEntries()) if (collectionId.equals(entry.collectionId)) out.add(entry);
        return Collections.unmodifiableList(out);
    }
}
