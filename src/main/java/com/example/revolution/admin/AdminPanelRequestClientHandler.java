package com.example.revolution.admin;

import com.example.revolution.AdminPanelRequestPacket;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-side registration placeholder: AdminPanelRequestPacket only travels to the server. */
public final class AdminPanelRequestClientHandler implements IMessageHandler<AdminPanelRequestPacket, IMessage> {
    @Override
    public IMessage onMessage(AdminPanelRequestPacket message, MessageContext ctx) {
        return null;
    }
}
