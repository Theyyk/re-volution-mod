package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CardPacketHandler implements IMessageHandler<CardPacket, IMessage> {

    @Override
    public IMessage onMessage(CardPacket message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            RuneGuiPreloader.acceptCard(message);
            CardSyncState.notePacket();

            if (Minecraft.getMinecraft().currentScreen instanceof CardsGuiScreen) {
                CardsGuiScreen screen = (CardsGuiScreen) Minecraft.getMinecraft().currentScreen;
                if (message.getSlot() == -1) {
                    screen.clearCards();
                } else {
                    screen.addCardFromServer(message.getSlot(), message.getCardIndex(), message.getLayer(), message.isAnimate());
                }
            }
        });
        return null;
    }
}
