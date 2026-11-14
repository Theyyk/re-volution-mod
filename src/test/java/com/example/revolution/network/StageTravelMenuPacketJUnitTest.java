package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** UI-05 regression: the same server snapshot carries the Fast Auto acknowledgement. */
class StageTravelMenuPacketJUnitTest {
    @Test void snapshotRoundTripPreservesServerAvailabilityAndFastAutoPreference() {
        for (boolean enabled : new boolean[] {false, true}) {
            StageTravelMenuPacket original = new StageTravelMenuPacket(Arrays.asList(
                    new StageTravelMenuPacket.Entry(1, 1, true, true, false),
                    new StageTravelMenuPacket.Entry(1, 2, false, false, true)), enabled);
            ByteBuf bytes = Unpooled.buffer();
            try {
                original.toBytes(bytes);
                StageTravelMenuPacket decoded = new StageTravelMenuPacket();
                decoded.fromBytes(bytes);
                assertEquals(enabled, decoded.isFastAutoEnabled());
                assertEquals(2, decoded.getEntries().size());
                assertEquals(1, decoded.getEntries().get(0).getWorldLocationId());
                assertEquals(1, decoded.getEntries().get(0).getMobSlotId());
                assertTrue(decoded.getEntries().get(0).isUnlocked());
                assertTrue(decoded.getEntries().get(0).hasTransitionPoint());
                assertFalse(decoded.getEntries().get(1).isUnlocked());
                assertFalse(decoded.getEntries().get(1).hasTransitionPoint());
                assertTrue(decoded.getEntries().get(1).isBoss());
                assertFalse(bytes.isReadable());
            } finally {
                bytes.release();
            }
        }
    }
}
