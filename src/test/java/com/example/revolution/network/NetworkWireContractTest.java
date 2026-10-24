package com.example.revolution.network;

import com.example.revolution.rune.service.RunePacketFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.math.BigDecimal;
import java.util.Arrays;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Independent expected byte streams for stable registered payloads; IDs 5/6 remain reserved. */
public final class NetworkWireContractTest {
    public static void main(String[] args) {
        ByteBuf expected = Unpooled.buffer();
        try {
            ByteBufUtils.writeUTF8String(expected, "enhance_rune:0:100");
            verify(new PingPacket("enhance_rune:0:100"), new PingPacket(), expected);
            expected.writeInt(12345);
            verify(new PongPacket(12345), new PongPacket(), expected);

            for (String value : new String[]{"100000000000000000000", "2", "3", "4", "5"}) ByteBufUtils.writeUTF8String(expected, value);
            expected.writeInt(6).writeInt(1);
            ByteBufUtils.writeUTF8String(expected, "coins"); expected.writeInt(100);
            ByteBufUtils.writeUTF8String(expected, "rune_dust"); expected.writeInt(40).writeInt(80);
            expected.writeDouble(12.5D).writeDouble(3.25D);
            verify(new PlayerStatsPacket(new BigDecimal("1E20"), BigDecimal.valueOf(2), BigDecimal.valueOf(3),
                    BigDecimal.valueOf(4), BigDecimal.valueOf(5), 6, 1, "coins", 100, "rune_dust", 40, 80, 12.5D, 3.25D),
                    new PlayerStatsPacket(), expected);

            expected.writeInt(1); ByteBufUtils.writeUTF8String(expected, "rune_dust"); expected.writeDouble(0.1D).writeInt(26);
            for (int rune = 0; rune < 26; rune++) {
                expected.writeInt(0).writeInt(10).writeInt(50).writeInt(100).writeInt(1000);
            }
            verify(RunePacketFactory.enhancements(new int[26]), new RuneEnhancementPacket(), expected);

            verify(new StageTravelMenuRequestPacket(), new StageTravelMenuRequestPacket(), expected);

            expected.writeInt(2);
            expected.writeInt(1).writeInt(1).writeBoolean(true).writeBoolean(true);
            expected.writeInt(1).writeInt(2).writeBoolean(false).writeBoolean(true);
            verify(new StageTravelMenuPacket(Arrays.asList(
                    new StageTravelMenuPacket.Entry(1, 1, true, true),
                    new StageTravelMenuPacket.Entry(1, 2, false, true))), new StageTravelMenuPacket(), expected);

            expected.writeInt(2).writeInt(3);
            verify(new StageTravelTeleportRequestPacket(2, 3), new StageTravelTeleportRequestPacket(), expected);
        } finally { expected.release(); }
        System.out.println("Network byte goldens and decoder round trips passed for IDs 0/1/4/7/10/11/12; IDs 5/6 remain reserved");
    }

    private static void verify(IMessage message, IMessage decoded, ByteBuf expected) {
        ByteBuf actual = Unpooled.buffer();
        ByteBuf roundTrip = Unpooled.buffer();
        try {
            message.toBytes(actual);
            if (!actual.equals(expected)) throw new AssertionError(message.getClass().getSimpleName() + " wire encoding changed");
            decoded.fromBytes(actual);
            if (actual.isReadable()) throw new AssertionError("Unconsumed packet bytes");
            decoded.toBytes(roundTrip);
            if (!roundTrip.equals(expected)) throw new AssertionError("Decoder changed the payload");
            expected.clear();
        } finally { actual.release(); roundTrip.release(); }
    }
}
