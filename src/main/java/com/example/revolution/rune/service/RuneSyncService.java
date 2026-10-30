package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;

public final class RuneSyncService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;
    private final RuneEnhancementBalanceService enhancementBalance;
    private final RuneCollectionUnlockService collectionUnlocks;

    public RuneSyncService(
            PlayerRepository players,
            RuneEnhancementRepository enhancements,
            RuneEnhancementBalanceService enhancementBalance) {
        this.players = players;
        this.enhancements = enhancements;
        this.enhancementBalance = enhancementBalance;
        this.collectionUnlocks = players instanceof RuneCollectionStateRepository
                ? new RuneCollectionUnlockService(players, (RuneCollectionStateRepository) players)
                : null;
    }

    public void sendCollectionState(EntityPlayerMP player) {
        List<String> collections = players.getCollectionNames(player.getUniqueID());
        int activeCollection = players.getActiveCollection(player.getUniqueID());
        NetworkHandler.sendToClient(RunePacketFactory.collections(collections, activeCollection), player);
        if (collectionUnlocks == null) return;

        List<Integer> progress = new ArrayList<>();
        for (RuneCollectionId collection : RuneCollectionId.values()) {
            progress.add(collectionUnlocks.improvedOrHigherProgress(player.getUniqueID(), collection));
        }
        NetworkHandler.sendToClient(
                RunePacketFactory.collectionMetadata(
                        collections,
                        activeCollection,
                        collectionUnlocks.unlocked(player.getUniqueID()),
                        progress,
                        collectionUnlocks.threshold()),
                player);
    }

    public void sendRunes(EntityPlayerMP player, int collectionIndex) {
        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), player);
        List<RuneRecord> runes = players.getRunesInCollection(player.getUniqueID(), collectionIndex);
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        RevolutionMod.logger.debug("Sending runes to client for collection "
                + (collectionId == null ? collectionIndex : collectionId.id()) + ": " + runes.size());
        for (RuneRecord rune : runes) {
            NetworkHandler.sendToClient(new RuneSlotPacket(rune.slot, rune.runeIndex, rune.layer, false), player);
        }
        if (collectionId != null) sendEnhancements(player, collectionIndex);
    }

    public void sendEnhancements(EntityPlayerMP player, int collectionIndex) {
        RuneCollectionId collectionId = RuneCollectionId.fromLegacyIndex(collectionIndex);
        if (collectionId == null) return;

        int[] levels = enhancements.getLevels(player.getUniqueID(), collectionIndex);
        int totalEnhancementLevel = enhancementBalance.totalEnhancementLevel(levels);
        BigDecimal[] bulkCosts = enhancementBalance.bulkCosts(
                collectionId,
                totalEnhancementLevel,
                RuneEnhancementPacket.BULK_AMOUNTS);
        NetworkHandler.sendToClient(
                RunePacketFactory.enhancements(
                        transportIdentity(collectionId),
                        enhancementBalance.resource(collectionId),
                        enhancementBalance.powerStep(collectionId),
                        levels,
                        bulkCosts),
                player);
    }

    private static int transportIdentity(RuneCollectionId collectionId) {
        // Preserve Collection 1's established client identity. New fixed collections only need a
        // distinct stable pulse key inside RuneEnhancementPacket; gameplay never reads this value.
        return collectionId == RuneCollectionId.COLLECTION_01 ? 1 : -(collectionId.legacyIndex() + 1);
    }
}
