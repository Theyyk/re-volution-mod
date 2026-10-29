package com.example.revolution.client.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Compact bottom-left RPG notification feed for server/system messages. */
@SideOnly(Side.CLIENT)
public final class NotificationFeedRenderer {
    private static final int MAX_VISIBLE = 3;
    private static final int MAX_TEXT_WIDTH = 220;
    private static final long LIFETIME_MS = 5000L;
    private static final long FADE_MS = 700L;
    private final Deque<Entry> entries = new ArrayDeque<>();

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.getMessage() == null) return;

        String plain = TextFormatting.getTextWithoutFormattingCodes(event.getMessage().getFormattedText());
        boolean legacyRuneStatus = plain != null && plain.startsWith("Активная колода:");
        if (event.getType() != ChatType.SYSTEM && !legacyRuneStatus) return;

        event.setCanceled(true);
        push(event.getMessage().getFormattedText());
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        long now = System.currentTimeMillis();
        prune(now);
        if (entries.isEmpty()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) return;
        FontRenderer font = minecraft.fontRenderer;
        ScaledResolution resolution = event.getResolution();

        int x = 8;
        int bottom = resolution.getScaledHeight() - 8;
        List<Entry> visible = new ArrayList<>(entries);
        for (int i = visible.size() - 1; i >= 0; i--) {
            Entry entry = visible.get(i);
            List<String> lines = font.listFormattedStringToWidth(entry.text, MAX_TEXT_WIDTH);
            int blockHeight = 8 + lines.size() * (font.FONT_HEIGHT + 1);
            int y = bottom - blockHeight;
            int alpha = alpha(entry, now);
            int background = (alpha * 150 / 255) << 24;
            int accent = (alpha * 190 / 255) << 24 | 0x5E8FB3;

            Gui.drawRect(x, y, x + MAX_TEXT_WIDTH + 12, bottom, background);
            Gui.drawRect(x, y, x + 2, bottom, accent);

            int textY = y + 4;
            int textColor = (alpha << 24) | 0xE6E6E6;
            for (String line : lines) {
                font.drawStringWithShadow(line, x + 7, textY, textColor);
                textY += font.FONT_HEIGHT + 1;
            }
            bottom = y - 4;
        }
    }

    private void push(String text) {
        if (text == null || text.trim().isEmpty()) return;
        entries.addLast(new Entry(text, System.currentTimeMillis()));
        while (entries.size() > MAX_VISIBLE) entries.removeFirst();
    }

    private void prune(long now) {
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            if (now - entry.createdAt >= LIFETIME_MS) iterator.remove();
        }
    }

    private static int alpha(Entry entry, long now) {
        long age = now - entry.createdAt;
        long fadeStart = LIFETIME_MS - FADE_MS;
        if (age <= fadeStart) return 255;
        double remaining = Math.max(0.0D, (double) (LIFETIME_MS - age) / (double) FADE_MS);
        return Math.max(0, Math.min(255, (int) Math.round(255.0D * remaining)));
    }

    private static final class Entry {
        private final String text;
        private final long createdAt;

        private Entry(String text, long createdAt) {
            this.text = text;
            this.createdAt = createdAt;
        }
    }
}
