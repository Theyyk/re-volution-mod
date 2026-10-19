package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Forge adapter: accepted requests are executed on the server thread. */
public final class PingPacketHandler implements IMessageHandler<PingPacket, IMessage> {
    @Override public IMessage onMessage(PingPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> ServerLifecycle.services().runeActions.handle(player, message.getAction()));
        return null;
    }
}
