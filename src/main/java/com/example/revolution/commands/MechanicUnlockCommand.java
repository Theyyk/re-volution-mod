package com.example.revolution.commands;

import com.example.revolution.mechanic.MechanicUnlockModel;
import com.example.revolution.mechanic.MechanicUnlockService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/** Admin/debug controls for v2.12.0 permanent mechanic unlock state. */
public final class MechanicUnlockCommand extends AdminOnlyCommand {
    private final MechanicUnlockService unlocks;
    public MechanicUnlockCommand(MechanicUnlockService unlocks) { this.unlocks = unlocks; }
    @Override public String getName() { return "mechanicunlock"; }
    @Override public String getUsage(ICommandSender sender) { return "/mechanicunlock status <player>|unlock <player> <id>|lock <player> <id>|clear <player> <id>|setprogress <player> <rarity> <count>"; }

    @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) throw new CommandException(getUsage(sender));
        EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(args[1]);
        if (player == null) throw new CommandException("Player must be online: " + args[1]);
        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        try {
            if ("status".equals(action)) {
                MechanicUnlockModel.PlayerState state = unlocks.refreshRuneProgress(player.getUniqueID());
                sender.sendMessage(new TextComponentString("§6Mechanic unlocks for §e" + player.getName()));
                sender.sendMessage(new TextComponentString("§fMax: " + state.maxRuneProgressByRarity));
                sender.sendMessage(new TextComponentString("§fUnlocked: " + state.unlockedMechanicIds));
                sender.sendMessage(new TextComponentString("§fForce unlocked: " + state.forceUnlockedMechanicIds));
                sender.sendMessage(new TextComponentString("§fForce locked: " + state.forceLockedMechanicIds));
                return;
            }
            if (("unlock".equals(action) || "lock".equals(action) || "clear".equals(action)) && args.length == 3) {
                if ("unlock".equals(action)) unlocks.forceUnlock(player.getUniqueID(), args[2]);
                else if ("lock".equals(action)) unlocks.forceLock(player.getUniqueID(), args[2]);
                else unlocks.clearOverride(player.getUniqueID(), args[2]);
                sender.sendMessage(new TextComponentString("§a" + action + " applied: " + args[2]));
                return;
            }
            if ("setprogress".equals(action) && args.length == 4) {
                MechanicUnlockModel.Rarity rarity = MechanicUnlockModel.Rarity.valueOf(args[2].toUpperCase(java.util.Locale.ROOT));
                int value = Integer.parseInt(args[3]);
                unlocks.setMaxProgress(player.getUniqueID(), rarity, value);
                sender.sendMessage(new TextComponentString("§a" + rarity + " max set to " + Math.max(0, value)));
                return;
            }
        } catch (IllegalArgumentException e) { throw new CommandException(e.getMessage()); }
        throw new CommandException(getUsage(sender));
    }

    @Override public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("status", "unlock", "lock", "clear", "setprogress"));
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        if (args.length == 3 && "setprogress".equalsIgnoreCase(args[0])) {
            List<String> values = new ArrayList<>(); for (MechanicUnlockModel.Rarity rarity : MechanicUnlockModel.Rarity.values()) values.add(rarity.name());
            return getListOfStringsMatchingLastWord(args, values);
        }
        if (args.length == 3 && ("unlock".equalsIgnoreCase(args[0]) || "lock".equalsIgnoreCase(args[0]) || "clear".equalsIgnoreCase(args[0]))) {
            List<String> ids = new ArrayList<>(); for (MechanicUnlockModel.Definition d : unlocks.policy().all()) ids.add(d.unlockId);
            return getListOfStringsMatchingLastWord(args, ids);
        }
        return new ArrayList<>();
    }
}
