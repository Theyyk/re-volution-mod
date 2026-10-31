package com.example.revolution.client.network;

import com.example.revolution.client.QuestGuiScreen;
import com.example.revolution.network.QuestSnapshotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Opens or refreshes the functional Quest board from a server-authoritative snapshot. */
public final class QuestSnapshotPacketHandler implements IMessageHandler<QuestSnapshotPacket, IMessage> {
    @Override public IMessage onMessage(QuestSnapshotPacket message, MessageContext ctx) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new QuestGuiScreen(
                message.getEntries(), message.getCooldownUntil(), message.getRerollsRemaining(), message.getCompletedQuestCount())));
        return null;
    }
}
