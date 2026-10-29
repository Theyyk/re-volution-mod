package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PongPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import java.util.List;
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

    public RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneSyncService sync) {
        this(players, stats, purchase, enhancement, awakening,
                new RuneRankRevertService(players, stats, sync, purchase.revertBalanceRules()), sync);
    }

    RuneActionService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RunePurchaseService purchase,
            RuneEnhancementService enhancement,
            RuneAwakeningService awakening,
            RuneRankRevertService revert,
            RuneSyncService sync) {
        this.players = players;
        this.stats = stats;
        this.purchase = purchase;
        this.enhancement = enhancement;
        this.awakening = awakening;
        this.revert = revert;
        this.sync = sync;
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

        List<String> collectionNames = players.getCollectionNames(player.getUniqueID());
        int collectionIndex = players.getActiveCollection(player.getUniqueID());

        if (!collectionNames.isEmpty() && (collectionIndex < 0 || collectionIndex >= collectionNames.size())) {
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
        } else if (action.startsWith("buy_cards:")) {
            String rawAmount = action.substring("buy_cards:".length());

            if (rawAmount.equalsIgnoreCase("all")) {
                purchase.buyRunes(player, collectionIndex, Integer.MAX_VALUE, true);
                evaluateCollectionUnlock(player);
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
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная команда усиления."));
            }
        } else if (action.startsWith("upgrade_rune:")) {
            String rawIndex = action.substring("upgrade_rune:".length());
            try {
                awakening.awakenRune(player, collectionIndex, Integer.parseInt(rawIndex));
                evaluateCollectionUnlock(player);
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная руна."));
            }
        } else if (action.startsWith("revert_rune:")) {
            String rawIndex = action.substring("revert_rune:".length());
            try {
                revert.revertRune(player, collectionIndex, Integer.parseInt(rawIndex));
            } catch (NumberFormatException e) {
                player.sendMessage(new TextComponentString("§cНекорректная руна."));
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
            sync.sendCollectionState(player);
        } else if (action.startsWith("switch_deck:")) {
            String rawIndex = action.substring("switch_deck:".length());
            try {
                int targetCollection = Integer.parseInt(rawIndex);
                RuneCollectionId targetId = RuneCollectionId.fromLegacyIndex(targetCollection);
                List<String> collections = players.getCollectionNames(player.getUniqueID());
                if (targetId == null || targetCollection < 0 || targetCollection >= collections.size()) {
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
