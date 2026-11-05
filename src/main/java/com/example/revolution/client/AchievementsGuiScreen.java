package com.example.revolution.client;

import com.example.revolution.achievement.AchievementStageLabels;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.AchievementActionPacket;
import com.example.revolution.network.AchievementSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/** Achievement categories, server rewards and meta levels in common E-menu chrome. */
public final class AchievementsGuiScreen extends GuiScreen {
    private static final int CATEGORY_BASE = 600;
    private static final String[] CATEGORIES =
            {"ALL", "DAMAGE", "RESOURCES", "MASTERY", "PETS", "RUNES", "KILLS", "LOCATIONS", "AURAS", "HIDDEN"};
    private static final String[] CATEGORY_LABELS =
            {"Все", "Урон", "Ресурсы", "Мастерство", "Питомцы", "Руны", "Убийства", "Локации", "Ауры", "Скрытые"};
    private static final int ROW_H = 70;
    private static long requestNonce = System.currentTimeMillis();
    private String category = "ALL";
    private int scrollRow;

    private int left() { return UnifiedMainMenuChrome.contentLeft(width, height); }
    private int right() { return UnifiedMainMenuChrome.contentRight(width, height); }
    private int top() { return UnifiedMainMenuChrome.contentTop(width, height); }

    private int categoryRowCount() {
        int x = left(), rows = 1;
        for (String label : CATEGORY_LABELS) {
            int w = Math.max(48, fontRenderer.getStringWidth(label) + 16);
            if (x + w > right()) { x = left(); rows++; }
            x += w + 4;
        }
        return rows;
    }

    private int contentTop() {
        return top() + categoryRowCount() * 23 + 44;
    }

    private int visibleRows() {
        return Math.max(1, (height - contentTop() - 42) / ROW_H);
    }

