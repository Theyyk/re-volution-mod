package com.example.revolution.client.network;

import com.example.revolution.network.StageTravelMenuRequestPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side registration placeholder for the outbound menu request. */
public final class StageTravelMenuRequestClientHandler implements IMessageHandler<StageTravelMenuRequestPacket, IMessage> {
    @Override public IMessage onMessage(StageTravelMenuRequestPacket message, MessageContext ctx) { return null; }
}
