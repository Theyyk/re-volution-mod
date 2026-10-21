package com.example.revolution.network;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MobZoneOverlayPacketHandler implements IMessageHandler<MobZoneOverlayPacket, IMessage> {
    @Override
    public IMessage onMessage(MobZoneOverlayPacket message, MessageContext ctx) {
        FMLCommonHandler.instance().getWorldThread(ctx.netHandler)
                .addScheduledTask(() -> MobZoneOverlayState.replace(message.getZones()));
        return null;
    }
}
