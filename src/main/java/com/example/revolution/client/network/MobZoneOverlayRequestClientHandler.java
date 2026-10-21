package com.example.revolution.client.network;

import com.example.revolution.network.MobZoneOverlayRequestPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side discriminator registration placeholder for the outbound request packet. */
public final class MobZoneOverlayRequestClientHandler implements IMessageHandler<MobZoneOverlayRequestPacket, IMessage> {
    @Override public IMessage onMessage(MobZoneOverlayRequestPacket message, MessageContext ctx) { return null; }
}
