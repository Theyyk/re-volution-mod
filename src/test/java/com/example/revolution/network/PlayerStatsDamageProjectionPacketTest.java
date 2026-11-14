package com.example.revolution.network;

import static org.junit.jupiter.api.Assertions.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/** New E projection has its own packet; legacy PlayerStatsPacket goldens stay byte-exact. */
public final class PlayerStatsDamageProjectionPacketTest {
    @Test public void roundTripIncludesCurrentDamageAndSourceDetails() {
        CombatProjectionPacket packet = new CombatProjectionPacket(1350000.75D, 275000D,
                "Руны: 1.08\\nMastery +2250\\nPets +0.1", "Яд: 275000");
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.toBytes(buf);
            CombatProjectionPacket decoded = new CombatProjectionPacket();
            decoded.fromBytes(buf);
            assertEquals(1350000.75D, decoded.getClick(), 0.000001D);
            assertEquals(275000D, decoded.getPoison(), 0.000001D);
            assertTrue(decoded.getClickDetails().contains("Mastery"));
            assertTrue(decoded.getClickDetails().contains("Pets"));
            assertFalse(buf.isReadable());
        } finally { buf.release(); }
    }
}
