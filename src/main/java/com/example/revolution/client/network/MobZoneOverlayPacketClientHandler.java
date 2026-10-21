package com.example.revolution.client.network;

import com.example.revolution.network.MobZoneOverlayPacket;
import com.example.revolution.network.MobZoneOverlayState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MobZoneOverlayPacketClientHandler implements IMessageHandler<MobZoneOverlayPacket, IMessage> {
    @Override
    public IMessage onMessage(MobZoneOverlayPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> MobZoneOverlayState.replace(message.getZones()));
        return null;
    }
}
