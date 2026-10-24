package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-side registration placeholder for the outbound travel-menu snapshot. */
public final class StageTravelMenuPacketServerHandler implements IMessageHandler<StageTravelMenuPacket, IMessage> {
    @Override public IMessage onMessage(StageTravelMenuPacket message, MessageContext ctx) { return null; }
}
