package com.example.revolution.client;

import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.util.List;

/** Server rune IDs determine stable slots, never the order of acquisition. */
final class UniqueRuneInlineLayout {
    private UniqueRuneInlineLayout() {}

    /** Owned slots and COMMON acquisitions are separate facts for legacy upgraded runes. */
    static String acquisitionCounter(int commonAcquired, int ownedCount) {
        String common = commonAcquired < 0 ? "?" : String.valueOf(commonAcquired);
        return "COMMON " + common + "/26 | Занято " + ownedCount + "/26";
    }

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
        return entryAtSlot(entries, collectionId, slotAtCanvasPoint(layout, canvasX, canvasY));
    }

    /** Deterministic tile number, -1 for gaps or points outside the grid. */
    static int slotAtCanvasPoint(GuiLayout layout, int canvasX, int canvasY) {
        if (layout == null) return -1;
        for (int slot = 0; slot < 26; slot++) {
            int[] bounds = layout.runeSlotPosition(slot);
            if (canvasX >= bounds[0] && canvasX < bounds[0] + layout.runeSlotWidth
                    && canvasY >= bounds[1] && canvasY < bounds[1] + layout.runeSlotHeight)
                return slot;
        }
        return -1;
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
