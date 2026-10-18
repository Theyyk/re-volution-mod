package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RuneCollectionsPacketHandler implements IMessageHandler<RuneCollectionsPacket, IMessage> {

    @Override
    public IMessage onMessage(RuneCollectionsPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            RuneGuiPreloader.acceptCollections(message.getCollectionNames(), message.getActiveCollection());

            if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
                RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
                screen.setCollections(message.getCollectionNames(), message.getActiveCollection());
            }
        });
        return null;
    }
}
