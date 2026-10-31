package com.example.revolution.client.network;

import com.example.revolution.network.AchievementActionPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Physical-client placeholder for the C2S Achievement intent discriminator. */
public final class AchievementActionPacketClientHandler implements IMessageHandler<AchievementActionPacket, IMessage> {
    @Override public IMessage onMessage(AchievementActionPacket message, MessageContext ctx) { return null; }
}
