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

    /**
     * Single authority for the tile hit target used by clicking and hover text.
     * The input here is already converted into logical canvas coordinates.
     * A gap between tiles must never be interpreted as the neighbouring rune.
     */
    static UniqueRuneSnapshotPacket.Entry entryAtCanvasPoint(
            List<UniqueRuneSnapshotPacket.Entry> entries, String collectionId,
            GuiLayout layout, int canvasX, int canvasY) {
        if (layout == null) return null;
        for (int slot = 0; slot < 26; slot++) {
            int[] bounds = layout.runeSlotPosition(slot);
            if (canvasX < bounds[0] || canvasX >= bounds[0] + layout.runeSlotWidth
                    || canvasY < bounds[1] || canvasY >= bounds[1] + layout.runeSlotHeight)
                continue;
            return entryAtSlot(entries, collectionId, slot);
        }
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
