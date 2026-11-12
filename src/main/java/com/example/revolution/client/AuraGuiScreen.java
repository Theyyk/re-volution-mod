package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.network.AuraClientState;
import com.example.revolution.client.ui.AuraListLayout;
import com.example.revolution.client.ui.AuraSlotState;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.AuraActionPacket;
import com.example.revolution.network.AuraSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/** Client projection of existing server-authoritative Auras; no gameplay mutations are defined here. */
public final class AuraGuiScreen extends GuiScreen {
    private static final int ROW_HEIGHT = 24;
    private static long nonce = System.currentTimeMillis();
    private final List<AuraSnapshotPacket.Entry> visible = new ArrayList<>();
    private String rarityFilter = "ALL";
    private String selectedId = "";
    private int page;
    private String pendingAction = "";
    private String pendingRarity = "";
    private String pendingIds = "";
    private String pendingInstanceId = "";
    private int pendingOpenQuantity;

    private int panelWidth() { return Math.max(1, Math.min(600, width - 16)); }
    private int panelHeight() { return Math.max(1, Math.min(390, height - 12)); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int listX() { return left() + 10; }
    private int listY() { return top() + 136; }
    private int listWidth() { return Math.max(1, (panelWidth() - 30) / 2); }
    private int detailX() { return listX() + listWidth() + 10; }
    private int detailWidth() { return Math.max(1, left() + panelWidth() - detailX() - 10); }
    private int rowsPerPage() {
        return Math.max(1, (panelHeight() - 232) / ROW_HEIGHT);
    }

    @Override public void initGui() {
        buttonList.clear();
        int x = left() + 10, w = panelWidth() - 20, topY = top();
        int gap = 4;
        int unit = Math.max(1, (w - gap * 4) / 5);
        for (int i = 0; i < 4; i++) {
            int[] counts = {1, 3, 5, 10};
            buttonList.add(new GuiControls.PanelButton(i + 1, x + i * (unit + gap),
                    topY + 63, unit, 20, "Открыть x" + counts[i]));
        }
        buttonList.add(new GuiControls.PanelButton(5, x + 4 * (unit + gap), topY + 63,
                Math.max(1, w - 4 * (unit + gap)), 20,
                "Анимация: " + (AuraClientState.skipAnimation() ? "OFF" : "ON")));
        int actionW = Math.max(1, (w - gap * 4) / 5);
        String[] labels = {"Фильтр: " + rarityFilter, "Надеть", "Снять", "Закрепить", "Открепить"};
        int[] ids = {10, 11, 12, 13, 14};
        for (int i = 0; i < ids.length; i++)
            buttonList.add(new GuiControls.PanelButton(ids[i], x + i * (actionW + gap),
                    topY + 88, i == 4 ? Math.max(1, w - i * (actionW + gap)) : actionW, 20, labels[i]));
        int bottomY = topY + panelHeight() - 29;
        String[] footer = {"Собрать", "Разобрать", "Обновить", "Закрыть"};
        for (int i = 0; i < 4; i++) {
            int bx = x + i * (w / 4);
            buttonList.add(new GuiControls.PanelButton(20 + i, bx, bottomY,
                    Math.max(1, (i == 3 ? x + w - bx : w / 4 - 3)), 20, footer[i]));
        }
        buttonList.add(new GuiControls.PanelButton(30, x, topY + panelHeight() - 55, 27, 19, "<"));
        buttonList.add(new GuiControls.PanelButton(31, x + listWidth() - 27, topY + panelHeight() - 55, 27, 19, ">"));
        rebuildVisible();
        updateButtons();
    }

    private boolean auraAvailable() {
        return ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.AURA_SYSTEM)
                && AuraClientState.featureEnabled();
    }

    private AuraSnapshotPacket.Entry activeAura() {
        for (AuraSnapshotPacket.Entry entry : AuraClientState.entries()) if (entry.active) return entry;
        return null;
    }

    private AuraSlotState slotState() {
        return AuraSlotState.resolve(auraAvailable(), activeAura() != null);
    }

