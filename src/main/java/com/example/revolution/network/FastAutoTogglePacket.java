package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client request to set the player's persistent fast-auto preference. */
public final class FastAutoTogglePacket implements IMessage {
    private boolean enabled;

    public FastAutoTogglePacket() {}
    public FastAutoTogglePacket(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    @Override public void fromBytes(ByteBuf buf) { enabled = buf.readBoolean(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeBoolean(enabled); }
}
