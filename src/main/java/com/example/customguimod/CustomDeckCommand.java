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

public class CustomDeckCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customdeck";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customdeck create|switch|list|delete";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("create", "switch", "list", "delete"));
        }

        if (args.length == 2 && sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (args[0].equalsIgnoreCase("create")) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            }
            if (args[0].equalsIgnoreCase("switch") || args[0].equalsIgnoreCase("delete")) {
                List<String> indexes = new ArrayList<>();
                List<String> decks = MongoManager.getDeckNames(player.getUniqueID());
                for (int i = 0; i < decks.size(); i++) indexes.add(String.valueOf(i));
                return getListOfStringsMatchingLastWord(args, indexes);
            }
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
            player.sendMessage(new TextComponentString("§e/customdeck create <name>"));
            player.sendMessage(new TextComponentString("§e/customdeck switch <index>"));
            player.sendMessage(new TextComponentString("§e/customdeck list"));
            player.sendMessage(new TextComponentString("§e/customdeck delete <index>"));
            return;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customdeck create <name>"));
                return;
            }

            List<String> existing = MongoManager.getDeckNames(player.getUniqueID());
            if (existing.contains(args[1])) {
                player.sendMessage(new TextComponentString("§cКолода с таким именем уже существует"));
                return;
            }
            if (existing.size() >= 9) {
                player.sendMessage(new TextComponentString("§cМаксимум 9 колод"));
                return;
            }

            MongoManager.addDeck(player.getUniqueID(), args[1]);
            player.sendMessage(new TextComponentString("§aКолода " + args[1] + " создана"));
        } else if (args[0].equalsIgnoreCase("switch")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customdeck switch <index>"));
                return;
            }

            try {
                int index = Integer.parseInt(args[1]);
                List<String> decks = MongoManager.getDeckNames(player.getUniqueID());

                if (index < 0 || index >= decks.size()) {
                    player.sendMessage(new TextComponentString("§cКолоды с номером " + index + " не существует"));
                    return;
                }

                MongoManager.setActiveDeck(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString(
                        "§aАктивная колода: §e" + decks.get(index) + " §7[" + index + "]"));
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> decks = MongoManager.getDeckNames(player.getUniqueID());
            int active = MongoManager.getActiveDeck(player.getUniqueID());
            player.sendMessage(new TextComponentString("§6Колоды (" + decks.size() + "):"));
            for (int i = 0; i < decks.size(); i++) {
                String marker = (i == active) ? " §a← активная" : "";
                player.sendMessage(new TextComponentString("§7[" + i + "] " + decks.get(i) + marker));
            }
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customdeck delete <index>"));
                return;
            }

            try {
                int index = Integer.parseInt(args[1]);
                List<String> decks = MongoManager.getDeckNames(player.getUniqueID());

                if (index < 0 || index >= decks.size()) {
                    player.sendMessage(new TextComponentString("§cКолоды с номером " + index + " не существует"));
                    return;
                }

                String deckName = decks.get(index);
                MongoManager.deleteDeck(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString("§cКолода " + deckName + " удалена"));
                PlayerStatsService.sendTo(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        }
    }
}
