package com.example.revolution.client.network;

import com.example.revolution.client.GodWorshipGuiScreen;
import com.example.revolution.network.GodWorshipSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Opens or refreshes the functional God Worship screen from an authoritative snapshot. */
public final class GodWorshipSnapshotPacketHandler implements IMessageHandler<GodWorshipSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(GodWorshipSnapshotPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            GodWorshipClientState.update(message);
            boolean alreadyOpen = minecraft.currentScreen instanceof GodWorshipGuiScreen;
            if (!alreadyOpen && !GodWorshipClientState.isOpenRequested()) return;
            if (alreadyOpen) ((GodWorshipGuiScreen) minecraft.currentScreen).suppressCloseSignal();
            minecraft.displayGuiScreen(new GodWorshipGuiScreen(
                    message.getEntries(),
                    message.getOfferings(),
                    message.getPermanentDurationLevel(),
                    message.getPermanentDurationBonusSeconds(),
                    message.getDurationUpgradeCost(),
                    message.isAutoActivationEnabled(),
                    message.isDurationUpgradeEnabled(),
                    message.isSecondaryBuffsEnabled()));
        });
        return null;
    }
}
