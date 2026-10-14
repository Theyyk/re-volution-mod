package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RuneEnhancementPacket implements IMessage {
    private int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];

    public RuneEnhancementPacket() {}

    public RuneEnhancementPacket(int[] source) {
        if (source == null) return;
        int limit = Math.min(levels.length, source.length);
        for (int i = 0; i < limit; i++) levels[i] = Math.max(0, source[i]);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = Math.max(0, Math.min(RuneInventory.BOOST_TYPE_COUNT, buf.readInt()));
        levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (int i = 0; i < count; i++) levels[i] = Math.max(0, buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(levels.length);
        for (int level : levels) buf.writeInt(Math.max(0, level));
    }

    public int[] getLevels() {
        return levels.clone();
    }
}
