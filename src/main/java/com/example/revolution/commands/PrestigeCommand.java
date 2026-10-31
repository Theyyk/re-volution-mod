package com.example.revolution.commands;

import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.prestige.MasteryResetMode;
import com.example.revolution.prestige.PrestigeBranch;
import com.example.revolution.prestige.PrestigeResetService;
import com.example.revolution.prestige.PrestigeService;
import com.example.revolution.prestige.PrestigeState;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/** Mechanics-first Mastery skeleton UI. Command name remains /prestige for compatibility. */
public final class PrestigeCommand extends CommandBase {
    private final PrestigeService prestige;
    public PrestigeCommand(PrestigeService prestige) { this.prestige = prestige; }
    @Override public String getName() { return "prestige"; }
    @Override public String getUsage(ICommandSender sender) { return "/prestige branches|select <branch>|status|collect|reset [confirm <runId>]|mastery-reset [confirm <runId> [normal|premium]]|diagnose"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender); String action = args.length == 0 ? "status" : args[0].toLowerCase();
        try {
            if (action.equals("branches")) { showBranches(player); return; }
            if (action.equals("select")) { if (args.length != 2) throw new WrongUsageException("/prestige select <branch>"); PrestigeState.RunState run = prestige.startRun(player.getUniqueID(), PrestigeBranch.valueOf(args[1].toUpperCase())); message(player, "§aMastery run started: " + run.activeBranch + " | runId=" + run.runId); return; }
            if (action.equals("status")) { showStatus(player); return; }
            if (action.equals("collect")) { collect(player); return; }
            if (action.equals("reset")) { if (args.length == 1) { showResetPreview(player); return; } if (args.length == 3 && args[1].equalsIgnoreCase("confirm")) { confirmReset(player, args[2]); return; } throw new WrongUsageException("/prestige reset OR /prestige reset confirm <runId>"); }
            if (action.equals("mastery-reset")) {
                if (args.length == 1) { showMasteryResetPreview(player); return; }
                if ((args.length == 3 || args.length == 4) && args[1].equalsIgnoreCase("confirm")) {
                    MasteryResetMode mode = args.length == 4 ? MasteryResetMode.parse(args[3]) : MasteryResetMode.NORMAL;
                    confirmMasteryReset(player, args[2], mode); return;
                }
                throw new WrongUsageException("/prestige mastery-reset OR /prestige mastery-reset confirm <runId> [normal|premium]");
            }
            if (action.equals("diagnose")) { if (!ServerLifecycle.services().adminAccess.canUseModCommands(sender)) throw new CommandException("Admin diagnostics permission required"); showDiagnostics(player); return; }
            throw new WrongUsageException(getUsage(sender));
        } catch (IllegalArgumentException e) { throw new CommandException("Unknown Mastery branch or reset mode"); }
        catch (IllegalStateException e) { throw new CommandException(e.getMessage()); }
    }

    private void showBranches(EntityPlayerMP player) { message(player, "§6Искусства / Mastery branches: §f" + prestige.branches()); for (PrestigeBranch branch : prestige.branches()) message(player, "§7" + branch + " source=" + prestige.progressSource(branch)); }
    private void showStatus(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID());
        if (state.activeRun == null) { message(player, "§7No active Mastery run. Use /prestige branches then /prestige select <branch>"); return; }
        PrestigeState.RunState run = state.activeRun;
        PrestigeState.BranchState permanent = state.branch(run.activeBranch);
        message(player, "§6Mastery §f" + run.activeBranch + " §7runId=" + run.runId);
        message(player, "§fSource: " + prestige.progressSource(run.activeBranch));
        message(player, "§fRun level: " + run.branchLevel + " | Progress: " + number(run.branchProgress) + " | Fragments: " + number(run.runBranchCurrency));
        message(player, "§fMastery pending: " + number(run.masteryPendingFragments) + " | Pet pending: " + number(run.petPendingFragments));
        message(player, "§fRanks: " + run.reachedRanks);
        message(player, "§fTier: " + permanent.masteryTier + " | Meta cap: " + permanent.rankMetaLevelCap + " | Gain/run: +" + prestige.metaLevelGainPerRun(player.getUniqueID(), run.activeBranch));
        message(player, "§fRank meta-levels: " + permanent.rankMetaLevels + " | Mastery Reset: " + (prestige.masteryResetAvailable(player.getUniqueID()) ? "§aREADY" : "§7LOCKED"));
    }
    private void collect(EntityPlayerMP player) {
        PrestigeState.RunState run = prestige.activeRun(player.getUniqueID()); if (run == null) throw new IllegalStateException("No active Mastery run");
        BigDecimal collected = prestige.collectFragments(player.getUniqueID(), run.runId, PrestigeService.FragmentSource.MASTERY);
        message(player, "§aMastery Collect: +" + number(collected) + " Fragments");
    }
    private void showResetPreview(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID()); if (state.activeRun == null) throw new IllegalStateException("No active Mastery run");
        PrestigeResetService.ResetPreview preview = prestige.previewReset(player.getUniqueID(), state.activeRun.runId);
        message(player, "§6Mastery run reset preview §7(" + preview.branch + ")");
        message(player, "§fCurrent result: " + number(preview.currentResult) + " | Previous best: " + number(preview.previousBest));
        message(player, "§fSaved candidate: " + number(preview.savedCandidate) + " | New best: " + number(preview.newBest));
        message(player, "§fMeta gain/run: +" + preview.metaLevelGainPerRun + " | Ranks upgraded: " + preview.ranksUpgraded); message(player, "§eConfirm: /prestige reset confirm " + preview.runId + " §7| Cancel: do nothing");
    }
    private void confirmReset(EntityPlayerMP player, String runId) {
        PrestigeResetService.ResetResult result = prestige.reset(player.getUniqueID(), runId);
        if (result == PrestigeResetService.ResetResult.APPLIED) message(player, "§aMastery run reset complete. Select an Искусство to begin a new run.");
        else if (result == PrestigeResetService.ResetResult.DUPLICATE) message(player, "§eThat reset was already applied; no rewards were duplicated.");
        else message(player, "§cStale Mastery run reset request rejected.");
    }
    private void showMasteryResetPreview(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID()); if (state.activeRun == null) throw new IllegalStateException("No active Mastery run");
        PrestigeResetService.MasteryResetPreview preview = prestige.previewMasteryReset(player.getUniqueID(), state.activeRun.runId);
        message(player, "§6MASTER RESET READY §7(" + preview.branch + ")");
        message(player, "§fTier: " + preview.currentMasteryTier + " -> " + preview.nextMasteryTier);
        message(player, "§fMeta cap: " + preview.currentRankMetaLevelCap + " -> " + preview.nextRankMetaLevelCap);
        message(player, "§fGain/run: +" + preview.currentMetaLevelGainPerRun + " -> +" + preview.nextMetaLevelGainPerRun);
        message(player, "§fNormal: canonical run/pet/rune reset rules apply.");
        if (preview.premiumPreserveEnabled) {
            message(player, "§dPremium Preserve: " + preview.premiumPreserveCrystalCost + " Crystals | runes=" + preview.premiumPreserveRunes + " | pets=" + preview.premiumPreservePets);
            message(player, "§eNormal: /prestige mastery-reset confirm " + preview.runId + " normal");
            message(player, "§ePremium: /prestige mastery-reset confirm " + preview.runId + " premium");
        } else {
            message(player, "§7Premium Preserve is disabled by Balance Core for this balance snapshot.");
            message(player, "§eConfirm: /prestige mastery-reset confirm " + preview.runId + " normal");
        }
    }
    private void confirmMasteryReset(EntityPlayerMP player, String runId, MasteryResetMode mode) {
        PrestigeResetService.MasteryResetResult result = prestige.masteryReset(player.getUniqueID(), runId, mode);
        if (result == PrestigeResetService.MasteryResetResult.APPLIED) message(player, "§aMastery Reset complete (" + mode + "). Tier advanced; select an Искусство to begin the next run.");
        else if (result == PrestigeResetService.MasteryResetResult.DUPLICATE) message(player, "§eThat Mastery Reset was already applied; tier was not duplicated.");
        else if (result == PrestigeResetService.MasteryResetResult.NOT_ELIGIBLE) message(player, "§cMastery Reset is locked until every rank reaches the current meta cap.");
        else if (result == PrestigeResetService.MasteryResetResult.INSUFFICIENT_CRYSTALS) message(player, "§cNot enough Crystals for the selected Mastery Reset option.");
        else if (result == PrestigeResetService.MasteryResetResult.OPTION_UNAVAILABLE) message(player, "§cThat Mastery Reset option is disabled by Balance Core.");
        else if (result == PrestigeResetService.MasteryResetResult.POLICY_UNAVAILABLE) message(player, "§cMastery Reset balance policy is unavailable.");
        else message(player, "§cStale Mastery Reset request rejected.");
    }
    private void showDiagnostics(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID());
        if (state.activeRun == null) message(player, "§7activeBranch=null | runId=null");
        else message(player, "§7activeBranch=" + state.activeRun.activeBranch + " | runId=" + state.activeRun.runId + " | source=" + prestige.progressSource(state.activeRun.activeBranch)
                + " | progress=" + number(state.activeRun.branchProgress) + " | level=" + state.activeRun.branchLevel + " | reachedRanks=" + state.activeRun.reachedRanks
                + " | fragments=" + number(state.activeRun.runBranchCurrency) + " | masteryPending=" + number(state.activeRun.masteryPendingFragments) + " | petPending=" + number(state.activeRun.petPendingFragments));
        for (PrestigeBranch branch : PrestigeBranch.values()) { PrestigeState.BranchState permanent = state.branch(branch); message(player, "§7" + branch + " source=" + prestige.progressSource(branch) + " best=" + number(permanent.bestSavedProgress) + " meta=" + permanent.rankMetaLevels + " cap=" + permanent.rankMetaLevelCap + " masteryTier=" + permanent.masteryTier + " gain=" + prestige.metaLevelGainPerRun(player.getUniqueID(), branch)); }
    }
    private static String number(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
    private static void message(EntityPlayerMP player, String text) { player.sendMessage(new TextComponentString(text)); }
    @Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("branches", "select", "status", "collect", "reset", "mastery-reset", "diagnose"));
        if (args.length == 2 && args[0].equalsIgnoreCase("select")) { List<String> names = new ArrayList<>(); for (PrestigeBranch branch : prestige.branches()) names.add(branch.name()); return getListOfStringsMatchingLastWord(args, names); }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("mastery-reset"))) return getListOfStringsMatchingLastWord(args, Arrays.asList("confirm"));
        if (args.length == 4 && args[0].equalsIgnoreCase("mastery-reset") && args[1].equalsIgnoreCase("confirm")) return getListOfStringsMatchingLastWord(args, Arrays.asList("normal", "premium"));
        return new ArrayList<>();
    }
}
