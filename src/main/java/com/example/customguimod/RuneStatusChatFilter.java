package com.example.customguimod;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RuneStatusChatFilter {

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.getMessage() == null) return;

        String text = event.getMessage().getUnformattedText();
        if (text != null && text.startsWith("Активная колода:")) {
            event.setCanceled(true);
        }
    }
}
