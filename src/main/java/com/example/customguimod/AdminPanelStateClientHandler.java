package com.example.customguimod;

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
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().currentScreen instanceof AdminPanelScreen) {
                ((AdminPanelScreen) Minecraft.getMinecraft().currentScreen).applyState(message);
            } else {
                Minecraft.getMinecraft().displayGuiScreen(new AdminPanelScreen(message));
            }
        });
        return null;
    }
}
