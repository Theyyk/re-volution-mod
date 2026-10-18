package com.example.customguimod;

import com.example.customguimod.commands.AdminOnlyCommand;
import com.example.customguimod.persistence.MongoManager;

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

public class CustomStatusCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customstatus";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customstatus create|switch|list|delete";
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
                List<String> statuses = MongoManager.getCollectionNames(player.getUniqueID());
                for (int i = 0; i < statuses.size(); i++) indexes.add(String.valueOf(i));
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
            player.sendMessage(new TextComponentString("§e/customstatus create <name>"));
            player.sendMessage(new TextComponentString("§e/customstatus switch <index>"));
            player.sendMessage(new TextComponentString("§e/customstatus list"));
            player.sendMessage(new TextComponentString("§e/customstatus delete <index>"));
            return;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customstatus create <name>"));
                return;
            }

            List<String> existing = MongoManager.getCollectionNames(player.getUniqueID());
            if (existing.contains(args[1])) {
                player.sendMessage(new TextComponentString("§cСтатус с таким именем уже существует"));
                return;
            }
            if (existing.size() >= 9) {
                player.sendMessage(new TextComponentString("§cМаксимум 9 статусов"));
                return;
            }

            MongoManager.addCollection(player.getUniqueID(), args[1]);
            player.sendMessage(new TextComponentString("§aСтатус " + args[1] + " создан"));
        } else if (args[0].equalsIgnoreCase("switch")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customstatus switch <index>"));
                return;
            }

            try {
                int index = Integer.parseInt(args[1]);
                List<String> statuses = MongoManager.getCollectionNames(player.getUniqueID());

                if (index < 0 || index >= statuses.size()) {
                    player.sendMessage(new TextComponentString("§cСтатуса с номером " + index + " не существует"));
                    return;
                }

                MongoManager.setActiveCollection(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString(
                        "§aАктивный статус: §e" + statuses.get(index) + " §7[" + index + "]"));
                PlayerStatsService.sendTo(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> statuses = MongoManager.getCollectionNames(player.getUniqueID());
            int active = MongoManager.getActiveCollection(player.getUniqueID());
            player.sendMessage(new TextComponentString("§6Статусы (" + statuses.size() + "):"));
            for (int i = 0; i < statuses.size(); i++) {
                String marker = (i == active) ? " §a← активный" : "";
                player.sendMessage(new TextComponentString("§7[" + i + "] " + statuses.get(i) + marker));
            }
        } else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customstatus delete <index>"));
                return;
            }

            try {
                int index = Integer.parseInt(args[1]);
                List<String> statuses = MongoManager.getCollectionNames(player.getUniqueID());

                if (index < 0 || index >= statuses.size()) {
                    player.sendMessage(new TextComponentString("§cСтатуса с номером " + index + " не существует"));
                    return;
                }

                String statusName = statuses.get(index);
                MongoManager.deleteCollection(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString("§cСтатус " + statusName + " удалён"));
                PlayerStatsService.sendTo(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        }
    }
}
