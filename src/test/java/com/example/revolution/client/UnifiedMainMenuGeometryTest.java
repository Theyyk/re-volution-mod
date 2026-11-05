package com.example.revolution.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The chrome never covers the nav strip or leaves no room for content. */
public class UnifiedMainMenuGeometryTest {
    @Test
    void contentStaysBetweenStatisticsAndNavigationAcrossViewports() {
        int[][] sizes = {{1280,669},{1920,1080},{2560,1440},{854,480},
                {640,360},{2560,1080},{1080,1920},{1279,668},{320,180}};
        for (int[] size : sizes) {
            int w = size[0], h = size[1];
            GuiLayout rune = new GuiLayout(26);
            rune.update(w, h);
            int left = UnifiedMainMenuGeometry.contentLeft(w,h);
            int right = UnifiedMainMenuGeometry.contentRight(w,h);
            int top = UnifiedMainMenuGeometry.contentTop(w,h);
            assertTrue(left >= rune.canvasX + Math.round(rune.runePanelX * rune.canvasScale),
                    "feature content overlaps persistent character/equipment panel at " + w + "x" + h);
            assertTrue(right < rune.canvasX + Math.round(rune.navPanelX * rune.canvasScale),
                    "nav overlap at " + w + "x" + h);
            assertTrue(right > left + 100 * rune.canvasScale, "content collapsed");
            assertTrue(top > 0 && top < h, "content header out of view");
        }
    }
}
