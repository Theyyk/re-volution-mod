package com.example.revolution.commands;



import com.example.revolution.player.PlayerStatsService;
import com.example.revolution.rune.RuneEnhancementManager;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.rune.RuneStatusRegistry;
import com.example.revolution.commands.AdminOnlyCommand;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.persistence.MongoManager;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomRunesCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customrunes";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customrunes <clear|resetenhancements>";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("clear", "resetenhancements"));
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("§cOnly for players"));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length == 0) {
            player.sendMessage(new TextComponentString("§e/customrunes <clear|resetenhancements>"));
            return;
        }

        int statusIndex = MongoManager.getActiveCollection(player.getUniqueID());
        int statusId = RuneStatusRegistry.statusForCollection(statusIndex);
        if (!RuneStatusRegistry.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cАктивный статус не поддерживает руны"));
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            MongoManager.clearCollection(player.getUniqueID(), statusIndex);
            player.sendMessage(new TextComponentString("§cВсе руны активного статуса очищены"));

            RuneSlotPacket msg = new RuneSlotPacket(-1, -1, -1, false);
            NetworkHandler.sendToClient(msg, player);
            PlayerStatsService.sendTo(player);
            return;
        }

        if (args[0].equalsIgnoreCase("resetenhancements")) {
            RuneEnhancementManager.resetCollection(player.getUniqueID(), statusIndex);
            NetworkHandler.sendToClient(
                    new RuneEnhancementPacket(RuneEnhancementManager.getLevels(player.getUniqueID(), statusIndex)),
                    player
            );
            PlayerStatsService.sendTo(player);
            player.sendMessage(new TextComponentString("§aУсиления рун активного статуса сброшены до +0"));
            return;
        }

        player.sendMessage(new TextComponentString("§e/customrunes <clear|resetenhancements>"));
    }
}
