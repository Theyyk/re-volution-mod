package com.example.revolution.network;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** E projection is server-authored and transported without the client recomputing effect layers. */
public final class PlayerStatsDamageProjectionPacketTest {
    @Test public void roundTripIncludesCurrentDamageAndSourceDetails() {
        PlayerStatsPacket packet = new PlayerStatsPacket(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, 0, "coins", 10, "", 0, 0, 1.08D, 2.5D);
        packet.setDamageProjection(1350000.75D, 275000D,
                "Руны: 1.08\nMastery +2250\nPets +0.1", "Яд: 275000");
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.toBytes(buf);
            PlayerStatsPacket decoded = new PlayerStatsPacket();
            decoded.fromBytes(buf);
            assertEquals(1.08D, decoded.getResolvedClickDamage(), 0.000001D);
            assertEquals(1350000.75D, decoded.getProjectedClickDamage(), 0.000001D);
            assertEquals(275000D, decoded.getProjectedPoisonDamage(), 0.000001D);
            assertTrue(decoded.getClickBreakdown().contains("Mastery"));
            assertTrue(decoded.getClickBreakdown().contains("Pets"));
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }
}
