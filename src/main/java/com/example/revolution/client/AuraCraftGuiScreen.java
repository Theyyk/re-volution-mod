package com.example.revolution.client;

import com.example.revolution.client.network.AuraClientState;
import com.example.revolution.client.ui.AuraCraftSelection;
import com.example.revolution.network.AuraActionPacket;
import com.example.revolution.network.AuraSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

/** UI-only three-stage explicit material selector; server remains authoritative. */
public final class AuraCraftGuiScreen extends GuiScreen {
    private static final String[] RARITIES = {"COMMON", "RARE", "EPIC", "LEGENDARY"};
    private static final int ROW = 29;
    private static long nonce = System.currentTimeMillis();
    private final AuraGuiScreen parent;
    private final List<String> selectedIds = new ArrayList<>();
    private String rarity = "";
    private int step = 1; // rarity -> materials -> final confirmation
    private int page;

    public AuraCraftGuiScreen(AuraGuiScreen parent) { this.parent = parent; }

    private int w() { return Math.min(510, Math.max(160, width - 24)); }
    private int h() { return Math.min(340, Math.max(170, height - 20)); }
    private int x() { return (width - w()) / 2; }
    private int y() { return (height - h()) / 2; }
    private int rows() { return Math.max(1, (h() - 145) / ROW); }
    private int required() { return AuraClientState.craftCost(rarity); }
    private List<AuraSnapshotPacket.Entry> eligible() {
        List<AuraSnapshotPacket.Entry> list = new ArrayList<>();
        for (AuraSnapshotPacket.Entry e : AuraClientState.entries())
            if (AuraCraftSelection.eligible(e, rarity)) list.add(e);
        return list;
    }
    private int lastPage(int count) { return Math.max(0, (Math.max(0, count) - 1) / rows()); }

