package com.example.revolution.client.network;

import com.example.revolution.network.GodWorshipActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side placeholder for the client-to-server God intent packet registration. */
public final class GodWorshipActionPacketClientHandler implements IMessageHandler<GodWorshipActionPacket, IMessage> {
    @Override public IMessage onMessage(GodWorshipActionPacket message, MessageContext ctx) { return null; }
}
