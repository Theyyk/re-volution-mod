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
            ClientRuneRanks.accept(message);
            CardSyncState.notePacket();

            if (message.isAnimate()
                    && RuneStatusRegistry.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId())
                    && message.getCardIndex() >= 0
                    && message.getCardIndex() < RuneInventory.BOOST_TYPE_COUNT) {
                RuneUxUiHandler.notePurchase(message.getCardIndex());
            }

            if (Minecraft.getMinecraft().currentScreen instanceof CardsGuiScreen) {
                if (RuneGuiPreloader.shouldSuppressLiveCard(message)) return;

                CardsGuiScreen screen = (CardsGuiScreen) Minecraft.getMinecraft().currentScreen;
                if (message.getSlot() == -1) {
                    screen.clearCards();
                } else {
                    // v1.2.9 intentionally removes the flying-card animation. The purchase feedback
                    // is now a short border pulse on the destination slot.
                    screen.addCardFromServer(message.getSlot(), message.getCardIndex(), message.getLayer(), false);
                }
            }
        });
        return null;
    }
}
