package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RuneSlotPacket implements IMessage {

    private int slot;
    private int runeIndex;
    private int layer;
    private boolean animate;

    public RuneSlotPacket() {}

    public RuneSlotPacket(int slot, int runeIndex, int layer, boolean animate) {
        this.slot = slot;
        this.runeIndex = runeIndex;
        this.layer = layer;
        this.animate = animate;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slot = buf.readInt();
        runeIndex = buf.readInt();
        layer = buf.readInt();
        animate = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slot);
        buf.writeInt(runeIndex);
        buf.writeInt(layer);
        buf.writeBoolean(animate);
    }

    public int getSlot() { return slot; }
    public int getRuneIndex() { return runeIndex; }
    public int getLayer() { return layer; }
    public boolean isAnimate() { return animate; }
}
