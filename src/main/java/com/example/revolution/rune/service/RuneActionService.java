package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.mechanic.MechanicUnlockService;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PongPacket;
import com.example.revolution.network.ShareChatDelivery;
import com.example.revolution.rune.definition.PrimaryRuneCatalog;
import com.example.revolution.rune.definition.RuneTechnicalCatalog;
import com.example.revolution.rune.definition.RuneStat;
import com.example.revolution.rune.definition.RuneTargetedPower;
import com.example.revolution.rune.definition.RuneCompositeEffect;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneActionService {
    private final PlayerRepository players;
    private final PlayerStatsSyncService stats;
    private final RunePurchaseService purchase;
    private final RuneEnhancementService enhancement;
    private final RuneAwakeningService awakening;
    private final RuneRankRevertService revert;
    private final RuneSyncService sync;
    private final RuneCollectionUnlockService collectionUnlocks;
    private final MechanicUnlockService mechanicUnlocks;
    private final RuneProgressRefreshCoalescer mechanicProgressRefreshes = new RuneProgressRefreshCoalescer();

    public RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneSyncService sync) {
        this(players, stats, purchase, enhancement, awakening,
                new RuneRankRevertService(players, stats, sync, purchase.revertBalanceRules()), sync, null);
    }

    public RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneSyncService sync,
            MechanicUnlockService mechanicUnlocks) {
        this(players, stats, purchase, enhancement, awakening,
                new RuneRankRevertService(players, stats, sync, purchase.revertBalanceRules()), sync, mechanicUnlocks);
    }

    RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneRankRevertService revert,
            RuneSyncService sync) {
        this(players, stats, purchase, enhancement, awakening, revert, sync, null);
    }

    RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneRankRevertService revert,
            RuneSyncService sync,
            MechanicUnlockService mechanicUnlocks) {
        this.players = players;
        this.stats = stats;
        this.purchase = purchase;
        this.enhancement = enhancement;
        this.awakening = awakening;
        this.revert = revert;
        this.sync = sync;
        this.mechanicUnlocks = mechanicUnlocks;
        this.collectionUnlocks = players instanceof RuneCollectionStateRepository
                ? new RuneCollectionUnlockService(players, (RuneCollectionStateRepository) players)
                : null;
    }

    public void handle(EntityPlayerMP player, String action) {
        if (action == null) return;

        String loggedAction = logActionName(action);
        if (isImportantPlayerAction(action)) {
            RevolutionMod.logger.info("Player action from " + player.getName() + ": " + loggedAction);
        } else {
            RevolutionMod.logger.debug("Packet from " + player.getName() + ": " + loggedAction);
        }

        int collectionIndex = players.getActiveCollection(player.getUniqueID());
        if (RuneCollectionId.fromLegacyIndex(collectionIndex) == null) {
            collectionIndex = 0;
            players.setActiveCollection(player.getUniqueID(), collectionIndex);
        }
        if (!isCollectionUnlocked(player, collectionIndex)) {
            collectionIndex = 0;
            players.setActiveCollection(player.getUniqueID(), 0);
        }

        if (action.equals("buy_card")) {
            purchase.buyRunes(player, collectionIndex, 1, false);
            evaluateCollectionUnlock(player);
            refreshMechanicProgress(player);
        } else if (action.startsWith("buy_cards:")) {
            String rawAmount = action.substring("buy_cards:".length());

            if (rawAmount.equalsIgnoreCase("all")) {
                purchase.buyRunes(player, collectionIndex, Integer.MAX_VALUE, true);
                evaluateCollectionUnlock(player);
                refreshMechanicProgress(player);
                return;
            }

            try {
                int requestedAmount = Integer.parseInt(rawAmount);
                if (!isSupportedBulkAmount(requestedAmount)) {
                    player.sendMessage(new TextComponentString("§cНекорректное количество рун."));
                    return;
                }
                purchase.buyRunes(player, collectionIndex, requestedAmount, false);
                evaluateCollectionUnlock(player);
                refreshMechanicProgress(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректное количество рун."));
            }
        } else if (action.startsWith("enhance_rune:")) {
            String[] parts = action.substring("enhance_rune:".length()).split(":");
            if (parts.length != 2) {
                player.sendMessage(new TextComponentString("§cНекорректная команда усиления."));
                return;
            }
            try {
                int runeIndex = Integer.parseInt(parts[0]);
                int requestedAmount = Integer.parseInt(parts[1]);
                if (!isSupportedBulkAmount(requestedAmount)) {
                    player.sendMessage(new TextComponentString("§cНекорректное количество усилений."));
                    return;
                }
                enhancement.enhanceRune(player, collectionIndex, runeIndex, requestedAmount);
                refreshMechanicProgress(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная команда усиления."));
            }
        } else if (action.startsWith("upgrade_rune:")) {
            String rawIndex = action.substring("upgrade_rune:".length());
            try {
                awakening.awakenRune(player, collectionIndex, Integer.parseInt(rawIndex));
                evaluateCollectionUnlock(player);
                refreshMechanicProgress(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная руна."));
            }
        } else if (action.startsWith("revert_rune:")) {
            String rawIndex = action.substring("revert_rune:".length());
            try {
                revert.revertRune(player, collectionIndex, Integer.parseInt(rawIndex));
                refreshMechanicProgress(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная руна."));
            }
        } else if (action.startsWith("share_rune:")) {
            try {
                shareRune(player, collectionIndex, Integer.parseInt(action.substring("share_rune:".length())));
            } catch (NumberFormatException ignored) {
                // Invalid client identifier: no chat message and no gameplay mutation.
            }
        } else if (action.equals("get_player_stats")) {
            stats.sendTo(player);
        } else if (action.equals("get_balance")) {
            int balance = players.getBalance(player.getUniqueID());
            player.sendMessage(new TextComponentString("§6Твой баланс: " + balance + " монет"));
            NetworkHandler.sendToClient(new PongPacket(balance), player);
        } else if (action.equals("load_cards")) {
            sync.sendRunes(player, collectionIndex);
        } else if (action.equals("load_decks")) {
            evaluateCollectionUnlock(player);
            refreshMechanicProgress(player);
            sync.sendCollectionState(player);
        } else if (action.startsWith("switch_deck:")) {
            String rawIndex = action.substring("switch_deck:".length());
            try {
                int targetCollection = Integer.parseInt(rawIndex);
                RuneCollectionId targetId = RuneCollectionId.fromLegacyIndex(targetCollection);
                if (targetId == null) {
                    player.sendMessage(new TextComponentString("§cТакой коллекции не существует."));
                    sync.sendCollectionState(player);
                    return;
                }
                if (!isCollectionUnlocked(player, targetCollection)) {
                    player.sendMessage(new TextComponentString("§cЭта коллекция ещё не открыта."));
                    sync.sendCollectionState(player);
                    return;
                }
                players.setActiveCollection(player.getUniqueID(), targetCollection);
                sync.sendCollectionState(player);
                sync.sendRunes(player, targetCollection);
                stats.sendTo(player);
                player.sendMessage(new TextComponentString("§aАктивная коллекция: §e" + (targetCollection + 1)));
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректный номер коллекции."));
            }
        }
    }

    private void shareRune(EntityPlayerMP player, int collectionIndex, int runeIndex) {
        RuneCollectionId collection = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collection == null || runeIndex < 0 || runeIndex >= 26
                || !isCollectionUnlocked(player, collectionIndex)) return;
        RuneRecord owned = null;
        for (RuneRecord record : players.getRunesInCollection(player.getUniqueID(), collectionIndex)) {
            if (record.runeIndex == runeIndex && (owned == null || record.layer > owned.layer)) owned = record;
        }
        if (owned == null) return;

        int rank = owned.layer;
        int enhancement = sync.enhancementLevel(player.getUniqueID(), collectionIndex, runeIndex);
        double enhancementMultiplier = sync.enhancementMultiplier(collection, enhancement);
        String name;
        StringBuilder details = new StringBuilder();
        if (collectionIndex == 0) {
            PrimaryRuneCatalog.Definition rune = PrimaryRuneCatalog.at(runeIndex);
            if (rune == null) return;
            name = rune.name;
            details.append("Type: ").append(rune.type);
            for (RuneStat stat : rune.stats)
                details.append("\nEffect: ").append(stat.type.displayName).append(" ")
                        .append(stat.type.format(stat.resolve(rank, enhancementMultiplier).finalValue));
            for (RuneTargetedPower power : rune.targetedPowers)
                details.append("\nTarget: ").append(power.tooltipLine(rank, enhancementMultiplier));
            for (RuneCompositeEffect effect : rune.compositeEffects)
                for (String line : effect.tooltipLines(rank, enhancementMultiplier))
                    details.append("\nEffect: ").append(line);
        } else {
            name = "Rune " + (runeIndex + 1);
            for (RuneTechnicalCatalog.EffectLabel effect : RuneTechnicalCatalog.effects(collection, runeIndex)) {
                details.append(details.length() == 0 ? "" : "\n").append("Effect: ")
                        .append(effect.type).append(effect.channel == null ? "" : " (" + effect.channel.name() + ")");
            }
        }
        String tooltip = name + "\nCollection: " + collection.id() + "\nRank: " + rank
                + "\nEnhancement: +" + enhancement + (details.length() == 0 ? "" : "\n" + details);
        ShareChatDelivery.send(player, "Rune: " + name, tooltip);
    }

    private boolean isCollectionUnlocked(EntityPlayerMP player, int collectionIndex) {
        if (collectionUnlocks == null) return true;
        RuneCollectionId id = RuneCollectionId.fromLegacyIndex(collectionIndex);
        return collectionUnlocks.isUnlocked(player.getUniqueID(), id);
    }

    private void evaluateCollectionUnlock(EntityPlayerMP player) {
        if (collectionUnlocks == null) return;
        RuneCollectionId unlocked = collectionUnlocks.evaluateNextUnlock(player.getUniqueID());
        if (unlocked == null) return;
        player.sendMessage(new TextComponentString("§aОткрыта коллекция рун §e" + (unlocked.legacyIndex() + 1) + "§a!"));
        sync.sendCollectionState(player);
    }

    private void refreshMechanicProgress(EntityPlayerMP player) {
        if (mechanicUnlocks == null) return;
        // MechanicUnlockService.refreshRuneProgress reads all seven rune
        // collections. Consecutive purchases still mutate synchronously and
        // in order; only their redundant derived progress refreshes are merged.
        java.util.UUID playerId = player.getUniqueID();
        mechanicProgressRefreshes.request(playerId,
                task -> player.getServerWorld().addScheduledTask(task),
                () -> mechanicUnlocks.refreshRuneProgress(playerId));
    }

    private static boolean isImportantPlayerAction(String action) {
        return action.equals("buy_card")
                || action.startsWith("buy_cards:")
                || action.startsWith("enhance_rune:")
                || action.startsWith("upgrade_rune:")
                || action.startsWith("revert_rune:");
    }

    private static String logActionName(String action) {
        if (action == null) return "null";
        if (action.equals("buy_card")) return "buy_rune";
        if (action.startsWith("buy_cards:")) return "buy_runes:" + action.substring("buy_cards:".length());
        if (action.equals("load_cards")) return "load_runes";
        if (action.equals("load_decks")) return "load_collections";
        if (action.startsWith("switch_deck:")) return "switch_collection:" + action.substring("switch_deck:".length());
        return action;
    }

    private boolean isSupportedBulkAmount(int amount) {
        return amount == 1 || amount == 5 || amount == 10 || amount == 100;
    }
}
