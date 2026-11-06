package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Wire regression: the client's price and daily reset must come from the server. */
public class UniqueRuneSnapshotPacketUiTest {
    @Test void roundTripContainsExactPriceAndUtcDailyReset() {
        UniqueRuneSnapshotPacket.Entry crystal = new UniqueRuneSnapshotPacket.Entry();
        crystal.collectionId = "CRYSTAL_COLLECTION";
        crystal.runeId = "crystal_test";
        crystal.name = "Кристальная руна";
        crystal.rarity = "IMPROVED";
        crystal.enhancement = 5;
        crystal.enhancementPearlCost = "12345678901234567890.5";
        crystal.effects = "CLICK +10";
        UniqueRuneSnapshotPacket.Collection collection = new UniqueRuneSnapshotPacket.Collection();
        collection.collectionId = crystal.collectionId;
        collection.rarity = "IMPROVED";
        collection.unlocked = true;
        collection.canRollback = true;
        long next = 1794096000000L;
        UniqueRuneSnapshotPacket sent = new UniqueRuneSnapshotPacket(
                Arrays.asList(crystal), Arrays.asList(collection),
                "1000", "5", "9", "10", "20", "draft", next);
        sent.setDailyExtraReward("coins", "100");
        sent.setCrystalEnhancementCost("1");
        sent.setLastCrystalRoll("crystal_26", true, "3");
        ByteBuf bytes = Unpooled.buffer();
        sent.toBytes(bytes);
        UniqueRuneSnapshotPacket received = new UniqueRuneSnapshotPacket();
        received.fromBytes(bytes);
        assertEquals(next, received.getDailyNextClaimMillis());
        assertEquals("coins", received.getDailyExtraResource());
        assertEquals("100", received.getDailyExtraAmount());
        assertEquals("1", received.getCrystalEnhancementCost());
        assertEquals("crystal_26", received.getLastCrystalRollId());
        assertTrue(received.isLastCrystalRollDuplicate());
        assertEquals("3", received.getLastCrystalRollCompensation());
        assertEquals(crystal.enhancementPearlCost, received.getEntries().get(0).enhancementPearlCost);
        assertEquals("5", received.getCrystals());
        assertEquals("1000", received.getPearl());
        assertEquals("9", received.getOfferings());
        assertTrue(received.getCollections().get(0).unlocked);
    }
}