    @Override public void initGui() { buttonList.clear(); }
    @Override public boolean doesGuiPauseGame() { return false; }
    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private void back() {
        if (step == 1) mc.displayGuiScreen(parent);
        else if (step == 3) step = 2;
        else { step = 1; rarity = ""; selectedIds.clear(); page = 0; }
    }
    @Override protected void keyTyped(char character, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) { back(); return; }
        super.keyTyped(character, keyCode);
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (step != 2) return;
        int delta = Mouse.getEventDWheel();
        if (delta != 0) page = Math.max(0, Math.min(lastPage(eligible().size()),
                page + (delta < 0 ? 1 : -1)));
    }
    @Override protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) return;
        int x = x(), y = y(), w = w(), h = h();
        if (hit(mx, my, x + 12, y + h - 35, 112, 23)) { back(); return; }
        if (step == 1) {
            for (int i = 0; i < RARITIES.length; i++) {
                if (hit(mx, my, x + 14, y + 55 + i * 35, w - 28, 30)
                        && AuraClientState.craftCost(RARITIES[i]) > 0) {
                    rarity = RARITIES[i]; selectedIds.clear(); page = 0; step = 2; return;
                }
            }
        } else if (step == 2) {
            List<AuraSnapshotPacket.Entry> list = eligible();
            if (hit(mx, my, x + 14, y + h - 67, 30, 23)) { page = Math.max(0, page - 1); return; }
            if (hit(mx, my, x + 49, y + h - 67, 30, 23)) { page = Math.min(lastPage(list.size()), page + 1); return; }
            if (hit(mx, my, x + w - 164, y + h - 35, 150, 23)) {
                if (AuraCraftSelection.valid(AuraClientState.entries(), rarity, selectedIds, required())) step = 3;
                return;
            }
            for (int i = 0; i < rows(); i++) {
                int index = page * rows() + i;
                if (index >= list.size()) break;
                if (!hit(mx, my, x + 14, y + 55 + i * ROW, w - 28, ROW - 2)) continue;
                String id = list.get(index).instanceId;
                if (selectedIds.contains(id)) selectedIds.remove(id);
                else if (selectedIds.size() < required()) selectedIds.add(id);
                return;
            }
        } else if (step == 3 && hit(mx, my, x + w - 164, y + h - 35, 150, 23)) {
            if (!AuraCraftSelection.valid(AuraClientState.entries(), rarity, selectedIds, required())) {
                step = 2; return;
            }
            StringBuilder ids = new StringBuilder();
            for (String id : selectedIds) {
                if (ids.length() != 0) ids.append(',');
                ids.append(id);
            }
            NetworkHandler.INSTANCE.sendToServer(new AuraActionPacket("craft", rarity, ids.toString(), ++nonce));
            mc.displayGuiScreen(parent);
        }
    }
    private void text(String label, int x, int y, int max, int color) {
        if (max > 6) fontRenderer.drawString(fontRenderer.trimStringToWidth(label, max), x, y, color);
    }
    private void control(String label, int x, int y, int w, int h, boolean enabled, int mx, int my) {
        GuiDrawing.drawSurface(x, y, w, h, !enabled ? GuiTheme.DISABLED_BG_COLOR
                        : hit(mx, my, x, y, w, h) ? GuiTheme.RUNE_HOVER_BG_COLOR : GuiTheme.BUTTON_BG_COLOR,
                GuiTheme.PANEL_BORDER_COLOR);
        drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(label, Math.max(1, w - 8)),
                x + w / 2, y + (h - fontRenderer.FONT_HEIGHT) / 2,
                enabled ? GuiTheme.TEXT_COLOR : GuiTheme.DISABLED_TEXT_COLOR);
    }
    private static String effect(String raw) {
        String[] parts = raw.split("=", 2);
        if (parts.length != 2) return raw;
        try {
            return parts[0].replace("_MULTIPLIER", "").replace('_', ' ') + "  +" +
                    new BigDecimal(parts[1]).movePointRight(2).stripTrailingZeros().toPlainString() + "%";
        } catch (NumberFormatException ignored) { return raw; }
    }
    @Override public void drawScreen(int mx, int my, float partialTicks) {
        parent.drawScreen(-1000, -1000, partialTicks);
        GuiDrawing.drawSurface(0, 0, width, height, 0xB8000000, 0x00000000);
        int x = x(), y = y(), w = w(), h = h();
        GuiDrawing.drawSurface(x, y, w, h, 0xFA11161D, GuiTheme.SELECTED_COLOR);
        drawCenteredString(fontRenderer, "СБОРКА АУР", x + w / 2, y + 12, GuiTheme.TEXT_COLOR);
        text(step == 1 ? "Шаг 1/3 — выберите редкость" :
                step == 2 ? "Шаг 2/3 — выберите материалы" : "Шаг 3/3 — подтвердите расход",
                x + 14, y + 35, w - 28, GuiTheme.SILVER_COLOR);
        control(step == 1 ? "Закрыть" : "Назад", x + 12, y + h - 35, 112, 23, true, mx, my);
        if (step == 1) {
            for (int i = 0; i < RARITIES.length; i++) {
                String r = RARITIES[i]; int count = 0;
                for (AuraSnapshotPacket.Entry e : AuraClientState.entries())
                    if (AuraCraftSelection.eligible(e, r)) count++;
                int cost = AuraClientState.craftCost(r);
                control(r + "   Доступно " + count + "   Требуется " + cost,
                        x + 14, y + 55 + i * 35, w - 28, 30, cost > 0, mx, my);
            }
        } else if (step == 2) {
            List<AuraSnapshotPacket.Entry> list = eligible();
            for (int i = 0; i < rows(); i++) {
                int idx = page * rows() + i; if (idx >= list.size()) break;
                AuraSnapshotPacket.Entry e = list.get(idx);
                int cy = y + 55 + i * ROW;
                boolean checked = selectedIds.contains(e.instanceId);
                GuiDrawing.drawSurface(x + 14, cy, w - 28, ROW - 2,
                        checked ? GuiTheme.SELECTED_BG_COLOR : GuiTheme.SLOT_BG_COLOR,
                        checked ? GuiTheme.SELECTED_COLOR : GuiTheme.PANEL_BORDER_COLOR);
                text((checked ? "[X] " : "[ ] ") + e.rarity + "  #" + (idx + 1),
                        x + 20, cy + 3, w - 40, GuiTheme.TEXT_COLOR);
                String[] effects = e.aggregatedEffects == null ? new String[0] : e.aggregatedEffects.split("\\|");
                text(effects.length == 0 ? "Нет эффектов" : effect(effects[0]),
                        x + 20, cy + 15, w - 40, GuiTheme.MUTED_TEXT_COLOR);
            }
            text("Выбрано: " + selectedIds.size() + " / " + required(), x + 14, y + h - 90,
                    w - 28, GuiTheme.TEXT_COLOR);
            control("<", x + 14, y + h - 67, 30, 23, page > 0, mx, my);
            control(">", x + 49, y + h - 67, 30, 23, (page + 1) * rows() < list.size(), mx, my);
            text((list.isEmpty() ? 0 : page + 1) + " / " + (list.isEmpty() ? 0 : lastPage(list.size()) + 1),
                    x + 88, y + h - 60, 90, GuiTheme.SILVER_COLOR);
            control("К подтверждению", x + w - 164, y + h - 35, 150, 23,
                    AuraCraftSelection.valid(AuraClientState.entries(), rarity, selectedIds, required()), mx, my);
        } else {
            text("Израсходовать " + required() + " ауры редкости " + rarity + "?",
                    x + 14, y + 60, w - 28, GuiTheme.TEXT_COLOR);
            List<AuraSnapshotPacket.Entry> entries = AuraClientState.entries();
            for (int i = 0; i < selectedIds.size() && i < 9; i++) {
                for (AuraSnapshotPacket.Entry e : entries) if (selectedIds.get(i).equals(e.instanceId)) {
                    String[] effects = e.aggregatedEffects == null ? new String[0] : e.aggregatedEffects.split("\\|");
                    text((i + 1) + ". " + (effects.length == 0 ? e.rarity : effect(effects[0])),
                            x + 14, y + 88 + i * 17, w - 28, GuiTheme.TEXT_COLOR);
                    break;
                }
            }
            text("Сервер проверит выбранные экземпляры. Действие необратимо.",
                    x + 14, y + h - 60, w - 28, 0xFFFFDD77);
            control("Подтвердить сборку", x + w - 164, y + h - 35, 150, 23,
                    AuraCraftSelection.valid(entries, rarity, selectedIds, required()), mx, my);
        }
    }
}
