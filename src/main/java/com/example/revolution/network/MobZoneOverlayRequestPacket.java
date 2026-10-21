package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client request for the current admin-visible mob-zone snapshot. */
public final class MobZoneOverlayRequestPacket implements IMessage {
    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}
}
