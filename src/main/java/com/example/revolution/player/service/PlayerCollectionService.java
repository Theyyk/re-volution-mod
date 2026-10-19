package com.example.revolution.player.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.resource.service.ResourceService;
import java.util.List;
import java.util.UUID;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class PlayerCollectionService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;

    public PlayerCollectionService(PlayerRepository players, ResourceService resources, PlayerStatsSyncService stats) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
    }

    public void execute(ICommandSender sender, String[] args) {

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

            List<String> existing = players.getCollectionNames(player.getUniqueID());
            if (existing.contains(args[1])) {
                player.sendMessage(new TextComponentString("§cСтатус с таким именем уже существует"));
                return;
            }
            if (existing.size() >= 9) {
                player.sendMessage(new TextComponentString("§cМаксимум 9 статусов"));
                return;
            }

            players.addCollection(player.getUniqueID(), args[1]);
            player.sendMessage(new TextComponentString("§aСтатус " + args[1] + " создан"));
        } else if (args[0].equalsIgnoreCase("switch")) {
            if (args.length < 2) {
                player.sendMessage(new TextComponentString("§c/customstatus switch <index>"));
                return;
            }

            try {
                int index = Integer.parseInt(args[1]);
                List<String> statuses = players.getCollectionNames(player.getUniqueID());

                if (index < 0 || index >= statuses.size()) {
                    player.sendMessage(new TextComponentString("§cСтатуса с номером " + index + " не существует"));
                    return;
                }

                players.setActiveCollection(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString(
                        "§aАктивный статус: §e" + statuses.get(index) + " §7[" + index + "]"));
                stats.sendTo(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            List<String> statuses = players.getCollectionNames(player.getUniqueID());
            int active = players.getActiveCollection(player.getUniqueID());
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
                List<String> statuses = players.getCollectionNames(player.getUniqueID());

                if (index < 0 || index >= statuses.size()) {
                    player.sendMessage(new TextComponentString("§cСтатуса с номером " + index + " не существует"));
                    return;
                }

                String statusName = statuses.get(index);
                players.deleteCollection(player.getUniqueID(), index);
                player.sendMessage(new TextComponentString("§cСтатус " + statusName + " удалён"));
                stats.sendTo(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНомер должен быть числом"));
            }
        }
        }

    public List<String> names(java.util.UUID player) { return players.getCollectionNames(player); }
}
