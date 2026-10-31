package com.example.revolution.client;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.QuestActionPacket;
import com.example.revolution.network.QuestSnapshotPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional v2.7.0 Quest UI. Visual polish is intentionally deferred. */
public final class QuestGuiScreen extends GuiScreen {
    private final List<QuestSnapshotPacket.Entry> entries;
    private final long cooldownUntil;
    private final int rerollsRemaining;
    private final long completedQuestCount;

    public QuestGuiScreen(List<QuestSnapshotPacket.Entry> entries, long cooldownUntil, int rerollsRemaining, long completedQuestCount) {
        this.entries = entries == null ? new ArrayList<QuestSnapshotPacket.Entry>() : new ArrayList<>(entries);
        this.cooldownUntil = Math.max(0L, cooldownUntil);
        this.rerollsRemaining = Math.max(0, rerollsRemaining);
        this.completedQuestCount = Math.max(0L, completedQuestCount);
    }

    @Override public void initGui() {
        buttonList.clear();
        int left = width / 2 - 150;
        int top = height / 2 - 92;
        for (int i = 0; i < entries.size(); i++) {
            QuestSnapshotPacket.Entry entry = entries.get(i);
            if ("COMPLETED".equals(entry.state)) {
                buttonList.add(new GuiButton(100 + i, left + 218, top + i * 48 + 15, 75, 20, "Claim +" + entry.crystalReward));
            }
        }
        GuiButton reroll = new GuiButton(10, left, top + 154, 120, 20, "Reroll (" + rerollsRemaining + ")");
        reroll.enabled = rerollsRemaining > 0 && hasActive();
        buttonList.add(reroll);
        buttonList.add(new GuiButton(11, left + 128, top + 154, 80, 20, "Refresh"));
        buttonList.add(new GuiButton(12, left + 216, top + 154, 77, 20, "Close"));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 100 && button.id < 100 + entries.size()) {
            QuestSnapshotPacket.Entry entry = entries.get(button.id - 100);
            NetworkHandler.INSTANCE.sendToServer(new QuestActionPacket("claim", entry.entryId));
            return;
        }
        if (button.id == 10) NetworkHandler.INSTANCE.sendToServer(new QuestActionPacket("reroll"));
        else if (button.id == 11) NetworkHandler.INSTANCE.sendToServer(new QuestActionPacket("sync"));
        else if (button.id == 12) mc.displayGuiScreen(null);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 150;
        int top = height / 2 - 92;
        drawCenteredString(fontRenderer, "Quests", width / 2, top - 14, 0xFFFFFF);
        drawString(fontRenderer, "Completed: " + completedQuestCount, left, top - 2, 0xAAAAAA);
        for (int i = 0; i < entries.size(); i++) {
            QuestSnapshotPacket.Entry entry = entries.get(i);
            int y = top + i * 48 + 12;
            drawRect(left, y - 7, left + 300, y + 34, 0x88000000);
            drawString(fontRenderer, entry.templateId + " [T" + entry.difficultyTier + "]", left + 7, y, 0xFFFFFF);
            drawString(fontRenderer, entry.progress + " / " + entry.target + "   " + entry.state, left + 7, y + 12, stateColor(entry.state));
            drawString(fontRenderer, "Reward: " + entry.crystalReward + " crystals", left + 7, y + 24, 0x55FFFF);
        }
        if (cooldownUntil > System.currentTimeMillis()) {
            long seconds = Math.max(0L, (cooldownUntil - System.currentTimeMillis() + 999L) / 1000L);
            drawString(fontRenderer, "Next cooldown refresh: " + formatDuration(seconds), left, top + 142, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private boolean hasActive() {
        for (QuestSnapshotPacket.Entry entry : entries) if ("ACTIVE".equals(entry.state)) return true;
        return false;
    }

    private static int stateColor(String state) {
        if ("COMPLETED".equals(state)) return 0x55FF55;
        if ("COOLDOWN".equals(state)) return 0xAAAAAA;
        return 0xFFFF55;
    }

    private static String formatDuration(long seconds) {
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
