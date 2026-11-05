package com.example.revolution.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Special collection controls must never collide with normal rune tabs. */
public class UniqueRuneInlineTabsTest {
    @Test
    void threeStableSelectorsAreAppendedAfterNormalCollections() {
        assertEquals(3, UniqueRuneInlineTabs.COUNT);
        assertEquals("CRYSTAL_COLLECTION", UniqueRuneInlineTabs.collectionId(0));
        assertEquals("DAILY_COLLECTION", UniqueRuneInlineTabs.collectionId(1));
        assertEquals("OFFERINGS_COLLECTION", UniqueRuneInlineTabs.collectionId(2));
        for (int i = 0; i < UniqueRuneInlineTabs.COUNT; i++) {
            int id = UniqueRuneInlineTabs.buttonId(i);
            assertTrue(UniqueRuneInlineTabs.isButton(id));
            assertEquals(i, UniqueRuneInlineTabs.index(id));
            // The existing normal Rune collection selector IDs are 100-107.
            assertTrue(id >= 140 && id < 200);
        }
        assertFalse(UniqueRuneInlineTabs.isButton(107));
    }

    @Test
    void placementReservesExistingOrdinaryButtonsAtEveryResolution() {
        int[][] sizes = {{1280,669},{1920,1080},{2560,1440},{854,480},{640,360},{2560,1080}};
        for (int[] size : sizes) {
            GuiLayout layout = new GuiLayout(26);
            layout.update(size[0],size[1]);
            int ordinaryRight = layout.runePanelX + 320;
            int uniqueX = UniqueRuneInlineTabs.positionX(layout,ordinaryRight);
            assertTrue(uniqueX >= ordinaryRight + 10);
            assertTrue(uniqueX + UniqueRuneInlineTabs.totalWidth()
                    <= layout.runePanelX + layout.runePanelWidth - 8);
        }
    }
}
