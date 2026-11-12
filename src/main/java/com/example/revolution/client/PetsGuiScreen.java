package com.example.revolution.client;

import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PetActionPacket;
import com.example.revolution.network.PetSnapshotPacket;
import com.example.revolution.prestige.definition.PrestigeRunResources;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional Pet screen with shared E-menu chrome; all actions remain server-authoritative. */
public final class PetsGuiScreen extends GuiScreen {
    private static final int BUY_NORMAL_SLOT = 500, BUY_PREMIUM_SLOT = 501, COLLECT = 502;
    private static final int PREV_PAGE = 503, NEXT_PAGE = 504, AWAKEN_BUTTON = 505, CLOSE_DIALOG = 506, LEVEL_BUTTON = 507;
    private static final int TILE_H = 57, GAP = 7;
    private int page;
    private int focusedSlot = -1;
    private String focusedPetId = "";
    private boolean dialogOpen;
    private boolean confirmAwaken;
    private long pendingRevision = -1L;

    @Override public void initGui() {
        buttonList.clear();
        UnifiedMainMenuChrome.installNavigation(buttonList, width, height, UiMenuSection.PETS);
        int left = UnifiedMainMenuChrome.contentLeft(width, height);
        int right = UnifiedMainMenuChrome.contentRight(width, height);
        int total = Math.max(180, right - left);
        int btn = Math.max(40, (total - 14) / 3);
        int y = Math.max(28, UnifiedMainMenuChrome.contentTop(width, height) - 23);
        buttonList.add(new GuiControls.PanelButton(
                BUY_NORMAL_SLOT, left, y, btn, 20,
                "Слот: " + ClientPets.nextNormalSlotCost()));
        GuiButton premium = new GuiControls.PanelButton(
                BUY_PREMIUM_SLOT, left + btn + 7, y, btn, 20, "Премиум");
        premium.enabled = ClientPets.premiumSlots() < 2;
        buttonList.add(premium);
        buttonList.add(new GuiControls.PanelButton(
                COLLECT, left + (btn + 7) * 2, y, btn, 20, "Собрать"));
        buttonList.add(new GuiControls.PanelButton(
                PREV_PAGE, Math.max(left, right - 126), height - 30, 56, 20, "<"));
        buttonList.add(new GuiControls.PanelButton(
                NEXT_PAGE, Math.max(left, right - 63), height - 30, 56, 20, ">"));
        int dialogX = dialogX(), dialogY = dialogY();
        int buttonWidth = Math.max(1, (dialogWidth() - 24) / 3);
        GuiButton level = new GuiControls.PanelButton(LEVEL_BUTTON, dialogX + 8, dialogY + 128,
                buttonWidth, 20, "Повысить ур.");
        level.visible = false;
        buttonList.add(level);
        GuiButton awaken = new GuiControls.PanelButton(AWAKEN_BUTTON, dialogX + 12 + buttonWidth, dialogY + 128,
                buttonWidth, 20, "Пробудить");
        awaken.visible = false;
        buttonList.add(awaken);
        GuiButton close = new GuiControls.PanelButton(CLOSE_DIALOG, dialogX + 16 + 2 * buttonWidth,
                dialogY + 128, buttonWidth, 20, "Закрыть");
        close.visible = false;
        buttonList.add(close);
        NetworkHandler.INSTANCE.sendToServer(new PetActionPacket("sync"));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.PETS, mc)) return;
        if (button.id == BUY_NORMAL_SLOT) send("purchase_normal_slot", "");
        if (button.id == BUY_PREMIUM_SLOT) send("purchase_premium_slot", "");
        if (button.id == COLLECT) send("collect_pet_fragments", "");
        if (button.id == PREV_PAGE) page = Math.max(0, page - 1);
        if (button.id == NEXT_PAGE) page = Math.min(lastPage(), page + 1);
        if (button.id == CLOSE_DIALOG) { dialogOpen = false; confirmAwaken = false; }
        if (button.id == LEVEL_BUTTON && dialogOpen) {
            PetSnapshotPacket.Entry chosen = PetContextSlotLayout.assigned(ClientPets.entries(), focusedSlot);
            if (chosen != null && chosen.levelUpReady) {
                confirmAwaken = false;
                send("level_up_pet", chosen.petId);
            }
        }
        if (button.id == AWAKEN_BUTTON && dialogOpen) {
            PetSnapshotPacket.Entry chosen = PetContextSlotLayout.assigned(ClientPets.entries(), focusedSlot);
            if (chosen != null && canAwaken(chosen)) {
                if (!confirmAwaken) confirmAwaken = true;
                else { send("awaken_pet", chosen.petId); dialogOpen = false; confirmAwaken = false; }
            }
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (dialogOpen) {
            if (mouseButton == 0) for (GuiButton b : buttonList) {
                if ((b.id == LEVEL_BUTTON || b.id == AWAKEN_BUTTON || b.id == CLOSE_DIALOG)
                        && b.visible && b.enabled && b.mousePressed(mc, mouseX, mouseY)) {
                    actionPerformed(b);
                    return;
                }
            }
            // Inactive buttons and empty space inside the dialog are inert.
            // Only an outside click or the explicit Close button dismisses it.
            if (mouseX < dialogX() || mouseX >= dialogX() + dialogWidth()
                    || mouseY < dialogY() || mouseY >= dialogY() + DIALOG_HEIGHT) {
                dialogOpen = false;
                confirmAwaken = false;
            }
            return;
        }
        PetContextSlotLayout context = contextSlots();
        int displayIndex = context.hit(mouseX, mouseY);
        if (displayIndex >= 0) {
            int physical = PetContextSlotLayout.physicalAt(displayIndex, ClientPets.normalSlots(), ClientPets.premiumSlots());
            if (mouseButton == 0 && PetContextSlotLayout.unlocked(physical, ClientPets.normalSlots(), ClientPets.premiumSlots())) {
                focusedSlot = physical;
                PetSnapshotPacket.Entry chosen = PetContextSlotLayout.assigned(ClientPets.entries(), physical);
                focusedPetId = chosen == null ? "" : chosen.petId;
                dialogOpen = chosen != null;
                confirmAwaken = false;
                // Occupied cell opens details; level-up is deliberate and available inside the dialog.
            }
            return;
        }
        int index = tileAt(mouseX, mouseY);
        if (index >= 0) {
            List<PetSnapshotPacket.Entry> entries = availableEntries();
            if (index < entries.size()) {
                PetSnapshotPacket.Entry e = entries.get(index);
                if (mouseButton == 1 && isCtrlKeyDown()) send("revert_pet", e.petId);
                else if (mouseButton == 0) {
                    if (!e.owned) send("purchase_pet", e.petId);
                    else if (focusedSlot >= 0) sendAtSlot("assign_pet_slot", e.petId, focusedSlot);
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void send(String action, String petId) {
        sendAtSlot(action, petId, -1);
    }

    private void sendAtSlot(String action, String petId, int slotIndex) {
        if (ClientPets.runId().isEmpty() && !"purchase_premium_slot".equals(action)) return;
        long currentRevision = ClientPets.revision();
        if (pendingRevision >= 0L && currentRevision <= pendingRevision) return;
        pendingRevision = currentRevision;
        NetworkHandler.INSTANCE.sendToServer(new PetActionPacket(action, petId, ClientPets.runId(), slotIndex));
    }

    private static final int DIALOG_HEIGHT = 156;
    private int dialogWidth() { return Math.max(1, Math.min(320, contentWidth() - 12)); }
    private int dialogX() {
        return UnifiedMainMenuChrome.contentLeft(width, height) + Math.max(0, (contentWidth() - dialogWidth()) / 2);
    }
    private int dialogY() {
        int panelTop = contextTop() + contextSlots().height() + 8;
        // Stay within the Pets content region and clear of its bottom paging controls.
        return Math.max(panelTop, Math.min(tileTop() + 24, height - DIALOG_HEIGHT - 40));
    }

    private static int rankIndex(String rank) {
        if ("IMPROVED".equals(rank)) return 1;
        if ("ADVANCED".equals(rank)) return 2;
        if ("MYTHIC".equals(rank)) return 3;
        if ("ANCIENT".equals(rank)) return 4;
        return 0;
    }

    private boolean canAwaken(PetSnapshotPacket.Entry target) {
        if (ClientPets.runId().isEmpty() || target == null || !target.selected || rankIndex(target.rank) >= 3)
            return false;
        if (ClientPets.normalSlots() != 8) return false;
        int count = 0;
        for (PetSnapshotPacket.Entry e : ClientPets.entries()) if (e.selected) {
            count++;
            if (rankIndex(e.rank) < rankIndex(target.rank)) return false;
        }
        return count == 8 + ClientPets.premiumSlots();
    }

    private int contentWidth() {
        return Math.max(1, UnifiedMainMenuChrome.contentRight(width, height)
                - UnifiedMainMenuChrome.contentLeft(width, height));
    }

    private int columns() {
        return Math.max(1, Math.min(4, (contentWidth() + GAP) / (180 + GAP)));
    }

    private int tileWidth() {
        return Math.max(1, (contentWidth() - (columns() - 1) * GAP) / columns());
    }

    private int contextTop() {
        return Math.max(69, UnifiedMainMenuChrome.contentTop(width, height) + 14);
    }

    private PetContextSlotLayout contextSlots() {
        return new PetContextSlotLayout(
                UnifiedMainMenuChrome.contentLeft(width, height), contentWidth(), contextTop());
    }

    private int tileTop() {
        return contextTop() + contextSlots().height() + 23;
    }

    private int rows() {
        return Math.max(1, (height - tileTop() - 42 + GAP) / (TILE_H + GAP));
    }

    private int perPage() {
        return Math.max(1, columns() * rows());
    }

    private List<PetSnapshotPacket.Entry> availableEntries() {
        return PetContextSlotLayout.available(ClientPets.entries());
    }

    private int lastPage() {
        int count = availableEntries().size();
        return Math.max(0, (count - 1) / perPage());
    }

    private int tileX(int indexOnPage) {
        return UnifiedMainMenuChrome.contentLeft(width, height)
                + (indexOnPage % columns()) * (tileWidth() + GAP);
    }

    private int tileY(int indexOnPage) {
        return tileTop() + (indexOnPage / columns()) * (TILE_H + GAP);
    }

    private int tileAt(int mouseX, int mouseY) {
        List<PetSnapshotPacket.Entry> entries = availableEntries();
        int start = Math.min(page, lastPage()) * perPage();
        for (int slot = 0; slot < perPage() && start + slot < entries.size(); slot++) {
            int x = tileX(slot), y = tileY(slot);
            if (mouseX >= x && mouseX < x + tileWidth()
                    && mouseY >= y && mouseY < y + TILE_H) return start + slot;
        }
        return -1;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        UnifiedMainMenuChrome.draw(mc, width, height, mouseX, mouseY, "ПИТОМЦЫ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        int left = UnifiedMainMenuChrome.contentLeft(width, height);
        int right = UnifiedMainMenuChrome.contentRight(width, height);
        String info = "Слоты: " + ClientPets.normalSlots() + "/8 + " + ClientPets.premiumSlots()
                + "/2  |  Фрагменты: " + ClientPets.fragments()
                + "  |  Готово к сбору: " + ClientPets.petPendingFragments();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(info, Math.max(10, right-left)),
                left, contextTop() - 13, GuiTheme.MUTED_TEXT_COLOR);
        if (ClientPets.runId().isEmpty()) {
            String hint = "Нет активного Mastery-run: выбор и покупка питомцев недоступны";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(hint, Math.max(10, right - left)),
                    left, contextTop() + contextSlots().height() + 3, 0xFFFFAA55);
        }

        List<PetSnapshotPacket.Entry> entries = ClientPets.entries();
        PetContextSlotLayout context = contextSlots();
        if (focusedSlot >= 0) {
            PetSnapshotPacket.Entry actual = PetContextSlotLayout.assigned(entries, focusedSlot);
            String actualId = actual == null ? "" : actual.petId;
            if (focusedPetId.isEmpty() && !actualId.isEmpty()) focusedPetId = actualId;
            else if (!actualId.equals(focusedPetId)) {
                focusedSlot = -1;
                focusedPetId = "";
                dialogOpen = false; confirmAwaken = false;
            }
        }
        for (int i = 0; i < PetContextSlotLayout.COUNT; i++) {
            int physical = PetContextSlotLayout.physicalAt(i, ClientPets.normalSlots(), ClientPets.premiumSlots());
            boolean unlocked = PetContextSlotLayout.unlocked(physical, ClientPets.normalSlots(), ClientPets.premiumSlots());
            PetSnapshotPacket.Entry assigned = PetContextSlotLayout.assigned(entries, physical);
            boolean hovered = context.hit(mouseX, mouseY) == i;
            boolean focused = focusedSlot == physical;
            boolean premium = physical >= PetContextSlotLayout.NORMAL_COUNT;
            int bg = premium && unlocked ? (focused ? 0xCC67498F : 0xCC4D2968)
                    : focused ? 0xAA45648A : assigned != null ? 0xAA254A35
                    : unlocked ? GuiTheme.PANEL_BG_SOFT : 0xAA292929;
            int border = premium && unlocked ? (focused ? 0xFFFFD988 : 0xFFB589DF)
                    : focused ? 0xFF99C9FF : hovered ? GuiTheme.SLOT_HOVER_COLOR
                    : GuiTheme.PANEL_BORDER_COLOR;
            GuiDrawing.drawSurface(context.x(i), context.y(i), context.width(), context.cellHeight(), bg, border);
            String title = assigned == null ? (unlocked ? "+" : "X") : assigned.petId;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(title, Math.max(1, context.width() - 6)),
                    context.x(i) + 3, context.y(i) + 3,
                    unlocked ? GuiTheme.TEXT_COLOR : GuiTheme.DISABLED_TEXT_COLOR);
            String sub = assigned == null ? (unlocked ? (premium ? "P ПУСТО" : "ПУСТО") : "ЗАКР.")
                    : premium ? (assigned.levelUpReady ? "P XP +" : "АКТ. [P]")
                    : assigned.levelUpReady ? "XP +" : "АКТИВЕН";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(sub, Math.max(1, context.width() - 6)),
                    context.x(i) + 3, context.y(i) + Math.min(context.cellHeight() - 9, 16),
                    assigned != null ? 0xA8E6B8 : GuiTheme.DISABLED_TEXT_COLOR);
            if (assigned != null && canAwaken(assigned))
                fontRenderer.drawString("!", context.x(i) + Math.max(1, context.width() - 9),
                        context.y(i) + 2, 0xFFFFCC66);
        }
        page = Math.min(page, lastPage());
        List<PetSnapshotPacket.Entry> available = availableEntries();
        int start = page * perPage();
        long now = System.currentTimeMillis();
        for (int slot = 0; slot < perPage() && start + slot < available.size(); slot++) {
            PetSnapshotPacket.Entry e = available.get(start + slot);
            int x = tileX(slot), y = tileY(slot), w = tileWidth();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TILE_H;
            int bg = e.selected && e.premiumSlot ? 0xAA4D2968
                    : e.selected ? 0xAA254A35 : GuiTheme.PANEL_BG_SOFT;
            GuiDrawing.drawSurface(x, y, w, TILE_H,
                    bg, hover ? GuiTheme.SLOT_HOVER_COLOR : GuiTheme.PANEL_BORDER_COLOR);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(e.petId + "  " + e.rank, w - 10),
                    x + 5, y + 5, GuiTheme.TEXT_COLOR);
            fontRenderer.drawString("Ур. " + e.level + "  Опыт " + e.xp + "/" + e.nextLevelThreshold,
                    x + 5, y + 17, GuiTheme.SILVER_COLOR);
            String state;
            if (e.cooldownUntil > now) state = "Перезарядка " + formatCooldown(e.cooldownUntil - now);
            else if (e.levelUpReady) state = "ДОСТУПНО УЛУЧШЕНИЕ";
            else if (e.selected) state = "АКТИВЕН" + (e.premiumSlot ? " [P]" : "");
            else if (e.owned) state = "Доступен";
            else state = "Цена " + e.purchaseCost + " монет";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(state, w-10),
                    x + 5, y + 29, e.cooldownUntil > now ? 0xFFAA66 : 0xA8E6B8);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(e.effects, w - 10),
                    x + 5, y + 41, GuiTheme.DISABLED_TEXT_COLOR);
        }
        String pages = "Стр. " + (page + 1) + "/" + (lastPage() + 1);
        fontRenderer.drawString(pages, left, height - 25, GuiTheme.SILVER_COLOR);
        if (!available.isEmpty())
            fontRenderer.drawString(fontRenderer.trimStringToWidth(
                    "Верхний слот: открыть карточку. В карточке: уровень / пробуждение. Ctrl+ПКМ: снизить ранг", Math.max(8, right-left)),
                    left, height - 41, GuiTheme.DISABLED_TEXT_COLOR);
        for (GuiButton b : buttonList) {
            if (b.id == PREV_PAGE) b.enabled = page > 0;
            if (b.id == NEXT_PAGE) b.enabled = page < lastPage();
            if (b.id == BUY_NORMAL_SLOT || b.id == COLLECT)
                b.enabled = !ClientPets.runId().isEmpty();
            if (b.id == BUY_PREMIUM_SLOT) b.enabled = ClientPets.premiumSlots() < 2;
            if (b.id == COLLECT) b.displayString = "Собрать: " + ClientPets.petPendingFragments();
            if (b.id == BUY_NORMAL_SLOT) b.displayString = PrestigeRunResources.FRAGMENT_OF_KNOWLEDGE_PLURAL
                    + ": " + ClientPets.nextNormalSlotCost();
        }
        for (GuiButton b : buttonList) {
            if (b.id == LEVEL_BUTTON || b.id == AWAKEN_BUTTON || b.id == CLOSE_DIALOG) {
                b.visible = dialogOpen;
                PetSnapshotPacket.Entry chosen = PetContextSlotLayout.assigned(entries, focusedSlot);
                if (b.id == LEVEL_BUTTON) b.enabled = chosen != null && chosen.levelUpReady && !ClientPets.runId().isEmpty();
                if (b.id == AWAKEN_BUTTON) {
                    b.enabled = chosen != null && canAwaken(chosen);
                    b.displayString = confirmAwaken ? "Подтвердить" : "Пробудить";
                }
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        // Keep all background buttons visible, but the modal input handler blocks them.
        // Render the dialog above the background buttons and its own buttons last.
        if (dialogOpen) {
            drawDialog(entries);
            for (GuiButton b : buttonList)
                if (b.id == LEVEL_BUTTON || b.id == AWAKEN_BUTTON || b.id == CLOSE_DIALOG)
                    b.drawButton(mc, mouseX, mouseY, partialTicks);
        }
        int hoveredSlot = context.hit(mouseX, mouseY);
        if (dialogOpen) return;
        if (hoveredSlot >= 0) {
            int physicalHover = PetContextSlotLayout.physicalAt(hoveredSlot, ClientPets.normalSlots(), ClientPets.premiumSlots());
            PetSnapshotPacket.Entry hoverPet = PetContextSlotLayout.assigned(entries, physicalHover);
            String description = hoverPet == null
                    ? (PetContextSlotLayout.unlocked(physicalHover, ClientPets.normalSlots(), ClientPets.premiumSlots())
                    ? "Пустой слот" : "Слот пока не разблокирован")
                    : hoverPet.petId + " | " + hoverPet.rank + " | Ур. " + hoverPet.level;
            drawHoveringText(java.util.Collections.singletonList(description), mouseX, mouseY);
        }
    }

    private void drawDialog(List<PetSnapshotPacket.Entry> entries) {
        if (dialogOpen) {
            PetSnapshotPacket.Entry selected = PetContextSlotLayout.assigned(entries, focusedSlot);
            if (selected == null) { dialogOpen = false; confirmAwaken = false; }
            else {
                int x = dialogX(), y = dialogY(), w = dialogWidth();
                GuiDrawing.drawSurface(x, y, w, DIALOG_HEIGHT, 0xF0212635, 0xFF99C9FF);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        "Питомец: " + selected.petId + " [" + selected.rank + "]"
                                + (selected.premiumSlot ? " [PREMIUM]" : ""), w - 16),
                        x + 8, y + 9, GuiTheme.TEXT_COLOR);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        "Уровень: " + selected.level + "  |  Опыт: " + selected.xp + "/" + selected.nextLevelThreshold,
                        w - 16), x + 8, y + 26, GuiTheme.SILVER_COLOR);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        selected.levelUpReady ? "Повышение уровня: ДОСТУПНО" : "Повышение уровня: недостаточно опыта",
                        w - 16), x + 8, y + 43, selected.levelUpReady ? 0xA8E6B8 : GuiTheme.DISABLED_TEXT_COLOR);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        canAwaken(selected) ? "Пробуждение: ДОСТУПНО" : "Пробуждение: условия не выполнены",
                        w - 16), x + 8, y + 60, canAwaken(selected) ? 0xA8E6B8 : 0xFFFFAA55);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        "Нужно 8 обычных и все разблокированные premium", w - 16),
                        x + 8, y + 77, GuiTheme.DISABLED_TEXT_COLOR);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        "Ранги остальных не ниже текущего ранга питомца", w - 16),
                        x + 8, y + 91, GuiTheme.DISABLED_TEXT_COLOR);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(
                        confirmAwaken ? "ПОДТВЕРДИТЕ: другие питомцы теряют XP и уровни!" :
                        "Пробуждение сбрасывает уровень/XP остальных и даёт cooldown",
                        w - 16), x + 8, y + 108, confirmAwaken ? 0xFFFFAA55 : GuiTheme.DISABLED_TEXT_COLOR);
            }
        }
    }

    private static String formatCooldown(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return (seconds / 3600L) + "ч " + ((seconds % 3600L) / 60L) + "м";
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
