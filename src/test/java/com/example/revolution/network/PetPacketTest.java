package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PetPacketTest {
    @Test public void actionRoundTripCarriesIntentOnly() {
        PetActionPacket source = new PetActionPacket("awaken_pet", "pet_07");
        ByteBuf buffer = Unpooled.buffer();
        source.toBytes(buffer);
        PetActionPacket restored = new PetActionPacket();
        restored.fromBytes(buffer);
        assertEquals("awaken_pet", restored.getAction());
        assertEquals("pet_07", restored.getPetId());
    }

    @Test public void snapshotRoundTripPreservesAuthoritativePetState() {
        PetSnapshotPacket.Entry entry = new PetSnapshotPacket.Entry(
                "pet_03", "ADVANCED", "12345678901234567890", "42.75",
                true, true, true, 123456789L, "350", "DAMAGE_MULTIPLIER[CLICK]=0.2");
        PetSnapshotPacket source = new PetSnapshotPacket(8, 2, "17", Arrays.asList(entry));
        ByteBuf buffer = Unpooled.buffer();
        source.toBytes(buffer);
        PetSnapshotPacket restored = new PetSnapshotPacket();
        restored.fromBytes(buffer);

        assertEquals(8, restored.getNormalSlots());
        assertEquals(2, restored.getPremiumSlots());
        assertEquals("17", restored.getNextNormalSlotCost());
        assertEquals(1, restored.getEntries().size());
        PetSnapshotPacket.Entry actual = restored.getEntries().get(0);
        assertEquals("pet_03", actual.petId);
        assertEquals("ADVANCED", actual.rank);
        assertEquals("12345678901234567890", actual.level);
        assertEquals("42.75", actual.xp);
        assertTrue(actual.owned);
        assertTrue(actual.selected);
        assertTrue(actual.premiumSlot);
        assertEquals(123456789L, actual.cooldownUntil);
        assertEquals("350", actual.purchaseCost);
        assertEquals("DAMAGE_MULTIPLIER[CLICK]=0.2", actual.effects);
    }
}
