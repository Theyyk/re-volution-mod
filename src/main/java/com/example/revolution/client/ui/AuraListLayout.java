package com.example.revolution.client.ui;

/** Matching draw/click hit-testing and page bounds for the Aura collection. No Minecraft dependency. */
public final class AuraListLayout {
    private AuraListLayout() {}

    public static int lastPage(int count, int perPage) {
        if (perPage <= 0) throw new IllegalArgumentException("perPage");
        return Math.max(0, (Math.max(0, count) - 1) / perPage);
    }

    public static int indexAt(int mouseX, int mouseY, int x, int y, int width, int rowHeight,
                              int perPage, int page, int count) {
        if (width <= 0 || rowHeight <= 0 || perPage <= 0 || page < 0 || count < 0) return -1;
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + rowHeight * perPage)
            return -1;
        int index = page * perPage + (mouseY - y) / rowHeight;
        return index >= 0 && index < count ? index : -1;
    }
}
