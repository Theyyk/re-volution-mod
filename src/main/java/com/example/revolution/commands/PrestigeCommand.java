package com.example.revolution.commands;

import com.example.revolution.lifecycle.ServerLifecycle;
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

/** Mechanics-first Prestige skeleton UI. */
public final class PrestigeCommand extends CommandBase {
    private final PrestigeService prestige;

    public PrestigeCommand(PrestigeService prestige) { this.prestige = prestige; }

    @Override public String getName() { return "prestige"; }
    @Override public String getUsage(ICommandSender sender) {
        return "/prestige branches|select <branch>|status|reset [confirm <runId>]|diagnose";
    }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        String action = args.length == 0 ? "status" : args[0].toLowerCase();
        try {
            if (action.equals("branches")) { showBranches(player); return; }
            if (action.equals("select")) {
                if (args.length != 2) throw new WrongUsageException("/prestige select <branch>");
                PrestigeState.RunState run = prestige.startRun(player.getUniqueID(), PrestigeBranch.valueOf(args[1].toUpperCase()));
                message(player, "§aPrestige run started: " + run.activeBranch + " | runId=" + run.runId);
                return;
            }
            if (action.equals("status")) { showStatus(player); return; }
            if (action.equals("reset")) {
                if (args.length == 1) { showResetPreview(player); return; }
                if (args.length == 3 && args[1].equalsIgnoreCase("confirm")) { confirmReset(player, args[2]); return; }
                throw new WrongUsageException("/prestige reset OR /prestige reset confirm <runId>");
            }
            if (action.equals("diagnose")) {
                if (!ServerLifecycle.services().adminAccess.canUseModCommands(sender)) throw new CommandException("Admin diagnostics permission required");
                showDiagnostics(player);
                return;
            }
            throw new WrongUsageException(getUsage(sender));
        } catch (IllegalArgumentException e) {
            throw new CommandException("Unknown Prestige branch");
        } catch (IllegalStateException e) {
            throw new CommandException(e.getMessage());
        }
    }

    private void showBranches(EntityPlayerMP player) {
        message(player, "§6Prestige branches: §f" + prestige.branches());
        for (PrestigeBranch branch : prestige.branches()) {
            message(player, "§7" + branch + " source=" + prestige.progressSource(branch));
        }
    }

    private void showStatus(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID());
        if (state.activeRun == null) { message(player, "§7No active Prestige run. Use /prestige branches then /prestige select <branch>"); return; }
        PrestigeState.RunState run = state.activeRun;
        message(player, "§6Prestige §f" + run.activeBranch + " §7runId=" + run.runId);
        message(player, "§fSource: " + prestige.progressSource(run.activeBranch));
        message(player, "§fRun level: " + run.branchLevel + " | Progress: " + number(run.branchProgress)
                + " | Currency: " + number(run.runBranchCurrency));
        message(player, "§fRanks: " + run.reachedRanks);
    }

    private void showResetPreview(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID());
        if (state.activeRun == null) throw new IllegalStateException("No active Prestige run");
        PrestigeResetService.ResetPreview preview = prestige.previewReset(player.getUniqueID(), state.activeRun.runId);
        message(player, "§6Prestige reset preview §7(" + preview.branch + ")");
        message(player, "§fCurrent result: " + number(preview.currentResult) + " | Previous best: " + number(preview.previousBest));
        message(player, "§fSaved candidate: " + number(preview.savedCandidate) + " | New best: " + number(preview.newBest));
        message(player, "§fRanks upgraded: " + preview.ranksUpgraded);
        message(player, "§eConfirm: /prestige reset confirm " + preview.runId + " §7| Cancel: do nothing");
    }

    private void confirmReset(EntityPlayerMP player, String runId) {
        PrestigeResetService.ResetResult result = prestige.reset(player.getUniqueID(), runId);
        if (result == PrestigeResetService.ResetResult.APPLIED) message(player, "§aPrestige reset complete. Select a branch to begin a new run.");
        else if (result == PrestigeResetService.ResetResult.DUPLICATE) message(player, "§eThat reset was already applied; no rewards were duplicated.");
        else message(player, "§cStale Prestige reset request rejected.");
    }

    private void showDiagnostics(EntityPlayerMP player) {
        PrestigeState state = prestige.snapshot(player.getUniqueID());
        if (state.activeRun == null) message(player, "§7activeBranch=null | runId=null");
        else message(player, "§7activeBranch=" + state.activeRun.activeBranch + " | runId=" + state.activeRun.runId
                + " | source=" + prestige.progressSource(state.activeRun.activeBranch)
                + " | progress=" + number(state.activeRun.branchProgress) + " | level=" + state.activeRun.branchLevel
                + " | reachedRanks=" + state.activeRun.reachedRanks + " | runCurrency=" + number(state.activeRun.runBranchCurrency));
        for (PrestigeBranch branch : PrestigeBranch.values()) {
            PrestigeState.BranchState permanent = state.branch(branch);
            message(player, "§7" + branch + " source=" + prestige.progressSource(branch)
                    + " best=" + number(permanent.bestSavedProgress) + " meta=" + permanent.rankMetaLevels
                    + " cap=" + permanent.rankMetaLevelCap + " masteryTier=" + permanent.masteryTier
                    + " masteryQuest=" + permanent.masteryQuestState);
        }
    }

    private static String number(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
    private static void message(EntityPlayerMP player, String text) { player.sendMessage(new TextComponentString(text)); }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("branches", "select", "status", "reset", "diagnose"));
        if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            List<String> names = new ArrayList<>();
            for (PrestigeBranch branch : prestige.branches()) names.add(branch.name());
            return getListOfStringsMatchingLastWord(args, names);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return getListOfStringsMatchingLastWord(args, Arrays.asList("confirm"));
        return new ArrayList<>();
    }
}