    private int maxScroll(List<AchievementSnapshotPacket.Entry> visible) {
        return Math.max(0, visible.size() - visibleRows());
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mx >= left() && mx < right() && my >= contentTop() && my < height-42)
            scrollRow = Math.max(0, Math.min(maxScroll(filtered()), scrollRow + (wheel < 0 ? 1 : -1)));
    }

    @Override public void initGui() {
        buttonList.clear();
        UnifiedMainMenuChrome.installNavigation(buttonList, width, height, UiMenuSection.ACHIEVEMENTS);
        int x = left(), y = top();
        for (int i = 0; i < CATEGORIES.length; i++) {
            int w = Math.max(48, fontRenderer.getStringWidth(CATEGORY_LABELS[i]) + 16);
            if (x + w > right()) { x = left(); y += 23; }
            GuiButton button = new GuiControls.PanelButton(
                    CATEGORY_BASE + i, x, y, w, 20, CATEGORY_LABELS[i]);
            button.enabled = !CATEGORIES[i].equals(category);
            buttonList.add(button);
            x += w + 4;
        }
        NetworkHandler.INSTANCE.sendToServer(new AchievementActionPacket("sync", "", 0, 0L));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.ACHIEVEMENTS, mc)) return;
        if (button.id >= CATEGORY_BASE && button.id < CATEGORY_BASE + CATEGORIES.length) {
            category = CATEGORIES[button.id - CATEGORY_BASE]; scrollRow = 0; initGui();
        }
    }

    private int claimX() { return Math.max(left(), right()-152); }
    private int shareX() { return Math.max(left(), right()-76); }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            List<AchievementSnapshotPacket.Entry> visible = filtered();
            scrollRow = Math.min(scrollRow, maxScroll(visible));
            int start = scrollRow;
            for (int row = 0; row < visibleRows() && start + row < visible.size(); row++) {
                AchievementSnapshotPacket.Entry e = visible.get(start + row);
                if (!e.revealed || e.achievementId.startsWith("hidden:")) continue;
                int y = contentTop() + row * ROW_H;
                if (mouseY < y + 30 || mouseY >= y + 54) continue;
                if (mouseX >= claimX() && mouseX < claimX() + 68) {
                    if (e.completed && !e.claimed)
                        NetworkHandler.INSTANCE.sendToServer(new AchievementActionPacket(
                                "claim", e.achievementId, e.stageIndex, nextNonce()));
                    return;
                }
                if (mouseX >= shareX() && mouseX < shareX() + 68) {
                    NetworkHandler.INSTANCE.sendToServer(new AchievementActionPacket(
                            "share", e.achievementId, e.stageIndex, nextNonce()));
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        UnifiedMainMenuChrome.draw(mc, width, height, mouseX, mouseY, "ДОСТИЖЕНИЯ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        String meta = "Очки: " + ClientAchievements.points()
                + "   Уровень: " + ClientAchievements.level()
                + "   Бонус: +" + ClientAchievements.bonusPercent()
                + "%   Далее: " + ClientAchievements.nextLevelThreshold();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(meta, Math.max(10,right()-left())),
                left(), contentTop()-30, GuiTheme.SILVER_COLOR);

        List<AchievementSnapshotPacket.Entry> visible = filtered();
        scrollRow = Math.min(scrollRow, maxScroll(visible));
        int start = scrollRow;
        for (int row = 0; row < visibleRows() && start + row < visible.size(); row++) {
            AchievementSnapshotPacket.Entry e = visible.get(start + row);
            int y = contentTop() + row * ROW_H;
            boolean hover = mouseX >= left() && mouseX < right()
                    && mouseY >= y && mouseY < y + ROW_H - 4;
            GuiDrawing.drawSurface(left(), y, Math.max(10,right()-left()), ROW_H-5,
                    hover ? GuiTheme.RUNE_HOVER_BG_COLOR : GuiTheme.PANEL_BG_SOFT,
                    GuiTheme.PANEL_BORDER_COLOR);
            if (!e.revealed) {
                fontRenderer.drawString("???", left()+8, y+7, GuiTheme.DISABLED_TEXT_COLOR);
                fontRenderer.drawString("Скрытое достижение", left()+8, y+22, GuiTheme.MUTED_TEXT_COLOR);
                continue;
            }
            int descriptionWidth = Math.max(24, claimX()-left()-13);
            String title = e.name + (e.stageRoman.isEmpty() ? "" : " " + e.stageRoman);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(title, descriptionWidth),
                    left()+7, y+5, GuiTheme.TEXT_COLOR);
            String history = "Выполнено " + stageHistory(e.highestCompletedStage)
                    + " / Получено " + stageHistory(e.highestClaimedStage);
            String progress = e.progress + "/" + e.target + "  [" + e.category + "]  " + history;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(progress, descriptionWidth),
                    left()+7, y+17, e.completed ? 0xFF8FE3A5 : GuiTheme.SILVER_COLOR);
            int progressBar = Math.max(12, descriptionWidth - 8);
            drawRect(left()+7, y+38, left()+7+progressBar, y+43, 0xFF29303A);
            double fraction = progressFraction(e.progress, e.target, e.completed);
            drawRect(left()+7, y+38, left()+7+(int)Math.round(progressBar*fraction),
                    y+43, 0xFF6EA8DA);
            String reward = "Кристаллы " + e.crystals + " | Подношения " + e.offerings
                    + " | Ключи ауры " + e.auraKeys + " | Очки " + e.achievementPoints;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(reward, descriptionWidth),
                    left()+7, y+49, GuiTheme.DISABLED_TEXT_COLOR);
            drawRect(claimX(), y+30, claimX()+68, y+54,
                    e.claimed ? 0xFF333333 : e.completed ? 0xFF2E7547 : 0xFF3B414A);
            drawCenteredString(fontRenderer, e.claimed ? "Получено" : e.completed ? "Забрать" : "Закрыто",
                    claimX()+34, y+38, GuiTheme.TEXT_COLOR);
            drawRect(shareX(), y+30, shareX()+68, y+54, 0xFF3B4E67);
            drawCenteredString(fontRenderer, "Поделиться", shareX()+34, y+38, GuiTheme.TEXT_COLOR);
        }
        fontRenderer.drawString("Показано: " + (visible.isEmpty() ? 0 : scrollRow+1)
                + "-" + Math.min(visible.size(), scrollRow+visibleRows()) + " из " + visible.size()
                + "   Колёсико мыши: прокрутка", left(), height-25, GuiTheme.SILVER_COLOR);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Achievement progress is serialized as decimal strings (often scientific notation). */
    static double progressFraction(String progress, String target, boolean completed) {
        try {
            BigDecimal goal = new BigDecimal(target);
            if (goal.signum() <= 0) return completed ? 1D : 0D;
            BigDecimal current = new BigDecimal(progress);
            double ratio = current.divide(goal, 6, RoundingMode.HALF_UP).doubleValue();
            return Math.max(0D, Math.min(1D, ratio));
        } catch (NumberFormatException | ArithmeticException invalid) {
            return completed ? 1D : 0D;
        }
    }

    private static synchronized long nextNonce() {
        requestNonce = Math.max(requestNonce + 1L, System.currentTimeMillis());
        return requestNonce;
    }

    private static String stageHistory(int stage) {
        return stage <= 0 ? "-" : AchievementStageLabels.roman(stage);
    }

    private List<AchievementSnapshotPacket.Entry> filtered() {
        List<AchievementSnapshotPacket.Entry> result = new ArrayList<>();
        for (AchievementSnapshotPacket.Entry e : ClientAchievements.entries())
            if ("ALL".equals(category) || category.equals(e.category)) result.add(e);
        return result;
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
