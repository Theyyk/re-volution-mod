package com.example.revolution.client;

import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UniqueRuneInlineLayoutTest {
    private static UniqueRuneSnapshotPacket.Entry entry(String id, String collection) {
        UniqueRuneSnapshotPacket.Entry result = new UniqueRuneSnapshotPacket.Entry();
        result.runeId = id;
        result.collectionId = collection;
        return result;
    }

    @Test
    void acquisitionCounterDoesNotConfuseOwnedWithCommon() {
        assertEquals("COMMON 24/26 | Занято 26/26", UniqueRuneInlineLayout.acquisitionCounter(24, 26));
        assertEquals("COMMON 18/26 | Занято 26/26", UniqueRuneInlineLayout.acquisitionCounter(18, 26));
        assertEquals("COMMON 26/26 | Занято 26/26", UniqueRuneInlineLayout.acquisitionCounter(26, 26));
        assertEquals("COMMON ?/26 | Занято 26/26", UniqueRuneInlineLayout.acquisitionCounter(-1, 26));
    }

    @Test
    void randomlyAcquiredRunesStayInTheirNumberedSlots() {
        List<UniqueRuneSnapshotPacket.Entry> owned = Arrays.asList(
            entry("crystal_21", "CRYSTAL_COLLECTION"),
            entry("crystal_24", "CRYSTAL_COLLECTION"),
            entry("crystal_13", "CRYSTAL_COLLECTION"),
            entry("crystal_02", "CRYSTAL_COLLECTION"),
            entry("crystal_01", "CRYSTAL_COLLECTION"),
            entry("crystal_03", "CRYSTAL_COLLECTION")
        );
        assertEquals("crystal_21", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 20).runeId);
        assertEquals("crystal_24", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 23).runeId);
        assertEquals("crystal_13", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 12).runeId);
        assertEquals("crystal_02", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 1).runeId);
        assertEquals("crystal_01", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 0).runeId);
        assertEquals("crystal_03", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 2).runeId);
        assertNull(UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 3));
        assertNull(UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 26));
    }

    @Test
    void collectionPrefixAndOwnershipMustMatch() {
        List<UniqueRuneSnapshotPacket.Entry> owned = Arrays.asList(
            entry("crystal_01", "CRYSTAL_COLLECTION"),
            entry("daily_01", "DAILY_COLLECTION"),
            entry("offerings_26", "OFFERINGS_COLLECTION")
        );
        assertEquals("crystal_01", UniqueRuneInlineLayout.entryAtSlot(owned, "CRYSTAL_COLLECTION", 0).runeId);
        assertEquals("daily_01", UniqueRuneInlineLayout.entryAtSlot(owned, "DAILY_COLLECTION", 0).runeId);
        assertEquals("offerings_26", UniqueRuneInlineLayout.entryAtSlot(owned, "OFFERINGS_COLLECTION", 25).runeId);
        assertNull(UniqueRuneInlineLayout.entryAtSlot(owned, "DAILY_COLLECTION", 25));
    }

    @Test
    void crystalEveryFullCommonSlotHasAnExactAndIndependentClickTarget() {
        List<UniqueRuneSnapshotPacket.Entry> owned = new java.util.ArrayList<>();
        for (int rune = 1; rune <= 26; rune++)
            owned.add(entry(String.format(java.util.Locale.ROOT, "crystal_%02d", rune), "CRYSTAL_COLLECTION"));
        java.util.Collections.reverse(owned); // acquisition order must never affect slot identity
        GuiLayout layout = new GuiLayout(26);
        for (int[] viewport : new int[][] {{1280, 669}, {854, 480}, {1920, 1080}, {2560, 1080}}) {
            layout.update(viewport[0], viewport[1]);
            for (int slot = 0; slot < 26; slot++) {
                int[] bounds = layout.runeSlotPosition(slot);
                int sx = Math.round(layout.canvasX + (bounds[0] + layout.runeSlotWidth / 2.0f) * layout.canvasScale);
                int sy = Math.round(layout.canvasY + (bounds[1] + layout.runeSlotHeight / 2.0f) * layout.canvasScale);
                int x = layout.canvasMouseX(sx);
                int y = layout.canvasMouseY(sy);
                assertEquals(slot, UniqueRuneInlineLayout.slotAtCanvasPoint(layout, x, y),
                        "clicked visual slot must be the same number at " + viewport[0] + "x" + viewport[1]);
                assertEquals(String.format(java.util.Locale.ROOT, "crystal_%02d", slot + 1),
                        UniqueRuneInlineLayout.entryAtCanvasPoint(owned, "CRYSTAL_COLLECTION", layout, x, y).runeId);
            }
            int[] six = layout.runeSlotPosition(5);
            assertEquals("crystal_06", UniqueRuneInlineLayout.entryAtCanvasPoint(
                    owned, "CRYSTAL_COLLECTION", layout, six[0] + 3, six[1] + 3).runeId);
            int[] ten = layout.runeSlotPosition(9);
            assertEquals("crystal_10", UniqueRuneInlineLayout.entryAtCanvasPoint(
                    owned, "CRYSTAL_COLLECTION", layout, ten[0] + 3, ten[1] + 3).runeId);
            int[] nineteen = layout.runeSlotPosition(18);
            int[] twentyFive = layout.runeSlotPosition(24);
            assertEquals("crystal_19", UniqueRuneInlineLayout.entryAtCanvasPoint(
                    owned, "CRYSTAL_COLLECTION", layout, nineteen[0] + 3, nineteen[1] + 3).runeId);
            assertEquals("crystal_25", UniqueRuneInlineLayout.entryAtCanvasPoint(
                    owned, "CRYSTAL_COLLECTION", layout, twentyFive[0] + 3, twentyFive[1] + 3).runeId);
            int[] first = layout.runeSlotPosition(0);
            assertEquals(-1, UniqueRuneInlineLayout.slotAtCanvasPoint(
                    layout, first[0] + layout.runeSlotWidth + 1, first[1] + 2));
        }
    }

    @Test
    void emptyOrCrossCollectionTilesNeverResolveToAnotherOwnedRune() {
        GuiLayout layout = new GuiLayout(26);
        layout.update(1280, 669);
        List<UniqueRuneSnapshotPacket.Entry> owned = Arrays.asList(
                entry("crystal_06", "CRYSTAL_COLLECTION"),
                entry("crystal_10", "CRYSTAL_COLLECTION"),
                entry("offerings_06", "OFFERINGS_COLLECTION"));
        int[] empty = layout.runeSlotPosition(18);
        assertNull(UniqueRuneInlineLayout.entryAtCanvasPoint(owned, "CRYSTAL_COLLECTION",
                layout, empty[0] + 10, empty[1] + 10));
        int[] crystal = layout.runeSlotPosition(5);
        assertEquals("crystal_06", UniqueRuneInlineLayout.entryAtCanvasPoint(owned, "CRYSTAL_COLLECTION",
                layout, crystal[0] + 10, crystal[1] + 10).runeId);
        assertEquals("offerings_06", UniqueRuneInlineLayout.entryAtCanvasPoint(owned, "OFFERINGS_COLLECTION",
                layout, crystal[0] + 10, crystal[1] + 10).runeId);
    }

    @Test
    void clientSelectionPersistsUntilReset() {
        ClientUniqueRunes.reset();
        assertEquals(-1, ClientUniqueRunes.selectedCollectionIndex());
        ClientUniqueRunes.selectCollectionIndex(0);
        assertEquals(0, ClientUniqueRunes.selectedCollectionIndex());
        ClientUniqueRunes.selectCollectionIndex(2);
        assertEquals(2, ClientUniqueRunes.selectedCollectionIndex());
        ClientUniqueRunes.reset();
        assertEquals(-1, ClientUniqueRunes.selectedCollectionIndex());
    }
}
