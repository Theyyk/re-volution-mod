package com.example.revolution.rune.service;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.definition.RuneRankUpRule;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneAwakeningDecision;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneAwakeningService {
    private final PlayerRepository players;
    private final PlayerStatsSyncService stats;
    private final RuneRankUpBalanceService rankUpBalance;
    private final RuneAwakeningPaymentService paymentService;
    private final RuneSyncService sync;

    public RuneAwakeningService(
            PlayerRepository players,
            PlayerStatsSyncService stats,
            RuneRankUpBalanceService rankUpBalance,
            RuneAwakeningPaymentService paymentService,
            RuneSyncService sync) {
        this.players = players;
        this.stats = stats;
        this.rankUpBalance = rankUpBalance;
        this.paymentService = paymentService;
        this.sync = sync;
    }

    public void awakenRune(EntityPlayerMP player, int collectionIndex, int runeIndex) {
        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        if (!RuneStatusConfig.isRuneStatus(statusId)) {
            player.sendMessage(new TextComponentString("§cПробуждение недоступно для этой legacy-коллекции."));
            return;
        }
        if (runeIndex < 0 || runeIndex >= RuneInventory.BOOST_TYPE_COUNT) {
            player.sendMessage(new TextComponentString("§cНекорректная руна."));
            return;
        }

        List<RuneRecord> runes = players.getRunesInCollection(player.getUniqueID(), collectionIndex);
        boolean[] owned = new boolean[RuneInventory.BOOST_TYPE_COUNT];
        int[] bestRanks = new int[RuneInventory.BOOST_TYPE_COUNT];
        for (RuneRecord rune : runes) {
            if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
            owned[rune.runeIndex] = true;
            bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], RuneProgression.clampRank(rune.layer));
        }

        if (!owned[runeIndex]) {
            player.sendMessage(new TextComponentString("§cЭта руна ещё не получена."));
            return;
        }

        int currentRank = RuneProgression.clampRank(bestRanks[runeIndex]);
        if (RuneProgression.isMaximumRank(currentRank)) {
            player.sendMessage(new TextComponentString("§6Эта руна уже продвинутая."));
            return;
        }

        RuneRankUpRule rule;
        try {
            rule = rankUpBalance.rule(statusId, currentRank, countAbove(bestRanks, currentRank));
        } catch (RuntimeException e) {
            player.sendMessage(new TextComponentString("§cПробуждение временно недоступно: правило перехода настроено некорректно."));
            return;
        }

        RuneAwakeningDecision decision = RuneAwakeningDecision.evaluate(
                bestRanks,
                runeIndex,
                rule.targetRank,
                rule.requiredSlots);
        if (!decision.isAllowed()) {
            if (decision.reason == RuneAwakeningDecision.Reason.COLLECTION_BEHIND) {
                int ready = countAtLeast(bestRanks, decision.currentRank);
                player.sendMessage(new TextComponentString("§eЧтобы пробудить эту руну дальше, нужно рун минимум ранга «"
                        + RuneProgression.rankName(decision.currentRank) + "»: " + ready + " / " + rule.requiredSlots + "."));
            } else {
                player.sendMessage(new TextComponentString("§cЭту руну сейчас нельзя пробудить."));
            }
            return;
        }

        RuneAwakeningPaymentService.Result payment = paymentService.charge(player.getUniqueID(), rule);
        if (!payment.allowed) {
            stats.sendTo(player);
            if (payment.invalidConfiguration) {
                player.sendMessage(new TextComponentString("§cПробуждение временно недоступно: ресурс оплаты настроен некорректно."));
            } else {
                player.sendMessage(new TextComponentString("§cНедостаточно ресурса «" + ResourceDefinitions.getDisplayName(payment.resource)
                        + "» для пробуждения. Нужно " + payment.cost + ", у тебя " + payment.balance + "."));
            }
            return;
        }

        applyRankUp(player, collectionIndex, runeIndex, bestRanks, rule);
        stats.sendTo(player);

        String paymentText = payment.paid ? " §7Стоимость: §e" + payment.cost + " §f"
                + ResourceDefinitions.getDisplayName(payment.resource) + "§7." : "";
        player.sendMessage(new TextComponentString("§aРуна пробуждена: §f" + RuneProgression.rankName(rule.sourceRank)
                + " §7→ §f" + RuneProgression.rankName(rule.targetRank) + "§a. Руны ранга «"
                + RuneProgression.rankName(rule.sourceRank)
                + "» сброшены, более высокие ранги сохранены. Усиления слотов сохранены." + paymentText));
    }

    private void applyRankUp(
            EntityPlayerMP player,
            int collectionIndex,
            int runeIndex,
            int[] bestRanks,
            RuneRankUpRule rule) {
        if (rule.clearMode != RuneRankUpRule.ClearMode.AT_OR_BELOW_SOURCE || !rule.preserveEnhancements) {
            throw new IllegalStateException("unsupported rank-up application rule");
        }

        players.clearCollection(player.getUniqueID(), collectionIndex);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (rune == runeIndex) continue;
            if (bestRanks[rune] <= rule.sourceRank) continue;
            players.addRuneToCollection(player.getUniqueID(), collectionIndex, rune, rune, bestRanks[rune], 1);
        }
        players.addRuneToCollection(player.getUniqueID(), collectionIndex, runeIndex, runeIndex, rule.targetRank, 1);

        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), player);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (rune == runeIndex) continue;
            if (bestRanks[rune] <= rule.sourceRank) continue;
            NetworkHandler.sendToClient(new RuneSlotPacket(rune, rune, bestRanks[rune], false), player);
        }
        NetworkHandler.sendToClient(new RuneSlotPacket(runeIndex, runeIndex, rule.targetRank, false), player);
        sync.sendEnhancements(player, collectionIndex);
    }

    private int countAtLeast(int[] bestRanks, int rank) {
        int count = 0;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (bestRanks[rune] >= rank) count++;
        }
        return count;
    }

    private int countAbove(int[] bestRanks, int rank) {
        int count = 0;
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (bestRanks[rune] > rank) count++;
        }
        return count;
    }
}
