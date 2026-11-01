package com.example.revolution.client.network;

import com.example.revolution.network.UniqueRuneActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Physical-client placeholder for the C2S Unique Rune intent discriminator. */
public final class UniqueRuneActionPacketClientHandler implements IMessageHandler<UniqueRuneActionPacket, IMessage> {
    @Override public IMessage onMessage(UniqueRuneActionPacket message, MessageContext ctx) { return null; }
}
