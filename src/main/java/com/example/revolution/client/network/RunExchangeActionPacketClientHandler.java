package com.example.revolution.client.network;

import com.example.revolution.network.RunExchangeActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client does not execute server Run Exchange mutations. */
public final class RunExchangeActionPacketClientHandler implements IMessageHandler<RunExchangeActionPacket, IMessage> {
    @Override public IMessage onMessage(RunExchangeActionPacket message, MessageContext ctx) { return null; }
}
