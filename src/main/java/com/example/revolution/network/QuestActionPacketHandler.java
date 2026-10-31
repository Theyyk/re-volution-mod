package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.quest.state.QuestEntry;
import com.example.revolution.quest.state.QuestPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server adapter for Quest UI intents. */
public final class QuestActionPacketHandler implements IMessageHandler<QuestActionPacket, IMessage> {
    @Override public IMessage onMessage(QuestActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, QuestActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        String action = request.getAction();
        if ("claim".equals(action)) services.quests.claim(player.getUniqueID(), request.getEntryId());
        else if ("reroll".equals(action)) services.quests.reroll(player.getUniqueID());
        else if (!"sync".equals(action)) return;
        sendSnapshot(player, services);
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        QuestPlayerState state = services.quests.state(player.getUniqueID());
        List<QuestSnapshotPacket.Entry> entries = new ArrayList<>();
        for (QuestEntry entry : state.boardEntries) {
            entries.add(new QuestSnapshotPacket.Entry(
                    entry.entryId,
                    entry.templateId,
                    entry.eventType == null ? "" : entry.eventType.name(),
                    exact(entry.progress),
                    exact(entry.target),
                    entry.state == null ? "ACTIVE" : entry.state.name(),
                    entry.crystalReward,
                    entry.difficultyTier));
        }
        int dailyLimit = 2;
        try { dailyLimit = Integer.parseInt(services.balance.getActiveValue("quest.reroll_daily_limit")); }
        catch (RuntimeException ignored) { }
        NetworkHandler.sendToClient(new QuestSnapshotPacket(
                entries,
                state.batchCooldownUntil,
                Math.max(0, dailyLimit - state.rerollsUsed),
                state.completedQuestCount), player);
    }

    private static String exact(java.math.BigDecimal value) {
        return value == null || value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
