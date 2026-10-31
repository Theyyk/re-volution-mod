package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server placeholder; the physical client registers the real God snapshot handler. */
public final class GodWorshipSnapshotPacketServerHandler implements IMessageHandler<GodWorshipSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(GodWorshipSnapshotPacket message, MessageContext ctx) { return null; }
}
