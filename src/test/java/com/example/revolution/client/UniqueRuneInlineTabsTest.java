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
            int uniqueY = UniqueRuneInlineTabs.positionY(layout,ordinaryRight);
            assertTrue(uniqueX >= ordinaryRight + 10);
            assertEquals(layout.runePanelY + 57, uniqueY);
            assertTrue(uniqueX + UniqueRuneInlineTabs.totalWidth()
                    <= layout.runePanelX + layout.runePanelWidth - 8);

            // If normal tabs fill the entire first row, the three special
            // controls wrap onto the spare row above the rune inventory grid.
            int crowdedRight = layout.runePanelX + layout.runePanelWidth - 20;
            assertFalse(UniqueRuneInlineTabs.fitsFirstRow(layout,crowdedRight));
            assertEquals(layout.runePanelX + 8,
                    UniqueRuneInlineTabs.positionX(layout,crowdedRight));
            int wrappedY = UniqueRuneInlineTabs.positionY(layout,crowdedRight);
            assertEquals(layout.runePanelY + 76,wrappedY);
            assertTrue(wrappedY > layout.runePanelY + 57 + 18);
            assertTrue(wrappedY + 18 < layout.runeGridY);
            assertTrue(UnqiueTabsFit(layout));
        }
    }

    private static boolean UnqiueTabsFit(GuiLayout layout) {
        return layout.runePanelX + 8 + UniqueRuneInlineTabs.totalWidth()
                <= layout.runePanelX + layout.runePanelWidth - 8;
    }
}
