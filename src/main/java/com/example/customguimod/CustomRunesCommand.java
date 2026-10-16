package com.example.customguimod;

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

        int statusIndex = MongoManager.getActiveDeck(player.getUniqueID());
        int statusId = RuneStatusRegistry.statusForDeck(statusIndex);
        if (!RuneStatusRegistry.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cАктивный статус не поддерживает руны"));
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            MongoManager.clearDeck(player.getUniqueID(), statusIndex);
            player.sendMessage(new TextComponentString("§cВсе руны активного статуса очищены"));

            CardPacket msg = new CardPacket(-1, -1, -1, false);
            NetworkHandler.INSTANCE.sendTo(msg, player);
            PlayerStatsService.sendTo(player);
            return;
        }

        if (args[0].equalsIgnoreCase("resetenhancements")) {
            RuneEnhancementManager.resetDeck(player.getUniqueID(), statusIndex);
            NetworkHandler.INSTANCE.sendTo(
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
