package com.example.revolution.rune.service;

import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneEconomyConfig;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.state.RuneInventory;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneEnhancementService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final RuneEnhancementRepository enhancements;
    private final RuneSyncService sync;

    public RuneEnhancementService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            RuneEnhancementRepository enhancements,
            RuneSyncService sync) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.enhancements = enhancements;
        this.sync = sync;
    }

    private final Random random = new Random();

    public void enhanceRune(EntityPlayerMP player, int collectionIndex, int ignoredRuneIndex, int requestedAmount) {
        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        if (!RuneStatusConfig.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cУсиление недоступно для этой legacy-коллекции."));
            return;
        }

        String configuredResource = RuneEconomyConfig.enhancementResourceForStatus(statusId);
        String resource = ResourceDefinitions.findCanonicalName(configuredResource);
        if (resource == null) {
            player.sendMessage(new TextComponentString("§cУсиление для этого статуса пока не настроено."));
            return;
        }

        int[] currentLevels = enhancements.getLevels(player.getUniqueID(), collectionIndex);
        int[] plannedLevels = Arrays.copyOf(currentLevels, currentLevels.length);
        int[] increments = new int[RuneInventory.BOOST_TYPE_COUNT];
        long totalCost = 0L;

        // Each requested enhancement is an independent random roll over all 26 fixed slots.
        // Empty slots are valid targets because enhancement is stored on the slot itself and
        // becomes an active bonus only after the corresponding rune is obtained.
        for (int attempt = 0; attempt < requestedAmount; attempt++) {
            int rune = random.nextInt(RuneInventory.BOOST_TYPE_COUNT);
            int level = plannedLevels[rune];
            if (level >= Integer.MAX_VALUE) {
                player.sendMessage(new TextComponentString("§cЭтот слот достиг технического предела усиления."));
                return;
            }

            int stepCost = RuneEconomyConfig.enhancementNextCost(statusId, level);
            if (stepCost <= 0) {
                player.sendMessage(new TextComponentString("§cУсиление для этого статуса пока не настроено."));
                return;
            }

            totalCost += stepCost;
            if (totalCost > Integer.MAX_VALUE) {
                player.sendMessage(new TextComponentString("§cСтоимость выбранного количества усилений слишком велика."));
                return;
            }

            plannedLevels[rune] = level + 1;
            increments[rune]++;
        }

        int cost = (int) totalCost;
        int balance = resources.getPlayerResource(player.getUniqueID(), resource);
        if (balance < cost) {
            player.sendMessage(new TextComponentString("§cНедостаточно ресурса «" + ResourceDefinitions.getDisplayName(resource)
                    + "» для " + requestedAmount + " усилений. Нужно " + cost + ", у тебя " + balance + "."));
            stats.sendTo(player);
            return;
        }

        if (!resources.spendPlayerResource(player.getUniqueID(), resource, cost)) {
            player.sendMessage(new TextComponentString("§cБаланс изменился. Повторите усиление."));
            stats.sendTo(player);
            return;
        }

        int affectedSlots = 0;
        for (int rune = 0; rune < increments.length; rune++) {
            if (increments[rune] <= 0) continue;
            enhancements.addLevels(player.getUniqueID(), collectionIndex, rune, increments[rune]);
            affectedSlots++;
        }

        sync.sendEnhancements(player, collectionIndex);
        stats.sendTo(player);
        player.sendMessage(new TextComponentString("§aСлучайных усилений: §f" + requestedAmount
                + "§a. Затронуто слотов: §f" + affectedSlots
                + "§a. Потрачено: §e" + cost + " §f" + ResourceDefinitions.getDisplayName(resource) + "§a."));
    }
}
