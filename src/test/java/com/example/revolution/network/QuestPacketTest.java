package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuestPacketTest {
    @Test public void actionRoundTripCarriesOnlyIntentAndStableEntryId() {
        QuestActionPacket source = new QuestActionPacket("claim", "entry-123");
        ByteBuf buffer = Unpooled.buffer(); source.toBytes(buffer);
        QuestActionPacket restored = new QuestActionPacket(); restored.fromBytes(buffer);
        assertEquals("claim", restored.getAction());
        assertEquals("entry-123", restored.getEntryId());
    }

    @Test public void snapshotRoundTripPreservesAuthoritativeBoardState() {
        QuestSnapshotPacket.Entry entry = new QuestSnapshotPacket.Entry(
                "entry-123", "rune_enhance", "RUNE_ENHANCE_ANY", "4", "5", "COMPLETED", 3, 2);
        QuestSnapshotPacket source = new QuestSnapshotPacket(Arrays.asList(entry), 123456789L, 1, 42L);
        ByteBuf buffer = Unpooled.buffer(); source.toBytes(buffer);
        QuestSnapshotPacket restored = new QuestSnapshotPacket(); restored.fromBytes(buffer);
        assertEquals(123456789L, restored.getCooldownUntil());
        assertEquals(1, restored.getRerollsRemaining());
        assertEquals(42L, restored.getCompletedQuestCount());
        assertEquals(1, restored.getEntries().size());
        QuestSnapshotPacket.Entry actual = restored.getEntries().get(0);
        assertEquals("entry-123", actual.entryId);
        assertEquals("rune_enhance", actual.templateId);
        assertEquals("RUNE_ENHANCE_ANY", actual.eventType);
        assertEquals("4", actual.progress);
        assertEquals("5", actual.target);
        assertEquals("COMPLETED", actual.state);
        assertEquals(3, actual.crystalReward);
        assertEquals(2, actual.difficultyTier);
    }
}
