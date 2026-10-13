package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Collects the first GUI snapshot before the rune screen becomes visible. */
@SideOnly(Side.CLIENT)
final class RuneGuiPreloader {
    private static final long CARD_SETTLE_MS = 120L;
    private static final long FALLBACK_TIMEOUT_MS = 1500L;
    private static final long POST_OPEN_REFRESH_TIMEOUT_MS = 750L;

    private static boolean pendingOpen;
    private static long startedAt;
    private static boolean statsReady;
    private static boolean decksReady;
    private static boolean cardsStarted;
    private static long lastCardPacketAt;
    private static List<String> deckNames = Collections.emptyList();
    private static int activeDeck;
    private static final List<CardSnapshot> cards = new ArrayList<>();

    private static boolean suppressInitialRefresh;
    private static boolean refreshStarted;
    private static long openedAt;
    private static long lastRefreshPacketAt;

    private RuneGuiPreloader() {}

    static void beginOpen() {
        pendingOpen = true;
        startedAt = System.currentTimeMillis();
        statsReady = false;
        decksReady = false;
        cardsStarted = false;
        lastCardPacketAt = 0L;
        deckNames = Collections.emptyList();
        activeDeck = 0;
        cards.clear();
        suppressInitialRefresh = false;
        refreshStarted = false;
        openedAt = 0L;
        lastRefreshPacketAt = 0L;

        NetworkHandler.INSTANCE.sendToServer(new PingPacket("get_player_stats"));
        NetworkHandler.INSTANCE.sendToServer(new PingPacket("load_decks"));
        NetworkHandler.INSTANCE.sendToServer(new PingPacket("load_cards"));
    }

    static void noteStats() {
        if (pendingOpen) statsReady = true;
    }

    static void acceptDecks(List<String> names, int selectedDeck) {
        if (!pendingOpen) return;
        deckNames = names == null ? Collections.emptyList() : new ArrayList<>(names);
        activeDeck = selectedDeck;
        decksReady = true;
    }

    static void acceptCard(CardPacket packet) {
        if (!pendingOpen) return;
        cardsStarted = true;
        lastCardPacketAt = System.currentTimeMillis();
        if (packet.getSlot() == -1) {
            cards.clear();
            return;
        }
        cards.add(new CardSnapshot(packet.getSlot(), packet.getCardIndex(), packet.getLayer()));
    }

    static boolean shouldSuppressLiveCard(CardPacket packet) {
        if (!suppressInitialRefresh) return false;

        long now = System.currentTimeMillis();
        if (packet.getSlot() == -1) {
            refreshStarted = true;
            lastRefreshPacketAt = now;
            return true;
        }
        if (refreshStarted && !packet.isAnimate()) {
            lastRefreshPacketAt = now;
            return true;
        }
        return false;
    }

    static void tick() {
        long now = System.currentTimeMillis();

        if (suppressInitialRefresh) {
            boolean refreshSettled = refreshStarted && now - lastRefreshPacketAt >= CARD_SETTLE_MS;
            boolean refreshTimedOut = openedAt > 0L && now - openedAt >= POST_OPEN_REFRESH_TIMEOUT_MS;
            if (refreshSettled || refreshTimedOut) suppressInitialRefresh = false;
        }

        if (!pendingOpen) return;

        boolean cardsSettled = cardsStarted && now - lastCardPacketAt >= CARD_SETTLE_MS;
        boolean complete = statsReady && decksReady && cardsSettled;
        boolean timedOut = now - startedAt >= FALLBACK_TIMEOUT_MS;
        if (!complete && !timedOut) return;

        pendingOpen = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        RuneResourcesCardsGuiScreen screen = new RuneResourcesCardsGuiScreen();
        mc.displayGuiScreen(screen);
        screen.setDecks(deckNames, activeDeck);
        screen.clearCards();
        for (CardSnapshot card : cards) {
            screen.addCardFromServer(card.slot, card.cardIndex, card.layer, false);
        }
        CardSyncState.completeLoad();

        suppressInitialRefresh = true;
        refreshStarted = false;
        openedAt = now;
        lastRefreshPacketAt = 0L;
    }

    static boolean isPendingOpen() {
        return pendingOpen;
    }

    private static final class CardSnapshot {
        final int slot;
        final int cardIndex;
        final int layer;

        CardSnapshot(int slot, int cardIndex, int layer) {
            this.slot = slot;
            this.cardIndex = cardIndex;
            this.layer = layer;
        }
    }
}
