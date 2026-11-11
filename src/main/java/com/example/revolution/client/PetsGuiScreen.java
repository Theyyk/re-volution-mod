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
    private static final int PREV_PAGE = 503, NEXT_PAGE = 504;
    private static final int TILE_H = 57, GAP = 7;
    private int page;
    private int focusedSlot = -1;
    private String focusedPetId = "";
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
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Hit regions share exactly the same geometry as the rendered tiles.
        // Clicking a context cell is UI-only; never perform Gameplay on focus.
        PetContextSlotLayout context = contextSlots();
        int contextIndex = context.hit(mouseX, mouseY);
        if (contextIndex >= 0) {
            if (mouseButton == 0) {
                focusedSlot = contextIndex;
                PetSnapshotPacket.Entry assigned = PetContextSlotLayout.assigned(ClientPets.entries(), contextIndex);
                focusedPetId = assigned == null ? "" : assigned.petId;
            }
            return;
        }
        int index = tileAt(mouseX, mouseY);
        if (index >= 0) {
            List<PetSnapshotPacket.Entry> entries = ClientPets.entries();
            if (index < entries.size()) {
                PetSnapshotPacket.Entry e = entries.get(index);
                if (mouseButton == 1 && isCtrlKeyDown()) send("revert_pet", e.petId);
                else if (mouseButton == 0) {
                    if (!e.owned) send("purchase_pet", e.petId);
                    else if (!e.selected) send("select_pet", e.petId);
                    else if (e.levelUpReady) send("level_up_pet", e.petId);
                    else send("awaken_pet", e.petId);
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void send(String action, String petId) {
        // A missing Mastery run is a server restriction, not an invitation to
        // fabricate gameplay state on the client. Premium-slot purchase remains allowed.
        if (ClientPets.runId().isEmpty() && !"purchase_premium_slot".equals(action)) return;
        // A single screen keeps at most one gameplay mutation in flight.
        // It is released only by the next server Pets snapshot, not a local click.
        long currentRevision = ClientPets.revision();
        if (pendingRevision >= 0L && currentRevision <= pendingRevision) return;
        pendingRevision = currentRevision;
        NetworkHandler.INSTANCE.sendToServer(new PetActionPacket(action, petId, ClientPets.runId()));
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

    private int lastPage() {
        int count = ClientPets.entries().size();
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
        List<PetSnapshotPacket.Entry> entries = ClientPets.entries();
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
            if (!actualId.equals(focusedPetId)) {
                focusedSlot = -1;
                focusedPetId = "";
            }
        }
        for (int i = 0; i < PetContextSlotLayout.COUNT; i++) {
            boolean unlocked = PetContextSlotLayout.unlocked(i, ClientPets.normalSlots(), ClientPets.premiumSlots());
            PetSnapshotPacket.Entry assigned = PetContextSlotLayout.assigned(entries, i);
            boolean hovered = context.hit(mouseX, mouseY) == i;
            boolean focused = focusedSlot == i;
            int bg = focused ? 0xAA45648A : assigned != null ? 0xAA254A35
                    : unlocked ? GuiTheme.PANEL_BG_SOFT : 0xAA292929;
            int border = focused ? 0xFF99C9FF : hovered ? GuiTheme.SLOT_HOVER_COLOR
                    : GuiTheme.PANEL_BORDER_COLOR;
            GuiDrawing.drawSurface(context.x(i), context.y(i), context.width(), context.cellHeight(), bg, border);
            String title = assigned == null ? (unlocked ? "+" : "X") : assigned.petId;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(title, Math.max(1, context.width() - 6)),
                    context.x(i) + 3, context.y(i) + 3,
                    unlocked ? GuiTheme.TEXT_COLOR : GuiTheme.DISABLED_TEXT_COLOR);
            String sub = assigned == null ? (unlocked ? "ПУСТО" : "ЗАКР.")
                    : (assigned.premiumSlot ? "АКТ. [P]" : "АКТИВЕН");
            fontRenderer.drawString(fontRenderer.trimStringToWidth(sub, Math.max(1, context.width() - 6)),
                    context.x(i) + 3, context.y(i) + Math.min(context.cellHeight() - 9, 16),
                    assigned != null ? 0xA8E6B8 : GuiTheme.DISABLED_TEXT_COLOR);
        }
        page = Math.min(page, lastPage());
        int start = page * perPage();
        long now = System.currentTimeMillis();
        for (int slot = 0; slot < perPage() && start + slot < entries.size(); slot++) {
            PetSnapshotPacket.Entry e = entries.get(start + slot);
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
        if (!entries.isEmpty())
            fontRenderer.drawString(fontRenderer.trimStringToWidth(
                    "ЛКМ: купить / выбрать / улучшить / пробудить   Ctrl+ПКМ: снизить ранг", Math.max(8, right-left)),
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
        super.drawScreen(mouseX, mouseY, partialTicks);
        int hoveredSlot = context.hit(mouseX, mouseY);
        if (hoveredSlot >= 0) {
            PetSnapshotPacket.Entry hoverPet = PetContextSlotLayout.assigned(entries, hoveredSlot);
            String description = hoverPet == null
                    ? (PetContextSlotLayout.unlocked(hoveredSlot, ClientPets.normalSlots(), ClientPets.premiumSlots())
                    ? "Пустой слот" : "Слот пока не разблокирован")
                    : hoverPet.petId + " | " + hoverPet.rank + " | Ур. " + hoverPet.level;
            drawHoveringText(java.util.Collections.singletonList(description), mouseX, mouseY);
        }
    }

    private static String formatCooldown(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return (seconds / 3600L) + "ч " + ((seconds % 3600L) / 60L) + "м";
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
