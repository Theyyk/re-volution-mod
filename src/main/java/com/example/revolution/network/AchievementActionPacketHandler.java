package com.example.revolution.network;

import com.example.revolution.achievement.AchievementDefinition;
import com.example.revolution.achievement.AchievementPlayerState;
import com.example.revolution.achievement.AchievementStage;
import com.example.revolution.achievement.AchievementStageLabels;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Player-facing access is gated; background factual achievement tracking remains active. */
public final class AchievementActionPacketHandler implements IMessageHandler<AchievementActionPacket, IMessage> {
    @Override public IMessage onMessage(AchievementActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, AchievementActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.ACHIEVEMENTS)) return;
        String action = request.getAction();
        if ("sync".equals(action)) { sendSnapshot(player, services); return; }
        if ("claim".equals(action)) {
            if (!AchievementRequestLimiter.acceptAction(player.getUniqueID(), request.getRequestNonce())) return;
            services.achievements.claim(player.getUniqueID(), request.getAchievementId(), request.getExpectedStageIndex());
            sendSnapshot(player, services); return;
        }
        if ("share".equals(action)) {
            if (!AchievementRequestLimiter.acceptAction(player.getUniqueID(), request.getRequestNonce())) return;
            share(player, services, request.getAchievementId());
        }
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) {
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.ACHIEVEMENTS)) return;
        AchievementPlayerState state = services.achievements.snapshot(player.getUniqueID());
        List<AchievementSnapshotPacket.Entry> entries = new ArrayList<>(); int hiddenOrdinal = 0;
        for (AchievementDefinition definition : services.achievements.catalog().all()) {
            boolean revealed = !definition.hidden || state.revealedHiddenAchievements.contains(definition.achievementId);
            if (!revealed) { AchievementSnapshotPacket.Entry hidden = new AchievementSnapshotPacket.Entry(); hidden.achievementId = "hidden:" + (++hiddenOrdinal); hidden.name = "???"; hidden.category = "HIDDEN"; hidden.hidden = true; hidden.revealed = false; entries.add(hidden); continue; }
            entries.add(entry(state, definition));
        }
        long points = state.achievementPoints;
        NetworkHandler.sendToClient(new AchievementSnapshotPacket(entries, points, services.achievementPolicy.level(points), exact(services.achievementPolicy.bonusPercent(points)), exact(services.achievementPolicy.nextLevelThreshold(points))), player);
    }

    private static AchievementSnapshotPacket.Entry entry(AchievementPlayerState state, AchievementDefinition definition) {
        AchievementSnapshotPacket.Entry out = new AchievementSnapshotPacket.Entry(); int claimed = state.claimedStage(definition.achievementId); BigDecimal progress = state.progress(definition.achievementId); int completed = completed(definition, progress); int displayStage = claimed < definition.stages.size() ? claimed + 1 : definition.stages.size(); AchievementStage stage = definition.stage(displayStage);
        out.achievementId = definition.achievementId; out.name = definition.name; out.category = definition.category.name(); out.stageIndex = stage == null ? 0 : stage.stageIndex; out.stageRoman = stage == null ? "" : AchievementStageLabels.roman(stage.stageIndex); out.progress = exact(progress); out.target = stage == null ? "0" : exact(stage.target); out.crystals = stage == null ? "0" : exact(stage.crystals); out.offerings = stage == null ? "0" : exact(stage.offerings); out.auraKeys = stage == null ? "0" : exact(stage.auraKeys); out.achievementPoints = stage == null ? 0L : stage.achievementPoints; out.highestCompletedStage = completed; out.highestClaimedStage = claimed; out.completed = stage != null && progress.compareTo(stage.target) >= 0; out.claimed = stage != null && claimed >= stage.stageIndex; out.finalStage = stage != null && stage.stageIndex == definition.stages.size(); out.hidden = definition.hidden; out.revealed = true; return out;
    }
    private static int completed(AchievementDefinition definition, BigDecimal progress) { int completed = 0; for (AchievementStage stage : definition.stages) { if (progress.compareTo(stage.target) < 0) break; completed = stage.stageIndex; } return completed; }
    private static void share(EntityPlayerMP player, ServerServices services, String achievementId) {
        AchievementDefinition definition = services.achievements.catalog().get(achievementId); if (definition == null) return; AchievementPlayerState state = services.achievements.snapshot(player.getUniqueID()); if (definition.hidden && !state.revealedHiddenAchievements.contains(achievementId)) return; int claimed = state.claimedStage(achievementId); int stageIndex = claimed < definition.stages.size() ? claimed + 1 : definition.stages.size(); AchievementStage stage = definition.stage(stageIndex); if (stage == null) return; BigDecimal progress = state.progress(achievementId); boolean completed = progress.compareTo(stage.target) >= 0; boolean stageClaimed = claimed >= stageIndex;
        TextComponentString tag = new TextComponentString("[Achievement: " + definition.name + " " + AchievementStageLabels.roman(stage.stageIndex) + "]"); TextComponentString tooltip = new TextComponentString(definition.name + " " + AchievementStageLabels.roman(stage.stageIndex) + "\nProgress: " + exact(progress) + " / " + exact(stage.target) + "\nCompleted: " + completed + "\nClaimed: " + stageClaimed); tag.setStyle(new Style().setColor(TextFormatting.GOLD).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))); TextComponentString message = new TextComponentString(player.getName() + " "); message.appendSibling(tag); message.appendText(" " + exact(progress) + " / " + exact(stage.target)); if (player.getServer() != null) player.getServer().getPlayerList().sendMessage(message);
    }
    private static String exact(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
}
