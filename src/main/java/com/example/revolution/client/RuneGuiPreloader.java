package com.example.revolution.client;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PingPacket;
import com.example.revolution.network.RuneSlotPacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Collects the first GUI snapshot before the rune screen becomes visible. */
@SideOnly(Side.CLIENT)
final class RuneGuiPreloader {
    private static final long RUNE_SETTLE_MS = 120L;
    private static final long FALLBACK_TIMEOUT_MS = 1500L;
    private static final long POST_OPEN_REFRESH_TIMEOUT_MS = 750L;

    private static boolean pendingOpen;
    private static long startedAt;
    private static boolean statsReady;
    private static boolean collectionsReady;
    private static boolean runesStarted;
    private static long lastRunePacketAt;
    private static List<String> collectionNames = Collections.emptyList();
    private static int activeCollection;
    private static final List<RuneSnapshot> runes = new ArrayList<>();

    private static boolean suppressInitialRefresh;
    private static boolean refreshStarted;
    private static long openedAt;
    private static long lastRefreshPacketAt;

    private RuneGuiPreloader() {}

    static void beginOpen() {
        pendingOpen = true;
        startedAt = System.currentTimeMillis();
        statsReady = false;
        collectionsReady = false;
        runesStarted = false;
        lastRunePacketAt = 0L;
        collectionNames = Collections.emptyList();
        activeCollection = 0;
        runes.clear();
        ClientRuneCollections.reset();
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

    static void acceptCollections(List<String> names, int selectedCollection) {
        if (!pendingOpen) return;
        collectionNames = names == null ? Collections.emptyList() : new ArrayList<>(names);
        activeCollection = selectedCollection;
        collectionsReady = true;
    }

    static void acceptRune(RuneSlotPacket packet) {
        if (!pendingOpen) return;
        runesStarted = true;
        lastRunePacketAt = System.currentTimeMillis();
        if (packet.getSlot() == -1) {
            runes.clear();
            return;
        }
        runes.add(new RuneSnapshot(packet.getSlot(), packet.getRuneIndex(), packet.getLayer()));
    }

    static boolean shouldSuppressLiveRune(RuneSlotPacket packet) {
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
            boolean refreshSettled = refreshStarted && now - lastRefreshPacketAt >= RUNE_SETTLE_MS;
            boolean refreshTimedOut = openedAt > 0L && now - openedAt >= POST_OPEN_REFRESH_TIMEOUT_MS;
            if (refreshSettled || refreshTimedOut) suppressInitialRefresh = false;
        }

        if (!pendingOpen) return;

        boolean runesSettled = runesStarted && now - lastRunePacketAt >= RUNE_SETTLE_MS;
        boolean complete = statsReady && collectionsReady && runesSettled;
        boolean timedOut = now - startedAt >= FALLBACK_TIMEOUT_MS;
        if (!complete && !timedOut) return;

        pendingOpen = false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        RuneResourcesGuiScreen screen = new RuneResourcesGuiScreen();
        mc.displayGuiScreen(screen);
        if (!ClientRuneCollections.applyTo(screen)) {
            screen.setCollections(collectionNames, activeCollection);
        }
        screen.clearRunes();
        for (RuneSnapshot rune : runes) {
            screen.addRuneFromServer(rune.slot, rune.runeIndex, rune.layer);
        }
        RuneCollectionSyncState.completeLoad();

        suppressInitialRefresh = true;
        refreshStarted = false;
        openedAt = now;
        lastRefreshPacketAt = 0L;
    }

    static boolean isPendingOpen() {
        return pendingOpen;
    }

    private static final class RuneSnapshot {
        final int slot;
        final int runeIndex;
        final int layer;

        RuneSnapshot(int slot, int runeIndex, int layer) {
            this.slot = slot;
            this.runeIndex = runeIndex;
            this.layer = layer;
        }
    }
}
