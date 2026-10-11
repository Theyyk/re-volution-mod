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

public class CustomCardsCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customcards";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customcards clear";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("clear"));
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
            player.sendMessage(new TextComponentString("§e/customcards clear"));
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            int deckIndex = MongoManager.getActiveDeck(player.getUniqueID());
            MongoManager.clearDeck(player.getUniqueID(), deckIndex);
            player.sendMessage(new TextComponentString("§cВсе карточки в активной колоде очищены"));

            CardPacket msg = new CardPacket(-1, -1, -1, false);
            NetworkHandler.INSTANCE.sendTo(msg, player);
            PlayerStatsService.sendTo(player);
        }
    }
}
