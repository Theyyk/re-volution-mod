package com.example.revolution.network.admin;

import com.example.revolution.lifecycle.ServerLifecycle;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class AdminPanelRequestHandler implements IMessageHandler<AdminPanelRequestPacket, IMessage> {
    @Override public IMessage onMessage(AdminPanelRequestPacket message, MessageContext ctx) {
        EntityPlayerMP admin = ctx.getServerHandler().player;
        if (admin == null || message == null) return null;
        admin.getServerWorld().addScheduledTask(() -> ServerLifecycle.services().adminPanel.handle(admin,
                message.getAction() == null ? "" : message.getAction()));
        return null;
    }
}
