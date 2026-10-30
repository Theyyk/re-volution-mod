package com.example.revolution.client;

import com.example.revolution.network.RuneCollectionMetadataPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-side read model for the fixed v2.3.0 collection chain. */
final class ClientRuneCollections {
    private static List<String> ids = Collections.emptyList();
    private static List<String> names = Collections.emptyList();
    private static List<Integer> progress = Collections.emptyList();
    private static int unlockedMask = 1;
    private static int activeCollection;
    private static int unlockThreshold = 13;

    private ClientRuneCollections() {}

    static void accept(RuneCollectionMetadataPacket packet) {
        ids = Collections.unmodifiableList(new ArrayList<>(packet.getCollectionIds()));
        names = Collections.unmodifiableList(new ArrayList<>(packet.getDisplayNames()));
        progress = Collections.unmodifiableList(new ArrayList<>(packet.getImprovedOrHigherProgress()));
        unlockedMask = packet.getUnlockedMask();
        activeCollection = packet.getActiveCollection();
        unlockThreshold = packet.getUnlockThreshold();
    }

    static List<String> ids() { return ids; }
    static List<String> names() { return names; }
    static int activeCollection() { return activeCollection; }
    static int unlockThreshold() { return unlockThreshold; }
    static boolean isUnlocked(int index) {
        return index >= 0 && index < Integer.SIZE && (unlockedMask & (1 << index)) != 0;
    }
    static int progress(int index) {
        return index >= 0 && index < progress.size() ? progress.get(index) : 0;
    }
}
