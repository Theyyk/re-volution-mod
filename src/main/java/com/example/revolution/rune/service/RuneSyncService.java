package com.example.revolution.rune.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.state.RuneInventory;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;

public final class RuneSyncService {
    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;

    public RuneSyncService(PlayerRepository players, RuneEnhancementRepository enhancements) {
        this.players = players;
        this.enhancements = enhancements;
    }

    public void sendCollectionState(EntityPlayerMP player) {
        List<String> collections = players.getCollectionNames(player.getUniqueID());
        int activeCollection = players.getActiveCollection(player.getUniqueID());
        NetworkHandler.sendToClient(RunePacketFactory.collections(collections, activeCollection), player);
    }

    public void sendRunes(EntityPlayerMP player, int collectionIndex) {
        NetworkHandler.sendToClient(new RuneSlotPacket(-1, 0, 0, false), player);
        List<RuneRecord> runes = players.getRunesInCollection(player.getUniqueID(), collectionIndex);
        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        RevolutionMod.logger.info("Sending runes to client for status " + statusId + ": " + runes.size());
        for (RuneRecord rune : runes) {
            NetworkHandler.sendToClient(new RuneSlotPacket(rune.slot, rune.runeIndex, rune.layer, false), player);
        }
        sendEnhancements(player, collectionIndex);
    }

    public void sendEnhancements(EntityPlayerMP player, int collectionIndex) {
        int statusId = RuneStatusConfig.statusForCollection(collectionIndex);
        int[] levels = RuneStatusConfig.isRuneStatus(statusId)
                ? enhancements.getLevels(player.getUniqueID(), collectionIndex)
                : new int[RuneInventory.BOOST_TYPE_COUNT];
        NetworkHandler.sendToClient(RunePacketFactory.enhancements(levels), player);
    }
}