    private void updateButtons() {
        boolean available = auraAvailable();
        AuraSnapshotPacket.Entry current = selected();
        for (GuiButton button : buttonList) {
            if (button.id == 23) { button.enabled = true; continue; }
            if (button.id == 30) { button.enabled = page > 0; continue; }
            if (button.id == 31) { button.enabled = (page + 1) * rowsPerPage() < visible.size(); continue; }
            boolean enabled = available;
            if (button.id == 11 || button.id == 13 || button.id == 14 || button.id == 20 || button.id == 21)
                enabled &= current != null;
            if (current != null) {
                if (button.id == 11) enabled &= !current.active;
                if (button.id == 13) enabled &= !current.pinned;
                if (button.id == 14) enabled &= current.pinned;
                if (button.id == 20) enabled &= !"MYTHIC".equals(current.rarity);
                if (button.id == 21) enabled &= !"COMMON".equals(current.rarity) && !current.active && !current.pinned;
            }
            if (button.id == 12) enabled &= activeAura() != null;
            button.enabled = enabled;
            if (button.id == 5)
                button.displayString = "Анимация: " + (AuraClientState.skipAnimation() ? "OFF" : "ON");
        }
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id >= 1 && button.id <= 4) {
            int[] amounts = {1, 3, 5, 10};
            previewOrConfirmOpen(amounts[button.id - 1]); return;
        }
        if (button.id == 5) { clearPending(); send("skip_animation", String.valueOf(!AuraClientState.skipAnimation()), ""); return; }
        if (button.id == 10) { clearPending(); cycleFilter(); initGui(); return; }
        if (button.id == 30) { page = Math.max(0, page - 1); clearPending(); return; }
        if (button.id == 31) { page = Math.min(AuraListLayout.lastPage(visible.size(), rowsPerPage()), page + 1); clearPending(); return; }
        AuraSnapshotPacket.Entry current = selected();
        if (button.id == 11 && current != null) { clearPending(); send("equip", current.instanceId, ""); }
        else if (button.id == 12) { clearPending(); send("unequip", "", ""); }
        else if (button.id == 13 && current != null) { clearPending(); send("pin", current.instanceId, ""); }
        else if (button.id == 14 && current != null) { clearPending(); send("unpin", current.instanceId, ""); }
        else if (button.id == 20 && current != null && !"MYTHIC".equals(current.rarity)) previewOrConfirmCraft(current.rarity);
        else if (button.id == 21 && current != null && !"COMMON".equals(current.rarity)) previewOrConfirmDismantle(current);
        else if (button.id == 22) { clearPending(); send("sync", "", ""); }
        else if (button.id == 23) mc.displayGuiScreen(null);
    }

    /** Existing Aura Key purchases require confirmation; owned keys open immediately. */
    private void previewOrConfirmOpen(int quantity) {
        int missing = Math.max(0, quantity - AuraClientState.auraKeys());
        if (missing == 0) { clearPending(); send("open", String.valueOf(quantity), ""); return; }
        if ("open".equals(pendingAction) && pendingOpenQuantity == quantity) {
            clearPending(); send("open", String.valueOf(quantity), ""); return;
        }
        clearPending(); pendingAction = "open"; pendingOpenQuantity = quantity;
    }

    private void previewOrConfirmCraft(String rarity) {
        int required = AuraClientState.craftCost(rarity);
        if (required <= 0) { clearPending(); return; }
        if ("craft".equals(pendingAction) && rarity.equals(pendingRarity) && !pendingIds.isEmpty()) {
            String ids = pendingIds; clearPending(); send("craft", rarity, ids); return;
        }
        List<String> ids = new ArrayList<>();
        for (AuraSnapshotPacket.Entry entry : AuraClientState.entries()) {
            if (rarity.equals(entry.rarity) && !entry.pinned && !entry.active) ids.add(entry.instanceId);
            if (ids.size() == required) break;
        }
        if (ids.size() != required) { clearPending(); return; }
        StringBuilder joined = new StringBuilder();
        for (String id : ids) { if (joined.length() > 0) joined.append(','); joined.append(id); }
        clearPending(); pendingAction = "craft"; pendingRarity = rarity; pendingIds = joined.toString();
    }

    private void previewOrConfirmDismantle(AuraSnapshotPacket.Entry current) {
        if (current.pinned || current.active || AuraClientState.dismantleReturn(current.rarity) <= 0) {
            clearPending(); return;
        }
        if ("dismantle".equals(pendingAction) && current.instanceId.equals(pendingInstanceId)) {
            String id = pendingInstanceId; clearPending(); send("dismantle", id, ""); return;
        }
        clearPending(); pendingAction = "dismantle"; pendingRarity = current.rarity; pendingInstanceId = current.instanceId;
    }

    private void clearPending() {
        pendingAction = ""; pendingRarity = ""; pendingIds = ""; pendingInstanceId = ""; pendingOpenQuantity = 0;
    }

    private void send(String action, String value, String ids) {
        NetworkHandler.INSTANCE.sendToServer(new AuraActionPacket(action, value, ids, ++nonce));
    }

    private void cycleFilter() {
        String[] values = {"ALL", "COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
        int index = 0;
        for (int i = 0; i < values.length; i++) if (values[i].equals(rarityFilter)) index = i;
        rarityFilter = values[(index + 1) % values.length];
        page = 0;
        rebuildVisible();
    }

    private void rebuildVisible() {
        visible.clear();
        for (AuraSnapshotPacket.Entry entry : AuraClientState.entries()) {
            if ("ALL".equals(rarityFilter) || rarityFilter.equals(entry.rarity)) visible.add(entry);
        }
        visible.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
        page = Math.min(page, AuraListLayout.lastPage(visible.size(), rowsPerPage()));
        if (!selectedId.isEmpty() && selected() == null) selectedId = "";
    }

    private AuraSnapshotPacket.Entry selected() {
        for (AuraSnapshotPacket.Entry entry : visible) if (entry.instanceId.equals(selectedId)) return entry;
        return null;
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mx >= listX() && mx < listX() + listWidth()
                && my >= listY() && my < listY() + rowsPerPage() * ROW_HEIGHT) {
            page = Math.max(0, Math.min(AuraListLayout.lastPage(visible.size(), rowsPerPage()),
                    page + (wheel < 0 ? 1 : -1)));
            clearPending();
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        int x = left() + 10, y = top() + 19;
        if (mouseX >= x && mouseX < x + panelWidth() - 20 && mouseY >= y && mouseY < y + 20
                && slotState() == AuraSlotState.ACTIVE) {
            AuraSnapshotPacket.Entry active = activeAura();
            if (active != null) {
                if (!"ALL".equals(rarityFilter) && !rarityFilter.equals(active.rarity)) {
                    rarityFilter = "ALL";
                    rebuildVisible();
                }
                selectedId = active.instanceId;
                for (int i = 0; i < visible.size(); i++)
                    if (selectedId.equals(visible.get(i).instanceId)) page = i / rowsPerPage();
                clearPending();
            }
            return;
        }
        int hit = AuraListLayout.indexAt(mouseX, mouseY, listX(), listY(), listWidth(), ROW_HEIGHT,
                rowsPerPage(), page, visible.size());
        if (hit >= 0) { selectedId = visible.get(hit).instanceId; clearPending(); }
    }

    private void line(String text, int x, int y, int maxWidth, int color) {
        if (maxWidth > 4) fontRenderer.drawString(fontRenderer.trimStringToWidth(text, maxWidth), x, y, color);
    }

    private static String effectLabel(String code) {
        switch (code) {
            case "CLICK_DAMAGE_MULTIPLIER": return "Урон кликом";
            case "POISON_DAMAGE_MULTIPLIER": return "Урон ядом";
            case "BURNING_DAMAGE_MULTIPLIER": return "Урон огнём";
            case "BLEEDING_DAMAGE_MULTIPLIER": return "Урон кровотечением";
            case "CURSE_DAMAGE_MULTIPLIER": return "Урон проклятием";
            case "WITHER_DAMAGE_MULTIPLIER": return "Урон иссушением";
            case "DECAY_DAMAGE_MULTIPLIER": return "Урон разложением";
            case "ALL_DAMAGE_MULTIPLIER": return "Весь урон";
            case "VULNERABILITY_MULTIPLIER": return "Уязвимость";
            case "EFFECTIVENESS_MULTIPLIER": return "Эффективность";
            case "COIN_REWARD_MULTIPLIER": return "Монеты";
            case "RUNE_DUST_REWARD_MULTIPLIER": return "Рунная пыль";
            case "RUNE_SHARDS_REWARD_MULTIPLIER": return "Осколки рун";
            case "RUNE_CORES_REWARD_MULTIPLIER": return "Ядра рун";
            default: return code;
        }
    }

    private static String formatEffect(String raw) {
        String[] parts = raw.split("=", 2);
        if (parts.length != 2) return raw;
        try {
            return effectLabel(parts[0]) + ": +" +
                    new BigDecimal(parts[1]).movePointRight(2).stripTrailingZeros().toPlainString() + "%";
        } catch (NumberFormatException ignored) {
            return effectLabel(parts[0]) + ": " + parts[1];
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        rebuildVisible();
        updateButtons();
        int x = left(), y = top(), w = panelWidth(), h = panelHeight();
        GuiDrawing.drawSurface(x, y, w, h, 0xEE101419, GuiTheme.PANEL_BORDER_COLOR);
        drawCenteredString(fontRenderer, "АУРЫ", x + w / 2, y + 7, GuiTheme.TEXT_COLOR);
        AuraSlotState state = slotState();
        AuraSnapshotPacket.Entry active = activeAura();
        boolean overSlot = mouseX >= x + 10 && mouseX < x + w - 10 && mouseY >= y + 19 && mouseY < y + 39;
        GuiDrawing.drawSurface(x + 10, y + 19, w - 20, 20, 0xB01F2732,
                overSlot && state == AuraSlotState.ACTIVE ? GuiTheme.SLOT_HOVER_COLOR : GuiTheme.PANEL_BORDER_COLOR);
        String slotText = state == AuraSlotState.LOCKED ? "Слот ауры: закрыт"
                : state == AuraSlotState.EMPTY ? "Слот ауры: пусто"
                : "Активная аура: " + active.rarity + " (нажми для выбора)";
        line(slotText, x + 16, y + 25, w - 32, GuiTheme.TEXT_COLOR);
        line("Ключи: " + AuraClientState.auraKeys() + "     Кристаллы: " + AuraClientState.crystals()
                + "     Цена ключа: " + AuraClientState.keyCrystalPrice(), x + 10, y + 45,
                w - 20, GuiTheme.SILVER_COLOR);
        line("Сундуки: " + AuraClientState.totalChestOpens() + "     Получено аур: " +
                AuraClientState.totalChestAuraDrops(), x + 10, y + 115, w - 20, GuiTheme.SILVER_COLOR);

        int lx = listX(), ly = listY(), lw = listWidth();
        int dx = detailX(), dw = detailWidth(), listHeight = rowsPerPage() * ROW_HEIGHT;
        GuiDrawing.drawSurface(lx - 2, ly - 3, lw + 4, listHeight + 6,
                0x900C1016, GuiTheme.PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(dx - 2, ly - 3, dw + 4, listHeight + 6,
                0x900C1016, GuiTheme.PANEL_BORDER_COLOR);
        for (int row = 0; row < rowsPerPage(); row++) {
            int index = page * rowsPerPage() + row;
            if (index >= visible.size()) break;
            AuraSnapshotPacket.Entry e = visible.get(index);
            int ry = ly + row * ROW_HEIGHT;
            boolean picked = e.instanceId.equals(selectedId);
            boolean hovered = mouseX >= lx && mouseX < lx + lw && mouseY >= ry && mouseY < ry + ROW_HEIGHT;
            GuiDrawing.drawSurface(lx, ry, lw, ROW_HEIGHT - 1,
                    picked ? GuiTheme.SELECTED_BG_COLOR : hovered ? GuiTheme.RUNE_HOVER_BG_COLOR : 0x50192029,
                    picked ? GuiTheme.SELECTED_COLOR : GuiTheme.PANEL_BORDER_COLOR);
            line(e.rarity + (e.active ? "  [АКТИВНА]" : "") + (e.pinned ? "  [PIN]" : ""),
                    lx + 5, ry + 4, lw - 10, picked ? GuiTheme.TEXT_COLOR : GuiTheme.SILVER_COLOR);
            String summary = e.aggregatedEffects == null ? "" : e.aggregatedEffects;
            String[] effects = summary.split("\\|");
            line(effects.length > 0 ? formatEffect(effects[0]) : "Без эффектов",
                    lx + 5, ry + 14, lw - 10, GuiTheme.MUTED_TEXT_COLOR);
        }
        AuraSnapshotPacket.Entry chosen = selected();
        if (chosen == null) {
            line("Выберите ауру в списке", dx + 4, ly + 8, dw - 8, GuiTheme.SILVER_COLOR);
            line("для просмотра эффектов", dx + 4, ly + 22, dw - 8, GuiTheme.MUTED_TEXT_COLOR);
        } else {
            line(chosen.rarity + (chosen.active ? "  |  Активна" : "") +
                    (chosen.pinned ? "  |  Закреплена" : ""), dx + 4, ly + 6,
                    dw - 8, GuiTheme.TEXT_COLOR);
            line("Эффекты:", dx + 4, ly + 22, dw - 8, GuiTheme.SILVER_COLOR);
            String[] effects = chosen.aggregatedEffects == null ? new String[0]
                    : chosen.aggregatedEffects.split("\\|");
            int availableLines = Math.max(0, (listHeight - 48) / 12);
            for (int i = 0; i < effects.length && i < availableLines; i++)
                line(formatEffect(effects[i]), dx + 4, ly + 36 + i * 12,
                        dw - 8, GuiTheme.TEXT_COLOR);
        }

        String preview = "";
        if ("open".equals(pendingAction)) {
            int missing = Math.max(0, pendingOpenQuantity - AuraClientState.auraKeys());
            preview = "Подтвердите: " + missing + " ключей за " +
                    (missing * (long) AuraClientState.keyCrystalPrice()) + " кристаллов";
        } else if ("craft".equals(pendingAction)) {
            preview = "Подтвердите сборку: " + AuraClientState.craftCost(pendingRarity) +
                    " x " + pendingRarity;
        } else if ("dismantle".equals(pendingAction)) {
            preview = "Подтвердите разбор: " + pendingRarity + " -> " +
                    AuraClientState.dismantleReturn(pendingRarity);
        }
        if (!preview.isEmpty()) line(preview, dx + 4, y + h - 51, dw - 8, 0xFFFFDD77);
        line(visible.isEmpty() ? "0 / 0" : (page + 1) + " / " +
                (AuraListLayout.lastPage(visible.size(), rowsPerPage()) + 1),
                lx + 35, y + h - 49, lw - 73, GuiTheme.SILVER_COLOR);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
