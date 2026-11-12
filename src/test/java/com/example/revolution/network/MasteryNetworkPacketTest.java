package com.example.revolution.network;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** UI-03-M wire and replay contract; no Minecraft server needed. */
public final class MasteryNetworkPacketTest {
    @Test public void actionRoundTrip() {
        MasteryActionPacket original = new MasteryActionPacket("start", "RUNE_RESOURCES", "", 42L);
        ByteBuf buf = Unpooled.buffer();
        try {
            original.toBytes(buf);
            MasteryActionPacket restored = new MasteryActionPacket();
            restored.fromBytes(buf);
            assertEquals("start", restored.getAction());
            assertEquals("RUNE_RESOURCES", restored.getBranch());
            assertEquals("", restored.getExpectedRunId());
            assertEquals(42L, restored.getRequestId());
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }

    @Test public void snapshotRoundTripIncludesRunResourcesAndUnlockState() {
        MasterySnapshotPacket original = new MasterySnapshotPacket();
        original.requestId = 77L;
        original.artsUnlocked = true;
        original.response = "RUN_STARTED";
        original.runId = "run-a";
        original.activeBranch = "POISON";
        original.progress = "12345678901234567890123";
        original.level = 17;
        original.fragments = "19";
        original.masteryPending = "4";
        original.petPending = "8";
        original.ranks = "POISON_I";
        original.startedAt = 1234L;
        original.masteryResetAvailable = true;
        original.premiumPreserveAvailable = true;
        original.premiumPreserveRunes = true;
        original.premiumPreservePets = false;
        original.premiumCrystalCost = 12;
        original.addBranch(new MasterySnapshotPacket.BranchEntry("POISON", "POISON_DAMAGE", "999", 2, 15, 3));
        original.addPermanentRank(new MasterySnapshotPacket.RankEntry("POISON", "POISON_I", "100", 2, 15));
        original.addPermanentRank(new MasterySnapshotPacket.RankEntry("POISON", "POISON_II", "500", 0, 15));
        ByteBuf buf = Unpooled.buffer();
        try {
            original.toBytes(buf);
            MasterySnapshotPacket copy = new MasterySnapshotPacket();
            copy.fromBytes(buf);
            assertEquals(77L, copy.requestId);
            assertTrue(copy.artsUnlocked);
            assertEquals("RUN_STARTED", copy.response);
            assertEquals("run-a", copy.runId);
            assertEquals("POISON", copy.activeBranch);
            assertEquals("12345678901234567890123", copy.progress);
            assertEquals(17, copy.level);
            assertEquals("19", copy.fragments);
            assertEquals("4", copy.masteryPending);
            assertEquals("8", copy.petPending);
            assertEquals("POISON_I", copy.ranks);
            assertEquals(1234L, copy.startedAt);
            assertTrue(copy.masteryResetAvailable);
            assertTrue(copy.premiumPreserveAvailable);
            assertTrue(copy.premiumPreserveRunes);
            assertFalse(copy.premiumPreservePets);
            assertEquals(12, copy.premiumCrystalCost);
            assertEquals(1, copy.getBranches().size());
            assertEquals("POISON", copy.getBranches().get(0).id);
            assertEquals(2, copy.getBranches().get(0).tier);
            assertEquals(2, copy.getPermanentRanks().size());
            assertEquals("POISON_I", copy.getPermanentRanks().get(0).id);
            assertEquals("100", copy.getPermanentRanks().get(0).threshold);
            assertEquals(2, copy.getPermanentRanks().get(0).metaLevel);
            assertEquals(15, copy.getPermanentRanks().get(0).metaCap);
            assertEquals("POISON_II", copy.getPermanentRanks().get(1).id);
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }

    @Test public void replayGuardSeparatesPlayersAndRejectsOldNonces() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        MasteryActionPacketHandler.clear();
        try {
            assertFalse(MasteryActionPacketHandler.accept(a, 0));
            assertTrue(MasteryActionPacketHandler.accept(a, 100));
            assertFalse(MasteryActionPacketHandler.accept(a, 100));
            assertFalse(MasteryActionPacketHandler.accept(a, 99));
            assertTrue(MasteryActionPacketHandler.accept(b, 99));
            assertTrue(MasteryActionPacketHandler.accept(a, 101));
        } finally { MasteryActionPacketHandler.clear(); }
    }
}
