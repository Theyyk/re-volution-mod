package com.example.revolution.client.network;

import com.example.revolution.network.MasteryActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Physical-client stub: Mastery actions always target the server. */
public final class MasteryActionPacketClientHandler implements IMessageHandler<MasteryActionPacket, IMessage> {
    @Override public IMessage onMessage(MasteryActionPacket message, MessageContext ctx) { return null; }
}
