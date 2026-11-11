package com.example.revolution.network;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.prestige.MasteryResetMode;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeResetService;
import com.example.revolution.prestige.PrestigeState;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** All Mastery mutations execute on the server thread and reuse the canonical PrestigeService. */
public final class MasteryActionPacketHandler implements IMessageHandler<MasteryActionPacket, IMessage> {
    private static final ConcurrentMap<UUID, Long> LAST_MUTATION = new ConcurrentHashMap<>();

    @Override public IMessage onMessage(MasteryActionPacket request, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player != null && request != null)
            player.getServerWorld().addScheduledTask(() -> handle(player, request));
        return null;
    }

    private static void handle(EntityPlayerMP player, MasteryActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        UUID playerId = player.getUniqueID();
        String action = request.getAction();
        String response = "";
        String previewKind = "";
        String previewDetails = "";
        boolean unlocked = services.mechanicUnlocks.isUnlocked(playerId, BalanceMechanicUnlockPolicy.ARTS);
        if (!"sync".equals(action) && !unlocked) {
            response = "ARTS_LOCKED";
        } else if ("sync".equals(action)) {
            // Read-only query.
        } else if (!known(action)) {
            response = "UNKNOWN_ACTION";
        } else if (isMutation(action) && !accept(playerId, request.getRequestId())) {
            response = "REPLAY_REJECTED";
        } else {
            try {
                PrestigeState.RunState active = services.prestige.activeRun(playerId);
                String expected = request.getExpectedRunId();
                if ("start".equals(action)) {
                    PrestigeBranch branch = PrestigeBranch.valueOf(request.getBranch());
                    if (!services.prestige.branches().contains(branch)) throw new IllegalStateException("BRANCH_UNAVAILABLE");
                    services.prestige.startRun(playerId, branch);
                    response = "RUN_STARTED";
                } else {
                    if (active == null) throw new IllegalStateException("NO_ACTIVE_RUN");
                    if (!active.runId.equals(expected)) throw new IllegalStateException("STALE_RUN");
                    if ("collect".equals(action)) {
                        BigDecimal collected = services.prestige.collectFragments(
                                playerId, expected, com.example.revolution.prestige.PrestigeService.FragmentSource.MASTERY);
                        response = "COLLECTED:" + exact(collected);
                    } else if ("preview_reset".equals(action)) {
                        PrestigeResetService.ResetPreview preview = services.prestige.previewReset(playerId, expected);
                        previewKind = "reset";
                        previewDetails = "Сохранится: " + exact(preview.savedCandidate) +
                                " | Лучшее: " + exact(preview.newBest) +
                                " | Meta: +" + preview.metaLevelGainPerRun +
                                " | Ранги: " + preview.ranksUpgraded;
                    } else if ("reset".equals(action)) {
                        response = "RESET_" + services.prestige.reset(playerId, expected).name();
                    } else if ("preview_mastery".equals(action)) {
                        PrestigeResetService.MasteryResetPreview preview = services.prestige.previewMasteryReset(playerId, expected);
                        previewKind = "mastery";
                        previewDetails = "Tier " + preview.currentMasteryTier + " -> " + preview.nextMasteryTier +
                                " | Meta cap " + preview.currentRankMetaLevelCap + " -> " + preview.nextRankMetaLevelCap +
                                " | Сброс рун и питомцев по правилам сервера";
                    } else if ("mastery".equals(action)) {
                        response = "MASTERY_" + services.prestige.masteryReset(playerId, expected, MasteryResetMode.NORMAL).name();
                    } else if ("preview_premium".equals(action)) {
                        PrestigeResetService.MasteryResetPreview preview = services.prestige.previewMasteryReset(playerId, expected);
                        if (!preview.premiumPreserveEnabled) throw new IllegalStateException("OPTION_UNAVAILABLE");
                        previewKind = "premium";
                        previewDetails = "Tier " + preview.currentMasteryTier + " -> " + preview.nextMasteryTier +
                                " | Crystals: " + preview.premiumPreserveCrystalCost +
                                " | Сохранить руны: " + preview.premiumPreserveRunes +
                                " | питомцев: " + preview.premiumPreservePets;
                    } else if ("premium".equals(action)) {
                        response = "PREMIUM_" + services.prestige.masteryReset(playerId, expected, MasteryResetMode.PREMIUM_PRESERVE).name();
                    }
                }
            } catch (IllegalArgumentException invalid) {
                response = "INVALID_BRANCH_OR_OPTION";
            } catch (IllegalStateException rejected) {
                response = rejected.getMessage() == null ? "SERVER_REJECTED" : rejected.getMessage();
            }
        }
        sendSnapshot(player, services, request.getRequestId(), response, previewKind, previewDetails);
    }

    private static boolean known(String action) {
        return "start".equals(action) || "collect".equals(action)
                || "preview_reset".equals(action) || "reset".equals(action)
                || "preview_mastery".equals(action) || "mastery".equals(action)
                || "preview_premium".equals(action) || "premium".equals(action);
    }
    private static boolean isMutation(String action) {
        return "start".equals(action) || "collect".equals(action) || "reset".equals(action)
                || "mastery".equals(action) || "premium".equals(action);
    }
    /** Strictly monotonic per-player replay rejection; startRun also forbids multiple active runs. */
    static boolean accept(UUID playerId, long nonce) {
        if (nonce <= 0L) return false;
        for (;;) {
            Long last = LAST_MUTATION.get(playerId);
            if (last != null && nonce <= last) return false;
            if (last == null ? LAST_MUTATION.putIfAbsent(playerId, nonce) == null
                    : LAST_MUTATION.replace(playerId, last, nonce)) return true;
        }
    }
    public static void clear() { LAST_MUTATION.clear(); }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services, long requestId,
                                    String response, String previewKind, String previewDetails) {
        UUID id = player.getUniqueID();
        MasterySnapshotPacket packet = new MasterySnapshotPacket();
        packet.requestId = requestId;
        packet.artsUnlocked = services.mechanicUnlocks.isUnlocked(id, BalanceMechanicUnlockPolicy.ARTS);
        packet.response = response;
        packet.previewKind = previewKind;
        packet.previewDetails = previewDetails;
        for (MechanicUnlockService.EntrySnapshot unlock : services.mechanicUnlocks.snapshot(id)) {
            if (BalanceMechanicUnlockPolicy.ARTS.equals(unlock.definition.unlockId)) {
                packet.unlockRequirement = unlock.definition.rarity.name() + " "
                        + unlock.progress + "/" + unlock.definition.target
                        + (unlock.eligible ? " | Можно разблокировать в разделе рун" : "");
                break;
            }
        }
        if (!packet.artsUnlocked && packet.unlockRequirement.isEmpty())
            packet.unlockRequirement = "Проверить требования ARTS в разблокировках";
        PrestigeState state = services.prestige.snapshot(id);
        for (PrestigeBranch branch : services.prestige.branches()) {
            PrestigeState.BranchState permanent = state.branch(branch);
            int gain = 0;
            try { gain = services.prestige.metaLevelGainPerRun(id, branch); }
            catch (IllegalStateException ignored) { }
            packet.addBranch(new MasterySnapshotPacket.BranchEntry(branch.name(),
                    services.prestige.progressSource(branch), exact(permanent.bestSavedProgress),
                    permanent.masteryTier, permanent.rankMetaLevelCap, gain));
        }
        PrestigeState.RunState run = state.activeRun;
        if (run != null) {
            packet.runId = run.runId;
            packet.activeBranch = run.activeBranch.name();
            packet.progress = exact(run.branchProgress);
            packet.level = run.branchLevel;
            packet.fragments = exact(run.runBranchCurrency);
            packet.masteryPending = exact(run.masteryPendingFragments);
            packet.petPending = exact(run.petPendingFragments);
            packet.ranks = String.join(", ", run.reachedRanks);
            packet.startedAt = run.startedAt;
            packet.masteryResetAvailable = services.prestige.masteryResetAvailable(id);
            if (packet.masteryResetAvailable) {
                try {
                    PrestigeResetService.MasteryResetPreview preview = services.prestige.previewMasteryReset(id, run.runId);
                    packet.premiumPreserveAvailable = preview.premiumPreserveEnabled;
                    packet.premiumCrystalCost = preview.premiumPreserveCrystalCost;
                } catch (IllegalStateException ignored) { }
            }
        }
        NetworkHandler.sendToClient(packet, player);
    }
    private static String exact(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
