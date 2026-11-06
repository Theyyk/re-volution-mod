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
