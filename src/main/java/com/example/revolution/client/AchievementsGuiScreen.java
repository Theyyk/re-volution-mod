package com.example.revolution.client;

import com.example.revolution.network.AchievementActionPacket;
import com.example.revolution.network.AchievementSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional-first Achievement tab: categories, progression, rewards, Claim, Share and meta header. */
public final class AchievementsGuiScreen extends GuiScreen {
    private static final int RUNES = 200, CLOSE = 299, PREV = 510, NEXT = 511, CATEGORY_BASE = 600;
    private static final String[] CATEGORIES = {"ALL", "DAMAGE", "RESOURCES", "MASTERY", "PETS", "RUNES", "KILLS", "LOCATIONS", "AURAS", "HIDDEN"};
    private static final int ROW_H = 46;
    private String category = "ALL";
    private int page;

    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(RUNES, 8, 8, 70, 20, "Runes"));
        buttonList.add(new GuiButton(CLOSE, width - 28, 8, 20, 20, "X"));
        int x = 8, y = 36;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int w = Math.max(48, fontRenderer.getStringWidth(CATEGORIES[i]) + 12);
            if (x + w > width - 8) { x = 8; y += 23; }
            GuiButton button = new GuiButton(CATEGORY_BASE + i, x, y, w, 20, CATEGORIES[i]);
            button.enabled = !CATEGORIES[i].equals(category);
            buttonList.add(button);
            x += w + 4;
        }
        buttonList.add(new GuiButton(PREV, width / 2 - 105, height - 28, 100, 20, "< Previous"));
        buttonList.add(new GuiButton(NEXT, width / 2 + 5, height - 28, 100, 20, "Next >"));
        NetworkHandler.INSTANCE.sendToServer(new AchievementActionPacket("sync", "", 0));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == CLOSE) { mc.displayGuiScreen(null); return; }
        if (button.id == RUNES) { ClientUiBridge.beginRuneOpen(); mc.displayGuiScreen(null); return; }
        if (button.id == PREV) { page = Math.max(0, page - 1); return; }
        if (button.id == NEXT) { page++; return; }
        if (button.id >= CATEGORY_BASE && button.id < CATEGORY_BASE + CATEGORIES.length) {
            category = CATEGORIES[button.id - CATEGORY_BASE];
            page = 0;
            initGui();
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        List<AchievementSnapshotPacket.Entry> visible = filtered();
        int top = contentTop();
        int perPage = perPage();
        int start = page * perPage;
        for (int row = 0; row < perPage && start + row < visible.size(); row++) {
            AchievementSnapshotPacket.Entry e = visible.get(start + row);
            if (!e.revealed || e.achievementId.startsWith("hidden:")) continue;
            int y = top + row * ROW_H;
            if (mouseY < y || mouseY >= y + ROW_H - 4) continue;
            if (mouseX >= width - 154 && mouseX < width - 84) {
                if (e.completed && !e.claimed) NetworkHandler.INSTANCE.sendToServer(
                        new AchievementActionPacket("claim", e.achievementId, e.stageIndex));
                return;
            }
            if (mouseX >= width - 78 && mouseX < width - 8) {
                NetworkHandler.INSTANCE.sendToServer(new AchievementActionPacket("share", e.achievementId, e.stageIndex));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "ACHIEVEMENTS", width / 2, 12, 0xFFFFFF);
        String meta = "Points: " + ClientAchievements.points() + " | Level: " + ClientAchievements.level()
                + " | Bonus: +" + ClientAchievements.bonusPercent() + "% | Next: " + ClientAchievements.nextLevelThreshold();
        drawCenteredString(fontRenderer, meta, width / 2, 25, 0xD8D8D8);

        List<AchievementSnapshotPacket.Entry> visible = filtered();
        int perPage = perPage();
        int maxPage = Math.max(0, (visible.size() - 1) / perPage);
        if (page > maxPage) page = maxPage;
        int start = page * perPage;
        int top = contentTop();
        for (int row = 0; row < perPage && start + row < visible.size(); row++) {
            AchievementSnapshotPacket.Entry e = visible.get(start + row);
            int y = top + row * ROW_H;
            boolean hover = mouseY >= y && mouseY < y + ROW_H - 4;
            drawRect(8, y, width - 8, y + ROW_H - 4, hover ? 0xAA343A43 : 0xAA20242A);
            if (!e.revealed) {
                fontRenderer.drawString("???", 14, y + 8, 0xAAAAAA);
                fontRenderer.drawString("Hidden Achievement", 14, y + 22, 0x777777);
                continue;
            }
            String title = e.name + (e.stageRoman.isEmpty() ? "" : " " + e.stageRoman);
            fontRenderer.drawString(title, 14, y + 6, 0xFFFFFF);
            fontRenderer.drawString(e.progress + " / " + e.target + "   [" + e.category + "]", 14, y + 18, e.completed ? 0x8FE3A5 : 0xC7CDD5);
            String reward = "Rewards: " + e.crystals + " Crystals, " + e.offerings + " Offerings, " + e.auraKeys
                    + " Aura Keys, " + e.achievementPoints + " AP";
            fontRenderer.drawString(reward, 14, y + 30, 0xAEB7C2);
            int claimColor = e.claimed ? 0xFF555555 : e.completed ? 0xFF2E7547 : 0xFF3B414A;
            drawRect(width - 154, y + 9, width - 84, y + 31, claimColor);
            drawCenteredString(fontRenderer, e.claimed ? "Claimed" : e.completed ? "Claim" : "Locked", width - 119, y + 16, 0xFFFFFF);
            drawRect(width - 78, y + 9, width - 8, y + 31, 0xFF4B5570);
            drawCenteredString(fontRenderer, "Share", width - 43, y + 16, 0xFFFFFF);
        }
        drawCenteredString(fontRenderer, "Page " + (page + 1) + "/" + (maxPage + 1), width / 2, height - 39, 0xAEB7C2);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private List<AchievementSnapshotPacket.Entry> filtered() {
        List<AchievementSnapshotPacket.Entry> result = new ArrayList<>();
        for (AchievementSnapshotPacket.Entry entry : ClientAchievements.entries()) {
            if ("ALL".equals(category) || category.equals(entry.category)) result.add(entry);
        }
        return result;
    }

    private int contentTop() { return width < 680 ? 105 : 82; }
    private int perPage() { return Math.max(1, (height - contentTop() - 48) / ROW_H); }
    @Override public boolean doesGuiPauseGame() { return false; }
}
