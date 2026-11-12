package com.example.revolution.client;

import com.example.revolution.network.PetSnapshotPacket;
import java.util.List;

/** Shared rendering/input geometry for the server-backed 8 normal + 2 premium Pet board. */
final class PetContextSlotLayout {
    static final int COUNT = 10;
    static final int NORMAL_COUNT = 8;
    private static final int COLUMNS = 5;
    private final int x, y, cellWidth, cellHeight, gap;

    PetContextSlotLayout(int contentLeft, int contentWidth, int top) {
        int width = Math.max(COLUMNS, contentWidth);
        gap = width < 40 ? 0 : Math.max(2, Math.min(7, width / 70));
        cellWidth = Math.max(1, (width - gap * (COLUMNS - 1)) / COLUMNS);
        cellHeight = Math.max(12, Math.min(31, cellWidth));
        int occupiedWidth = COLUMNS * cellWidth + (COLUMNS - 1) * gap;
        x = contentLeft + Math.max(0, (width - occupiedWidth) / 2);
        y = top;
    }

    int x(int index) { return x + (index % COLUMNS) * (cellWidth + gap); }
    int y(int index) { return y + (index / COLUMNS) * (cellHeight + gap); }
    int width() { return cellWidth; }
    int cellHeight() { return cellHeight; }
    /** Entire occupied board height including the gap between two rows. */
    int height() { return 2 * cellHeight + gap; }

    int hit(int mouseX, int mouseY) {
        for (int i = 0; i < COUNT; i++) {
            int sx = x(i), sy = y(i);
            if (mouseX >= sx && mouseX < sx + cellWidth
                    && mouseY >= sy && mouseY < sy + cellHeight) return i;
        }
        return -1;
    }

    /** Visual order: open normal, open premium, locked normal, locked premium. */
    static int physicalAt(int displayIndex, int normalSlots, int premiumSlots) {
        if (displayIndex < 0 || displayIndex >= COUNT) return -1;
        int normal = Math.max(0, Math.min(NORMAL_COUNT, normalSlots));
        int premium = Math.max(0, Math.min(COUNT - NORMAL_COUNT, premiumSlots));
        if (displayIndex < normal) return displayIndex;
        int next = displayIndex - normal;
        if (next < premium) return NORMAL_COUNT + next;
        next -= premium;
        if (next < NORMAL_COUNT - normal) return normal + next;
        return NORMAL_COUNT + premium + next - (NORMAL_COUNT - normal);
    }

    static int displayAt(int physicalIndex, int normalSlots, int premiumSlots) {
        for (int i = 0; i < COUNT; i++)
            if (physicalAt(i, normalSlots, premiumSlots) == physicalIndex) return i;
        return -1;
    }

    static boolean unlocked(int index, int normalSlots, int premiumSlots) {
        if (index < 0 || index >= COUNT) return false;
        return index < NORMAL_COUNT ? index < normalSlots : index - NORMAL_COUNT < premiumSlots;
    }

    static PetSnapshotPacket.Entry assigned(List<PetSnapshotPacket.Entry> entries, int index) {
        if (entries == null || index < 0 || index >= COUNT) return null;
        for (PetSnapshotPacket.Entry entry : entries)
            if (entry.selected && entry.slotIndex == index) return entry;
        return null;
    }
}
