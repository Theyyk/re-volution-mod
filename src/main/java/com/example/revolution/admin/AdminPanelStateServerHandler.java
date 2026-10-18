package com.example.revolution.admin;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server-side registration placeholder: AdminPanelStatePacket only travels to the client. */
public final class AdminPanelStateServerHandler implements IMessageHandler<AdminPanelStatePacket, IMessage> {
    @Override
    public IMessage onMessage(AdminPanelStatePacket message, MessageContext ctx) {
        return null;
    }
}
