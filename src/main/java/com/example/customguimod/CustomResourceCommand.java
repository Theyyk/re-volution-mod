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

public class CustomResourceCommand extends AdminOnlyCommand {

    @Override
    public String getName() {
        return "customresource";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/customresource create|delete|list|balance";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("create", "delete", "list", "balance"));
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                return getListOfStringsMatchingLastWord(args, Arrays.asList("<name>"));
            }
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("balance")) {
                return getListOfStringsMatchingLastWord(args, ResourceManager.getResourceNames());
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
            showUsage(player);
            return;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length != 2) {
                player.sendMessage(new TextComponentString("§c/customresource create <name>"));
                return;
            }

            String name = args[1];
            if (ResourceManager.isRetiredResource(name)) {
                player.sendMessage(new TextComponentString("§cРесурс lightnings выведен из системы и не может быть создан снова."));
                return;
            }
            if (!ResourceManager.isValidName(name)) {
                player.sendMessage(new TextComponentString("§cИмя ресурса: только буквы, цифры, _ и -"));
                return;
            }
            if (!ResourceManager.createResource(name)) {
                player.sendMessage(new TextComponentString("§cТакой ресурс уже существует"));
                return;
            }

            player.sendMessage(new TextComponentString("§aРесурс создан: §e" + ResourceManager.normalize(name)));
            return;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length != 2) {
                player.sendMessage(new TextComponentString("§c/customresource delete <resource>"));
                return;
            }

            if (!ResourceManager.deleteResource(args[1])) {
                player.sendMessage(new TextComponentString("§cРесурс не найден или является встроенным"));
                return;
            }

            player.sendMessage(new TextComponentString("§cРесурс удалён: " + args[1]));
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            List<String> resources = ResourceManager.getResourceNames();
            player.sendMessage(new TextComponentString("§6Ресурсы (" + resources.size() + "): §e" + String.join("§7, §e", resources)));
            return;
        }

        if (args[0].equalsIgnoreCase("balance")) {
            if (args.length != 2) {
                player.sendMessage(new TextComponentString("§c/customresource balance <resource>"));
                return;
            }

            String canonical = ResourceManager.findCanonicalName(args[1]);
            if (canonical == null) {
                player.sendMessage(new TextComponentString("§cРесурс не найден: " + args[1]));
                return;
            }

            int value = ResourceManager.getPlayerResource(player.getUniqueID(), canonical);
            player.sendMessage(new TextComponentString("§6" + ResourceManager.getDisplayName(canonical) + ": §e" + value));
            return;
        }

        showUsage(player);
    }

    private void showUsage(EntityPlayerMP player) {
        player.sendMessage(new TextComponentString("§e/customresource create <name>"));
        player.sendMessage(new TextComponentString("§e/customresource delete <resource>"));
        player.sendMessage(new TextComponentString("§e/customresource list"));
        player.sendMessage(new TextComponentString("§e/customresource balance <resource>"));
    }
}
