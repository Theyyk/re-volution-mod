package com.example.revolution.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Projection is server-to-client only; reject any serverbound write. */
public final class CombatProjectionPacketServerHandler implements IMessageHandler<CombatProjectionPacket, IMessage> {
    @Override public IMessage onMessage(CombatProjectionPacket message, MessageContext ctx) { return null; }
}
