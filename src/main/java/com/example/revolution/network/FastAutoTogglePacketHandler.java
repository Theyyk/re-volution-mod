package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Applies the fast-auto preference only on the authoritative server thread. */
public final class FastAutoTogglePacketHandler implements IMessageHandler<FastAutoTogglePacket, IMessage> {
    @Override
    public IMessage onMessage(FastAutoTogglePacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServer().addScheduledTask(() ->
                ServerLifecycle.services().fastAuto.setEnabled(player.getUniqueID(), message.isEnabled()));
        return null;
    }
}
