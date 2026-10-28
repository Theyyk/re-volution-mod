package com.example.revolution.rune.service;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PongPacket;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneCollectionProgression;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.rules.RuneStorageRules;
import com.example.revolution.rune.state.RuneInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RunePurchaseService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final RunePurchaseBalanceService purchaseBalance;

    public RunePurchaseService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            RunePurchaseBalanceService purchaseBalance) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
        this.purchaseBalance = purchaseBalance;
    }

    private static final int LEGACY_RUNE_COST = 100;
    private final Random random = new Random();

    public void buyRunes(EntityPlayerMP player, int collectionIndex, int requestedAmount, boolean buyAll) {
        List<String> collections = players.getCollectionNames(player.getUniqueID());
        if (collections.isEmpty() || collectionIndex < 0 || collectionIndex >= collections.size()) {
            player.sendMessage(new TextComponentString("§cУ тебя нет доступных колод."));
            return;
        }

        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        if (RuneStatusConfig.isRuneStatus(statusId)) {
            buyRunes(player, collectionIndex, statusId, requestedAmount, buyAll);
            return;
        }

        int balance = players.getBalance(player.getUniqueID());
        List<Integer> freeSlots = freeSlots(player.getUniqueID(), collectionIndex);
        if (freeSlots.isEmpty()) {
            player.sendMessage(new TextComponentString("§eВ этой legacy-коллекции больше нет свободных слотов."));
            NetworkHandler.sendToClient(new PongPacket(balance), player);
            return;
        }

        int affordable = balance / LEGACY_RUNE_COST;
        if (affordable <= 0) {
            player.sendMessage(new TextComponentString("§cНедостаточно монет! Нужно минимум " + LEGACY_RUNE_COST + ", у тебя " + balance));
            NetworkHandler.sendToClient(new PongPacket(balance), player);
            return;
        }

        int maxPossible = Math.min(affordable, freeSlots.size());
        int amountToBuy = buyAll ? maxPossible : Math.min(requestedAmount, maxPossible);
        if (amountToBuy <= 0) {
            NetworkHandler.sendToClient(new PongPacket(balance), player);
            return;
        }

        Collections.shuffle(freeSlots);
        for (int i = 0; i < amountToBuy; i++) {
            int slot = freeSlots.get(i);
            int runeIndex = random.nextInt(10);
            int layer = RuneInventory.NORMAL_RANK;
            players.addRuneToCollection(player.getUniqueID(), collectionIndex, slot, runeIndex, layer, 1);
            NetworkHandler.sendToClient(new RuneSlotPacket(slot, runeIndex, layer, true), player);
        }

        finishLegacyPurchase(player, balance, requestedAmount, buyAll, amountToBuy, amountToBuy, 0);
    }

    private void buyRunes(EntityPlayerMP player, int collectionIndex, int statusId, int requestedAmount, boolean buyAll) {
        String purchaseResource = purchaseBalance.purchaseResource(statusId);
        String duplicateResource = purchaseBalance.duplicateResource(statusId);
        String purchaseDisplayName = ResourceDefinitions.getDisplayName(purchaseResource);
        int startingBalance = resources.getPlayerResource(player.getUniqueID(), purchaseResource);

        List<RuneRecord> runes = players.getRunesInCollection(player.getUniqueID(), collectionIndex);
        int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (RuneRecord rune : runes) {
            if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
            bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], RuneProgression.clampRank(rune.layer));
        }

        RuneCollectionProgression progression = RuneCollectionProgression.fromBestRanks(bestRanks);
        int purchaseRank = progression.stageRank();
        boolean[] ownedAtStage = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            ownedAtStage[rune] = progression.isOwnedAtStage(rune);
        }
        int stageCount = progression.progressionIndex();

        if (stageCount >= RuneInventory.BOOST_TYPE_COUNT) {
            player.sendMessage(new TextComponentString(purchaseRank >= RuneProgression.ADVANCED_RANK
                    ? "§6Все 26 рун уже продвинутые. Достигнут максимальный ранг."
                    : "§eВсе 26 слотов текущего ранга заполнены. §6Пробудите подходящую руну."));
            syncPurchaseBalance(player, purchaseResource, startingBalance);
            return;
        }

        int attemptsLimit = buyAll ? Integer.MAX_VALUE : Math.max(0, requestedAmount);
        int attempts = 0;
        int newRunes = 0;
        int duplicates = 0;
        int spent = 0;
        int duplicateCompensation = 0;

        while (attempts < attemptsLimit && stageCount < RuneInventory.BOOST_TYPE_COUNT) {
            int progressionIndex = stageCount;
            int attemptCost = purchaseBalance.purchaseCost(statusId, purchaseRank, progressionIndex);
            int remaining = startingBalance - spent;
            if (remaining < attemptCost) break;

            attempts++;
            spent = safeAdd(spent, attemptCost);
            int runeIndex = random.nextInt(RuneInventory.BOOST_TYPE_COUNT);
            if (ownedAtStage[runeIndex]) {
                duplicates++;
                duplicateCompensation = safeAdd(
                        duplicateCompensation,
                        purchaseBalance.duplicateReward(statusId, purchaseRank, progressionIndex));
                NetworkHandler.sendToClient(
                        new RuneSlotPacket(runeIndex, runeIndex, bestRanks[runeIndex], true), player);
                continue;
            }

            ownedAtStage[runeIndex] = true;
            bestRanks[runeIndex] = purchaseRank;
            stageCount++;
            newRunes++;
            players.addRuneToCollection(
                    player.getUniqueID(),
                    collectionIndex,
                    runeIndex,
                    runeIndex,
                    purchaseRank,
                    1);
            NetworkHandler.sendToClient(
                    new RuneSlotPacket(runeIndex, runeIndex, purchaseRank, true),
                    player);
        }

        if (attempts <= 0) {
            int required = purchaseBalance.purchaseCost(statusId, purchaseRank, stageCount);
            player.sendMessage(new TextComponentString(
                    "§cНедостаточно ресурса «" + purchaseDisplayName + "»! Нужно минимум " + required
                            + ", у тебя " + startingBalance + "."));
            syncPurchaseBalance(player, purchaseResource, startingBalance);
            return;
        }

        if (duplicateCompensation > 0) {
            resources.addPlayerResource(player.getUniqueID(), duplicateResource, duplicateCompensation);
        }

        finishRunePurchase(
                player,
                purchaseResource,
                requestedAmount,
                buyAll,
                attempts,
                newRunes,
                duplicates,
                spent);

        if (duplicateCompensation > 0) {
            player.sendMessage(new TextComponentString("§aКомпенсация за дубликаты: §e+" + duplicateCompensation + " §f"
                    + ResourceDefinitions.getDisplayName(duplicateResource) + "§a."));
        }

        if (stageCount >= RuneInventory.BOOST_TYPE_COUNT) {
            player.sendMessage(new TextComponentString(purchaseRank >= RuneProgression.ADVANCED_RANK
                    ? "§6Все 26 рун уже продвинутые. Максимальный ранг достигнут."
                    : "§6Все 26 слотов текущего ранга заполнены. Пробудите подходящую руну."));
        }
    }

    private void finishLegacyPurchase(EntityPlayerMP player, int balance, int requestedAmount, boolean buyAll, int amountToBuy, int newRunes, int duplicates) {
        int spent = amountToBuy * LEGACY_RUNE_COST;
        int newBalance = balance - spent;
        players.setBalance(player.getUniqueID(), newBalance);
        stats.sendTo(player);
        String duplicateText = duplicates > 0 ? " §7Дубликаты: §e" + duplicates + "§7." : "";
        if (!buyAll && amountToBuy < requestedAmount) {
            player.sendMessage(new TextComponentString("§eКуплено " + amountToBuy + " из " + requestedAmount + " попыток. Новых рун: " + newRunes
                    + ". Потрачено: " + spent + ". Остаток: " + newBalance + "." + duplicateText));
        } else {
            player.sendMessage(new TextComponentString("§aПокупок: " + amountToBuy + ". Новых рун: " + newRunes
                    + ". Потрачено: " + spent + ". Остаток: " + newBalance + "." + duplicateText));
        }
    }

    private void finishRunePurchase(
            EntityPlayerMP player,
            String purchaseResource,
            int requestedAmount,
            boolean buyAll,
            int amountToBuy,
            int newRunes,
            int duplicates,
            int spent) {
        int newBalance = resources.addPlayerResource(player.getUniqueID(), purchaseResource, -spent);
        stats.sendTo(player);
        syncPurchaseBalance(player, purchaseResource, newBalance);
        String resourceName = ResourceDefinitions.getDisplayName(purchaseResource);
        String duplicateText = duplicates > 0 ? " §7Дубликаты: §e" + duplicates + "§7." : "";
        if (!buyAll && amountToBuy < requestedAmount) {
            player.sendMessage(new TextComponentString("§eКуплено " + amountToBuy + " из " + requestedAmount + " попыток. Новых рун: " + newRunes
                    + ". Потрачено: " + spent + " " + resourceName + ". Остаток: " + newBalance + "." + duplicateText));
        } else {
            player.sendMessage(new TextComponentString("§aПокупок: " + amountToBuy + ". Новых рун: " + newRunes
                    + ". Потрачено: " + spent + " " + resourceName + ". Остаток: " + newBalance + "." + duplicateText));
        }
    }

    private void syncPurchaseBalance(EntityPlayerMP player, String resource, int balance) {
        stats.sendTo(player);
        if ("coins".equals(ResourceDefinitions.normalize(resource))) NetworkHandler.sendToClient(new PongPacket(balance), player);
    }

    private int safeAdd(int left, int right) {
        long total = (long) left + right;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private List<Integer> freeSlots(UUID player, int collection) {
        List<Integer> occupied = new ArrayList<>();
        for (RuneRecord rune : players.getRunesInCollection(player, collection)) occupied.add(rune.slot);
        return RuneStorageRules.freePurchaseSlots(occupied);
    }
}
