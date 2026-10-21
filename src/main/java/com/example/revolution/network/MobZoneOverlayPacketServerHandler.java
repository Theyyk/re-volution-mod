package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Dedicated-server registration placeholder for the server-to-client overlay packet. */
public final class MobZoneOverlayPacketServerHandler implements IMessageHandler<MobZoneOverlayPacket, IMessage> {
    @Override public IMessage onMessage(MobZoneOverlayPacket message, MessageContext ctx) { return null; }
}
