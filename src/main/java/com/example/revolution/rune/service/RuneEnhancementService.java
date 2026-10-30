package com.example.revolution.rune.service;

import com.example.revolution.number.LargeNumbers;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneEnhancementService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final RuneEnhancementRepository enhancements;
    private final RuneSyncService sync;
    private final RuneEnhancementPlanningService planning;
    private final Random random = new Random();

    public RuneEnhancementService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            RuneEnhancementRepository enhancements,
            RuneSyncService sync,
            RuneEnhancementPlanningService planning) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.enhancements = enhancements;
        this.sync = sync;
        this.planning = planning;
    }

    public void enhanceRune(EntityPlayerMP player, int collectionIndex, int ignoredRuneIndex, int requestedAmount) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collectionId == null) {
            player.sendMessage(new TextComponentString("§cУсиление недоступно для этой legacy-коллекции."));
            return;
        }
        if (requestedAmount <= 0) return;

        int[] currentLevels = enhancements.getLevels(player.getUniqueID(), collectionIndex);
        RuneEnhancementPlan plan;
        try {
            plan = planning.plan(collectionId, currentLevels, ownedSlots(player, collectionIndex), requestedAmount, random);
        } catch (IllegalArgumentException | IllegalStateException e) {
            if ("no eligible enhancement targets remain".equals(e.getMessage())) {
                player.sendMessage(new TextComponentString("§eНет доступных слотов для усиления."));
            } else {
                player.sendMessage(new TextComponentString("§cУсиление для этой коллекции пока не настроено."));
            }
            return;
        }

        String resource = plan.getResource();
        BigDecimal cost = plan.getTotalCost();
        BigDecimal playerBalance = resources.getPlayerResourceValue(player.getUniqueID(), resource);
        if (playerBalance.compareTo(cost) < 0) {
            player.sendMessage(new TextComponentString("§cНедостаточно ресурса «" + ResourceDefinitions.getDisplayName(resource)
                    + "» для " + requestedAmount + " усилений. Нужно " + LargeNumbers.format(cost)
                    + ", у тебя " + LargeNumbers.format(playerBalance) + "."));
            stats.sendTo(player);
            return;
        }

        if (!resources.spendPlayerResource(player.getUniqueID(), resource, cost)) {
            player.sendMessage(new TextComponentString("§cБаланс изменился. Повторите усиление."));
            stats.sendTo(player);
            return;
        }

        int[] increments = plan.getIncrements();
        for (int slot = 0; slot < increments.length; slot++) {
            if (increments[slot] <= 0) continue;
            enhancements.addLevels(player.getUniqueID(), collectionIndex, slot, increments[slot]);
        }

        sync.sendEnhancements(player, collectionIndex);
        stats.sendTo(player);
        player.sendMessage(new TextComponentString("§aСлучайных усилений: §f" + requestedAmount
                + "§a. Затронуто слотов: §f" + plan.getAffectedSlots()
                + "§a. Потрачено: §e" + LargeNumbers.format(cost) + " §f"
                + ResourceDefinitions.getDisplayName(resource) + "§a."));
    }

    private boolean[] ownedSlots(EntityPlayerMP player, int collectionIndex) {
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        for (RuneRecord rune : players.getRunesInCollection(player.getUniqueID(), collectionIndex)) {
            if (rune.slot >= 0 && rune.slot < owned.length) owned[rune.slot] = true;
        }
        return owned;
    }
}
