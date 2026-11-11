package com.example.revolution.client;

import com.example.revolution.network.PetSnapshotPacket;
import java.util.List;

/** Pure geometry and server-snapshot mapping for the 8+2 pet context cells. */
final class PetContextSlotLayout {
    static final int COUNT = 10;
    static final int NORMAL_COUNT = 8;
    private final int x, y, cellWidth, cellHeight, gap;

    PetContextSlotLayout(int contentLeft, int contentWidth, int top) {
        int width = Math.max(COUNT, contentWidth);
        gap = width < 40 ? 0 : Math.max(1, Math.min(5, width / 100));
        cellWidth = Math.max(1, (width - gap * (COUNT - 1)) / COUNT);
        cellHeight = Math.max(12, Math.min(45, cellWidth));
        int occupiedWidth = COUNT * cellWidth + (COUNT - 1) * gap;
        x = contentLeft + Math.max(0, (width - occupiedWidth) / 2);
        y = top;
    }

    int x(int index) { return x + index * (cellWidth + gap); }
    int y() { return y; }
    int width() { return cellWidth; }
    int height() { return cellHeight; }

    int hit(int mouseX, int mouseY) {
        if (mouseY < y || mouseY >= y + cellHeight) return -1;
        for (int i = 0; i < COUNT; i++)
            if (mouseX >= x(i) && mouseX < x(i) + cellWidth) return i;
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
