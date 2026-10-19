package com.example.revolution.network;

import com.example.revolution.rune.service.RunePacketFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import net.minecraftforge.fml.common.network.ByteBufUtils;

/** Pins the pre-cleanup wire layout independently of the renamed Java class names. */
public final class RuneTransportContractTest {
    public static void main(String[] args) {
        ByteBuf slot = Unpooled.buffer();
        ByteBuf expectedSlot = Unpooled.buffer();
        ByteBuf collections = Unpooled.buffer();
        ByteBuf expectedCollections = Unpooled.buffer();
        try {
            new RuneSlotPacket(-1, 25, 3, true).toBytes(slot);
            expectedSlot.writeBytes(new byte[]{-1,-1,-1,-1, 0,0,0,25, 0,0,0,3, 1});
            check(slot.equals(expectedSlot), "slot int/int/int/boolean encoding changed");
            RuneSlotPacket decodedSlot = new RuneSlotPacket();
            decodedSlot.fromBytes(expectedSlot);
            check(decodedSlot.getSlot() == -1 && decodedSlot.getRuneIndex() == 25
                    && decodedSlot.getLayer() == 3 && decodedSlot.isAnimate()
                    && !expectedSlot.isReadable(), "slot decoding changed");

            RunePacketFactory.collections(Arrays.asList("stored first name", "Other"), 1).toBytes(collections);
            expectedCollections.writeInt(2);
            ByteBufUtils.writeUTF8String(expectedCollections, "Яд");
            ByteBufUtils.writeUTF8String(expectedCollections, "Other");
            expectedCollections.writeInt(1);
            check(collections.equals(expectedCollections), "collection list/string/active-int encoding changed");
            RuneCollectionsPacket decodedCollections = new RuneCollectionsPacket();
            decodedCollections.fromBytes(expectedCollections);
            check(decodedCollections.getCollectionNames().equals(Arrays.asList("Яд", "Other"))
                    && decodedCollections.getActiveCollection() == 1 && !expectedCollections.isReadable(),
                    "collection decoding changed");
        } finally {
            slot.release(); expectedSlot.release(); collections.release(); expectedCollections.release();
        }
        System.out.println("RuneTransportContractTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
