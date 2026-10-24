package com.example.revolution.client.network;

import com.example.revolution.network.FastAutoTogglePacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side registration counterpart; toggle packets are only sent client to server. */
public final class FastAutoTogglePacketClientHandler implements IMessageHandler<FastAutoTogglePacket, IMessage> {
    @Override public IMessage onMessage(FastAutoTogglePacket message, MessageContext ctx) { return null; }
}
