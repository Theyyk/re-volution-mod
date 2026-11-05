package com.example.revolution.client;

import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Unique rune collections inside the common E-menu chrome. */
public final class UniqueRunesGuiScreen extends GuiScreen {
    private static final String[] COLLECTIONS =
            {"CRYSTAL_COLLECTION", "DAILY_COLLECTION", "OFFERINGS_COLLECTION"};
    private static final int ACQUIRE = 520, AWAKEN = 521, ROLLBACK = 522;
    private int selected;
    private boolean rollbackArmed;

    @Override public void initGui() {
        buttonList.clear();
        UnifiedMainMenuChrome.installNavigation(buttonList, width, height, UiMenuSection.UNIQUE_RUNES);
        int left = UnifiedMainMenuChrome.contentLeft(width, height);
        int right = UnifiedMainMenuChrome.contentRight(width, height);
        int colWidth = Math.max(35, (right - left - 10) / 3);
        int y = UnifiedMainMenuChrome.contentTop(width, height);
        for (int i = 0; i < COLLECTIONS.length; i++) {
            buttonList.add(new GuiControls.PanelButton(
                    510 + i, left + i * (colWidth + 5), y, colWidth, 20, label(COLLECTIONS[i])));
        }
        for (int i = 0; i < 3; i++) {
            buttonList.add(new GuiControls.PanelButton(ACQUIRE + i,
                    left + i * (colWidth + 5), height - 32, colWidth, 22, ""));
        }
        ClientUniqueRunes.sync();
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.UNIQUE_RUNES, mc)) return;
        if (button.id >= 510 && button.id <= 512) {
            selected = button.id - 510; rollbackArmed = false; return;
        }
        String collection = COLLECTIONS[selected];
        if (button.id == ACQUIRE) {
            rollbackArmed = false;
            if ("CRYSTAL_COLLECTION".equals(collection))
                ClientUniqueRunes.action("purchase_crystal", collection, "", false);
            else if ("DAILY_COLLECTION".equals(collection))
                ClientUniqueRunes.action("daily", collection, "", false);
            else ClientUniqueRunes.action("purchase_offerings", collection, "", false);
        } else if (button.id == AWAKEN) {
            rollbackArmed = false;
            List<UniqueRuneSnapshotPacket.Entry> entries = ClientUniqueRunes.entriesFor(collection);
            if (!entries.isEmpty()) ClientUniqueRunes.action("awaken", collection, entries.get(0).runeId, false);
        } else if (button.id == ROLLBACK) {
            if (!rollbackArmed) { rollbackArmed = true; return; }
            ClientUniqueRunes.action("rollback", collection, "", true);
            rollbackArmed = false;
        }
    }

    private int left() { return UnifiedMainMenuChrome.contentLeft(width, height); }
    private int right() { return UnifiedMainMenuChrome.contentRight(width, height); }
    private int slotSize() { return Math.max(14, Math.min(50, (right() - left() - 9 * 4) / 10)); }
    private int gridY() { return UnifiedMainMenuChrome.contentTop(width, height) + 93; }
    private int slotX(int slot) { return left() + (slot % 10) * (slotSize() + 4); }
    private int slotY(int slot) { return gridY() + (slot / 10) * (slotSize() + 4); }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        UnifiedMainMenuChrome.draw(mc, width, height, "УНИКАЛЬНЫЕ РУНЫ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        UniqueRuneSnapshotPacket snapshot = ClientUniqueRunes.snapshot();
        String collection = COLLECTIONS[selected];
        UniqueRuneSnapshotPacket.Collection state = state(snapshot, collection);
        List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        updateActionButtons(state, owned, collection);
        int left = left(), top = UnifiedMainMenuChrome.contentTop(width, height) + 28;
        if (state != null) {
            String status = state.unlocked ? "" : "  [ЗАКРЫТО]";
            fontRenderer.drawString("Редкость: " + state.rarity + "   Прогресс: "
                    + state.progress + "/26" + status, left, top, GuiTheme.TEXT_COLOR);
        }
        String resources = "Кристаллы: " + snapshot.getCrystals()
                + "   Жемчуг: " + snapshot.getPearl()
                + "   Подношения: " + snapshot.getOfferings();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(resources, right() - left),
                left, top + 16, GuiTheme.SILVER_COLOR);
        String cost = "CRYSTAL_COLLECTION".equals(collection)
                ? "Цена: " + snapshot.getCrystalPurchaseCost() + " кристаллов"
                : "DAILY_COLLECTION".equals(collection)
                ? "Ежедневно: уникальная руна и дополнительная награда"
                : "Цена: " + snapshot.getOfferingsPurchaseCost() + " подношений";
        fontRenderer.drawString(fontRenderer.trimStringToWidth(cost, right() - left),
                left, top + 33, GuiTheme.DISABLED_TEXT_COLOR);

        for (int slot = 0; slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            GuiDrawing.drawSurface(x, y, size, size,
                    GuiTheme.SLOT_BG_COLOR, GuiTheme.SLOT_BORDER_COLOR);
            String text = slot < owned.size() ? owned.get(slot).runeId.replaceFirst("^[^_]+_", "") : "?";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(text, size-4),
                    x + 2, y + Math.max(2, (size - fontRenderer.FONT_HEIGHT) / 2), GuiTheme.TEXT_COLOR);
        }
        if (rollbackArmed) {
            fontRenderer.drawString(fontRenderer.trimStringToWidth(
                    "Внимание: откат тратит кристаллы, компенсация не выдаётся. Повторите для подтверждения.",
                    Math.max(8, right()-left)), left, height - 49, 0xFFFF7777);
        } else {
            fontRenderer.drawString("ZBT: " + snapshot.getZbtStatus(),
                    left, height - 49, GuiTheme.DISABLED_TEXT_COLOR);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int slot = 0; slot < owned.size() && slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            if (mouseX >= x && mouseX < x+size && mouseY >= y && mouseY < y+size) {
                UniqueRuneSnapshotPacket.Entry e = owned.get(slot);
                drawHoveringText(Arrays.asList(e.name, e.rarity + " +" + e.enhancement,
                        e.effects, "ЛКМ: поделиться", "Shift+ЛКМ: усилить (кристаллы)"), mouseX, mouseY);
                break;
            }
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        String collection = COLLECTIONS[selected];
        List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        for (int slot = 0; slot < owned.size() && slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            if (mouseX >= x && mouseX < x+size && mouseY >= y && mouseY < y+size) {
                UniqueRuneSnapshotPacket.Entry e = owned.get(slot);
                rollbackArmed = false;
                if (isShiftKeyDown() && "CRYSTAL_COLLECTION".equals(collection))
                    ClientUniqueRunes.action("enhance", collection, e.runeId, false);
                else ClientUniqueRunes.action("share", collection, e.runeId, false);
                return;
            }
        }
    }

    private void updateActionButtons(UniqueRuneSnapshotPacket.Collection state,
                                     List<UniqueRuneSnapshotPacket.Entry> owned, String collection) {
        for (GuiButton button : buttonList) {
            if (button.id >= 510 && button.id <= 512) button.enabled = button.id != 510 + selected;
            if (button.id == ACQUIRE) {
                button.enabled = state != null && state.unlocked;
                button.displayString = "DAILY_COLLECTION".equals(collection) ? "Ежедневная награда" : "Купить руну";
            } else if (button.id == AWAKEN) {
                button.enabled = state != null && state.unlocked && state.canAwaken && !owned.isEmpty();
                button.displayString = "Пробудить";
            } else if (button.id == ROLLBACK) {
                button.enabled = state != null && state.unlocked && state.canRollback;
                button.displayString = rollbackArmed ? "ПОДТВЕРДИТЬ ОТКАТ" : "Откат";
            }
        }
    }

    private static UniqueRuneSnapshotPacket.Collection state(UniqueRuneSnapshotPacket snapshot, String collection) {
        for (UniqueRuneSnapshotPacket.Collection item : snapshot.getCollections())
            if (collection.equals(item.collectionId)) return item;
        return null;
    }

    private static String label(String id) {
        return id.startsWith("CRYSTAL") ? "Кристаллы"
                : id.startsWith("DAILY") ? "Ежедневные" : "Подношения";
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
