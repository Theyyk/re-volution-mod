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
import java.util.UUID;

public final class AdminPanelCommand extends AdminOnlyCommand {
    @Override
    public String getName() {
        return "adminpanel";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/adminpanel whitelist add|remove|list|reload";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, Arrays.asList("whitelist"));
        if (args.length == 2 && "whitelist".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, Arrays.asList("add", "remove", "list", "reload"));
        }
        if (args.length == 3 && "whitelist".equalsIgnoreCase(args[0])
                && ("add".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1]))) {
            List<String> names = new ArrayList<>();
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) names.add(player.getName());
            return getListOfStringsMatchingLastWord(args, names);
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || !"whitelist".equalsIgnoreCase(args[0])) {
            sender.sendMessage(new TextComponentString("§e/adminpanel whitelist add|remove|list|reload"));
            return;
        }

        String action = args[1].toLowerCase();
        if ("reload".equals(action)) {
            AdminPanelAccess.reload();
            sender.sendMessage(new TextComponentString("§aAdmin Panel whitelist reloaded."));
            return;
        }
        if ("list".equals(action)) {
            List<AdminPanelAccess.Entry> entries = AdminPanelAccess.entries();
            sender.sendMessage(new TextComponentString("§6Admin Panel whitelist (" + entries.size() + "):"));
            for (AdminPanelAccess.Entry entry : entries) {
                sender.sendMessage(new TextComponentString("§7- §e" + entry.name + " §8" + entry.uuid));
            }
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(new TextComponentString("§cУкажите игрока: /adminpanel whitelist " + action + " <player|uuid>"));
            return;
        }

        EntityPlayerMP online = server.getPlayerList().getPlayerByUsername(args[2]);
        UUID uuid;
        String name;
        if (online != null) {
            uuid = online.getUniqueID();
            name = online.getName();
        } else {
            try {
                uuid = UUID.fromString(args[2]);
                name = args[2];
            } catch (IllegalArgumentException e) {
                sender.sendMessage(new TextComponentString("§cИгрок не найден онлайн и значение не является UUID."));
                return;
            }
        }

        if ("add".equals(action)) {
            boolean changed = AdminPanelAccess.add(uuid, name);
            sender.sendMessage(new TextComponentString(changed
                    ? "§aДобавлен в Admin Panel whitelist: §e" + name
                    : "§eИгрок уже есть в Admin Panel whitelist."));
        } else if ("remove".equals(action)) {
            boolean changed = AdminPanelAccess.remove(uuid);
            sender.sendMessage(new TextComponentString(changed
                    ? "§aУдалён из Admin Panel whitelist: §e" + name
                    : "§eИгрока нет в Admin Panel whitelist."));
        } else {
            sender.sendMessage(new TextComponentString("§cНеизвестное действие: " + action));
        }
    }
}
