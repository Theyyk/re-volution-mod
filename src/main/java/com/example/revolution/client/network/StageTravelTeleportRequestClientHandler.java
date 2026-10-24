package com.example.revolution.client.network;

import com.example.revolution.network.StageTravelTeleportRequestPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side registration placeholder for the outbound teleport request. */
public final class StageTravelTeleportRequestClientHandler implements IMessageHandler<StageTravelTeleportRequestPacket, IMessage> {
    @Override public IMessage onMessage(StageTravelTeleportRequestPacket message, MessageContext ctx) { return null; }
}
