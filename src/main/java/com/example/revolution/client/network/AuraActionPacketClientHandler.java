package com.example.revolution.client.network;

import com.example.revolution.network.AuraActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Physical-client placeholder for the server-bound Aura action discriminator. */
public final class AuraActionPacketClientHandler implements IMessageHandler<AuraActionPacket, IMessage> {
    @Override public IMessage onMessage(AuraActionPacket message, MessageContext ctx) { return null; }
}
