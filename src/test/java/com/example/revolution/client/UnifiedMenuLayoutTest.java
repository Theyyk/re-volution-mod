package com.example.revolution.client;

import com.example.revolution.client.ui.UiMenuSection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnifiedMenuLayoutTest {
    @Test
    void tabsDoNotOverlapRuneOrCloseAtCommonSizes() {
        int[][] sizes = {{1280,669},{2560,1338},{1920,1080},{854,480},
                {640,360},{2560,1080},{320,180},{1080,1920},{1279,668}};
        for (int[] size : sizes) {
            GuiLayout layout = new GuiLayout(26);
            layout.update(size[0], size[1]);
            int x = UnifiedMenuLayout.x(layout);
            int button = UnifiedMenuLayout.size(layout);
            int count = UiMenuSection.auxiliaryTabs().length;
            assertEquals(30, button);
            assertEquals(layout.navPanelX + 2, x);
            int runeBottom = layout.navPanelY + 2 + button;
            int lastBottom = runeBottom;
            for (int i = 0; i < count; i++) {
                int top = UnifiedMenuLayout.y(layout, i);
                assertTrue(top > lastBottom, "tab overlap at " + size[0] + "x" + size[1]);
                lastBottom = top + button;
            }
            int closeTop = layout.navPanelY + layout.navPanelHeight - 2 - button;
            assertTrue(lastBottom < closeTop, "tabs overlap close at " + size[0] + "x" + size[1]);
        }
    }
}
