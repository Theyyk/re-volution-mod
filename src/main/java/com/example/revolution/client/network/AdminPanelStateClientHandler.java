package com.example.revolution.client.network;

import com.example.revolution.client.ClientUiBridge;
import com.example.revolution.network.admin.AdminPanelStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AdminPanelStateClientHandler implements IMessageHandler<AdminPanelStatePacket, IMessage> {
    @Override
    public IMessage onMessage(AdminPanelStatePacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientUiBridge.applyAdminPanelState(message));
        return null;
    }
}
