package com.example.revolution.rune.service;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.player.service.PlayerStatsSyncService;
import com.example.revolution.resource.definition.ResourceDefinitions;
import com.example.revolution.resource.service.ResourceService;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneAwakeningDecision;
import com.example.revolution.rune.rules.RuneProgression;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public final class RuneAwakeningService {
    private final PlayerRepository players;
    private final ResourceService resources;
    private final PlayerStatsSyncService stats;
    private final RuneAwakeningPaymentService paymentService;
    private final RuneSyncService sync;

    public RuneAwakeningService(
            PlayerRepository players,
            ResourceService resources,
            PlayerStatsSyncService stats,
            RuneAwakeningPaymentService paymentService,
            RuneSyncService sync) {
        this.players = players;
        this.resources = resources;
        this.stats = stats;
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
        int ownedCount = 0;
        for (RuneRecord rune : runes) {
            if (rune.runeIndex < 0 || rune.runeIndex >= RuneInventory.BOOST_TYPE_COUNT) continue;
            if (!owned[rune.runeIndex]) {
                owned[rune.runeIndex] = true;
                ownedCount++;
            }
            bestRanks[rune.runeIndex] = Math.max(bestRanks[rune.runeIndex], RuneProgression.clampRank(rune.layer));
        }

        if (ownedCount < RuneInventory.BOOST_TYPE_COUNT) {
            player.sendMessage(new TextComponentString("§eСначала заполни все 26 слотов. Сейчас: " + ownedCount + " / 26."));
            return;
        }
        if (!owned[runeIndex]) {
            player.sendMessage(new TextComponentString("§cЭта руна ещё не получена."));
            return;
        }

        RuneAwakeningDecision decision = RuneAwakeningDecision.evaluate(bestRanks, runeIndex);
        if (decision.reason == RuneAwakeningDecision.Reason.MAXIMUM_RANK) {
            player.sendMessage(new TextComponentString("§6Эта руна уже продвинутая."));
            return;
        }
        if (!decision.isAllowed()) {
            if (decision.reason == RuneAwakeningDecision.Reason.COLLECTION_BEHIND) {
                player.sendMessage(new TextComponentString("§eЧтобы пробудить эту руну дальше, сначала доведите все 26 рун минимум до ранга «"
                        + RuneProgression.rankName(decision.currentRank) + "»."));
            } else {
                player.sendMessage(new TextComponentString("§cЭту руну сейчас нельзя пробудить."));
            }
            return;
        }

        int currentRank = decision.currentRank;
        int newRank = decision.nextRank;
        RuneAwakeningPaymentService.Result payment = paymentService.charge(player.getUniqueID(), statusId, currentRank);
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

        players.clearCollection(player.getUniqueID(), collectionIndex);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (rune == runeIndex) continue;
            if (bestRanks[rune] <= currentRank) continue;
            players.addRuneToCollection(player.getUniqueID(), collectionIndex, rune, rune, bestRanks[rune], 1);
        }
        players.addRuneToCollection(player.getUniqueID(), collectionIndex, runeIndex, runeIndex, newRank, 1);

        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), player);
        for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
            if (rune == runeIndex) continue;
            if (bestRanks[rune] <= currentRank) continue;
            NetworkHandler.sendToClient(new RuneSlotPacket(rune, rune, bestRanks[rune], false), player);
        }
        NetworkHandler.sendToClient(new RuneSlotPacket(runeIndex, runeIndex, newRank, false), player);
        sync.sendEnhancements(player, collectionIndex);
        stats.sendTo(player);

        String paymentText = payment.paid ? " §7Стоимость: §e" + payment.cost + " §f" + ResourceDefinitions.getDisplayName(payment.resource) + "§7." : "";
        player.sendMessage(new TextComponentString("§aРуна пробуждена: §f" + RuneProgression.rankName(currentRank)
                + " §7→ §f" + RuneProgression.rankName(newRank) + "§a. Руны ранга «" + RuneProgression.rankName(currentRank)
                + "» сброшены, более высокие ранги сохранены. Усиления слотов сохранены." + paymentText));
    }
}
