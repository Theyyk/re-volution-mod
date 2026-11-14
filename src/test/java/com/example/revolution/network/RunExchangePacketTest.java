package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RunExchangePacketTest {
    @Test public void actionPacketCarriesOnlyServerResolvableIntent() {
        RunExchangeActionPacket sent = new RunExchangeActionPacket("purchase", "run-1", "coins_fragments", 123L);
        ByteBuf bytes = Unpooled.buffer();
        sent.toBytes(bytes);
        RunExchangeActionPacket received = new RunExchangeActionPacket();
        received.fromBytes(bytes);
        assertEquals("purchase", received.getAction());
        assertEquals("run-1", received.getExpectedRunId());
        assertEquals("coins_fragments", received.getOfferId());
        assertEquals(123L, received.getRequestId());
    }

    @Test public void snapshotRoundTripKeepsServerPricesAndAvailability() {
        RunExchangeSnapshotPacket sent = new RunExchangeSnapshotPacket();
        sent.requestId = 42L;
        sent.unlocked = true;
        sent.runId = "run-1";
        sent.fragments = "8.5";
        sent.crystals = "100";
        sent.response = "APPLIED";
        sent.addOffer(new RunExchangeSnapshotPacket.OfferEntry(
                "coins_fragments", "FRAGMENTS", "coins", "1.35", "+110 (10%)", 1, true, ""));
        sent.addOffer(new RunExchangeSnapshotPacket.OfferEntry(
                "shards_crystals", "CRYSTALS", "rune_shards", "—", "—", 0, false, "Нет забега"));
        ByteBuf bytes = Unpooled.buffer();
        sent.toBytes(bytes);
        RunExchangeSnapshotPacket received = new RunExchangeSnapshotPacket();
        received.fromBytes(bytes);
        assertEquals(42L, received.requestId);
        assertTrue(received.unlocked);
        assertEquals("run-1", received.runId);
        assertEquals("8.5", received.fragments);
        assertEquals("100", received.crystals);
        assertEquals("APPLIED", received.response);
        assertEquals(2, received.getOffers().size());
        assertEquals("1.35", received.getOffers().get(0).cost);
        assertEquals(1, received.getOffers().get(0).purchaseCount);
        assertTrue(received.getOffers().get(0).available);
        assertFalse(received.getOffers().get(1).available);
    }

    @Test public void duplicateAndDecreasingPurchaseIdsAreRejectedPerPlayer() {
        RunExchangeActionPacketHandler.clear();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertFalse(RunExchangeActionPacketHandler.acceptPurchase(a, 0L));
        assertTrue(RunExchangeActionPacketHandler.acceptPurchase(a, 100L));
        assertFalse(RunExchangeActionPacketHandler.acceptPurchase(a, 100L));
        assertFalse(RunExchangeActionPacketHandler.acceptPurchase(a, 99L));
        assertTrue(RunExchangeActionPacketHandler.acceptPurchase(b, 100L));
        assertTrue(RunExchangeActionPacketHandler.acceptPurchase(a, 101L));
        RunExchangeActionPacketHandler.clear();
    }
}
