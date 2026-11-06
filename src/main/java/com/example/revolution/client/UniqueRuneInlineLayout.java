package com.example.revolution.client;

import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.util.List;

/** Server rune IDs determine stable slots, never the order of acquisition. */
final class UniqueRuneInlineLayout {
    private UniqueRuneInlineLayout() {}

    static UniqueRuneSnapshotPacket.Entry entryAtSlot(
            List<UniqueRuneSnapshotPacket.Entry> entries, String collectionId, int slot) {
        if (entries == null || collectionId == null || slot < 0 || slot >= 26) return null;
        String expectedId = runePrefix(collectionId) + String.format(java.util.Locale.ROOT, "%02d", slot + 1);
        for (UniqueRuneSnapshotPacket.Entry entry : entries)
            if (entry != null && collectionId.equals(entry.collectionId) && expectedId.equals(entry.runeId))
                return entry;
        return null;
    }

    private static String runePrefix(String collectionId) {
        switch (collectionId) {
            case "CRYSTAL_COLLECTION": return "crystal_";
            case "DAILY_COLLECTION": return "daily_";
            case "OFFERINGS_COLLECTION": return "offerings_";
            default: return "";
        }
    }
}
