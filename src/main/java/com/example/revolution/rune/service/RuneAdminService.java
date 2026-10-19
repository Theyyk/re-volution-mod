package com.example.revolution.rune.service;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneAdminService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final RuneEnhancementRepository enhancements;

    public RuneAdminService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            RuneEnhancementRepository enhancements) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.enhancements = enhancements;
    }

    public void execute(ICommandSender sender, String[] args) {

        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("§cOnly for players"));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length == 0) {
            player.sendMessage(new TextComponentString("§e/customrunes <clear|resetenhancements>"));
            return;
        }

        int statusIndex = players.getActiveCollection(player.getUniqueID());
        int statusId = RuneStatusConfig.statusForCollection(statusIndex);
        if (!RuneStatusConfig.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cАктивный статус не поддерживает руны"));
            return;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            players.clearCollection(player.getUniqueID(), statusIndex);
            player.sendMessage(new TextComponentString("§cВсе руны активного статуса очищены"));

            RuneSlotPacket msg = new RuneSlotPacket(-1, -1, -1, false);
            NetworkHandler.sendToClient(msg, player);
            stats.sendTo(player);
            return;
        }

        if (args[0].equalsIgnoreCase("resetenhancements")) {
            enhancements.resetCollection(player.getUniqueID(), statusIndex);
            NetworkHandler.sendToClient(
                    RunePacketFactory.enhancements(enhancements.getLevels(player.getUniqueID(), statusIndex)),
                    player
            );
            stats.sendTo(player);
            player.sendMessage(new TextComponentString("§aУсиления рун активного статуса сброшены до +0"));
            return;
        }

        player.sendMessage(new TextComponentString("§e/customrunes <clear|resetenhancements>"));
        }

}
