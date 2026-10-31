package com.example.revolution.client.network;

import com.example.revolution.client.ClientAchievements;
import com.example.revolution.network.AchievementSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class AchievementSnapshotPacketHandler implements IMessageHandler<AchievementSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(AchievementSnapshotPacket message, MessageContext ctx) {
        if (message == null) return null;
        Minecraft.getMinecraft().addScheduledTask(() -> ClientAchievements.accept(message));
        return null;
    }
}
