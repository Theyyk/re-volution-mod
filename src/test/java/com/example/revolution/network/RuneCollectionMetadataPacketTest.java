package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;

public final class RuneCollectionMetadataPacketTest {
    public static void main(String[] args) {
        RuneCollectionMetadataPacket source = new RuneCollectionMetadataPacket(
                Arrays.asList("collection_01", "collection_02", "collection_03", "collection_04", "collection_05", "collection_06", "collection_07"),
                Arrays.asList("Яд", "Collection 02", "Collection 03", "Collection 04", "Collection 05", "Collection 06", "Collection 07"),
                0b0000011,
                1,
                Arrays.asList(26, 7, 0, 0, 0, 0, 0),
                13);
        ByteBuf buf = Unpooled.buffer();
        try {
            source.toBytes(buf);
            RuneCollectionMetadataPacket decoded = new RuneCollectionMetadataPacket();
            decoded.fromBytes(buf);
            check(decoded.getCollectionIds().size() == 7, "fixed collection count changed");
            check(decoded.getDisplayNames().size() == 7, "display-name count changed");
            check(decoded.isUnlocked(0) && decoded.isUnlocked(1) && !decoded.isUnlocked(2), "unlock mask changed");
            check(decoded.getActiveCollection() == 1, "active collection changed");
            check(decoded.getImprovedOrHigherProgress().equals(Arrays.asList(26, 7, 0, 0, 0, 0, 0)), "factual progress changed");
            check(decoded.getUnlockThreshold() == 13, "unlock threshold changed");
            check(!buf.isReadable(), "metadata packet left unread bytes");
        } finally {
            buf.release();
        }
        System.out.println("RuneCollectionMetadataPacketTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
