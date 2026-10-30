package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import com.example.revolution.network.PlayerStatsPacket;
import com.example.revolution.network.RuneCollectionMetadataPacket;
import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.rune.config.RuneStatusConfig;
import com.example.revolution.rune.state.RuneInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * Narrow public boundary between client adapters and the package-private GUI/runtime implementation.
 * Transport and input adapters delegate here instead of widening internal GUI state visibility.
 */
public final class ClientUiBridge {
    private ClientUiBridge() {}

    public static void tickRunePreloader() {
        RuneGuiPreloader.tick();
    }

    public static boolean isRuneOpenPending() {
        return RuneGuiPreloader.isPendingOpen();
    }

    public static void beginRuneOpen() {
        RuneGuiPreloader.beginOpen();
    }

    public static boolean isRunesScreen(GuiScreen screen) {
        return screen instanceof RunesGuiScreen;
    }

    public static void acceptLegacyCoins(int balance) {
        ClientPlayerStats.acceptLegacyCoins(balance);
        RevolutionMod.logger.info("Legacy coins sync received on client: " + balance);
    }

    public static void acceptRune(RuneSlotPacket message) {
        RuneGuiPreloader.acceptRune(message);
        ClientRuneRanks.accept(message);
        RuneCollectionSyncState.notePacket();

        if (message.isAnimate()
                && RuneStatusConfig.isRuneStatus(ClientPlayerStats.getActiveRuneStatusId())
                && message.getRuneIndex() >= 0
                && message.getRuneIndex() < RuneInventory.BOOST_TYPE_COUNT) {
            RuneUxUiHandler.notePurchase(message.getRuneIndex());
        }

        if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
            if (RuneGuiPreloader.shouldSuppressLiveRune(message)) return;

            RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
            if (message.getSlot() == -1) {
                screen.clearRunes();
            } else {
                screen.addRuneFromServer(message.getSlot(), message.getRuneIndex(), message.getLayer());
            }
        }
    }

    public static void acceptCollections(RuneCollectionsPacket message) {
        ClientRuneEnhancements.suppressNextPulse();
        RuneGuiPreloader.acceptCollections(message.getCollectionNames(), message.getActiveCollection());

        if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
            RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
            screen.setCollections(message.getCollectionNames(), message.getActiveCollection());
        }
    }

    public static void acceptCollectionMetadata(RuneCollectionMetadataPacket message) {
        ClientRuneCollections.accept(message);
        if (Minecraft.getMinecraft().currentScreen instanceof RunesGuiScreen) {
            RunesGuiScreen screen = (RunesGuiScreen) Minecraft.getMinecraft().currentScreen;
            screen.setCollections(ClientRuneCollections.selectorNames(), ClientRuneCollections.activeCollection());
        }
    }

    public static void acceptPlayerStats(PlayerStatsPacket message) {
        ClientPlayerStats.update(
                message.getCoins(),
                message.getCrystals(),
                message.getRuneDust(),
                message.getRuneShards(),
                message.getRuneCores(),
                message.getTotalDamage(),
                message.getActiveRuneStatusId(),
                message.getPurchaseResource(),
                message.getPurchaseUnitCost(),
                message.getAwakeningResource(),
                message.getAwakeningNormalCost(),
                message.getAwakeningImprovedCost()
        );
        ClientPlayerStats.setResolvedCombat(message.getResolvedClickDamage(), message.getResolvedPoisonDamage());
        RuneGuiPreloader.noteStats();
        RevolutionMod.logger.info("Player stats updated on client: coins=" + message.getCoins()
                + ", crystals=" + message.getCrystals()
                + ", runeDust=" + message.getRuneDust()
                + ", runeShards=" + message.getRuneShards()
                + ", runeCores=" + message.getRuneCores()
                + ", damage=" + message.getTotalDamage()
                + ", resolvedClickDamage=" + message.getResolvedClickDamage()
                + ", resolvedPoisonDamage=" + message.getResolvedPoisonDamage()
                + ", activeRuneStatusId=" + message.getActiveRuneStatusId()
                + ", purchaseResource=" + message.getPurchaseResource()
                + ", purchaseUnitCost=" + message.getPurchaseUnitCost()
                + ", awakeningResource=" + message.getAwakeningResource()
                + ", awakeningNormalCost=" + message.getAwakeningNormalCost()
                + ", awakeningImprovedCost=" + message.getAwakeningImprovedCost());
    }

    public static void acceptEnhancement(RuneEnhancementPacket message) {
        ClientRuneEnhancements.accept(message);
    }
}
