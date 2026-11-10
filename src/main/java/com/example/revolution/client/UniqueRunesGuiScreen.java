package com.example.revolution.client;

import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Unique rune collections inside the common E-menu chrome. */
public final class UniqueRunesGuiScreen extends GuiScreen {
    private static final String[] COLLECTIONS =
            {"CRYSTAL_COLLECTION", "DAILY_COLLECTION", "OFFERINGS_COLLECTION"};
    private static final int ACQUIRE = 520, AWAKEN = 521, ROLLBACK = 522;
    /** E-menu tab preference lives only for the current client session. */
    private static int sessionSelected;
    private int selected;
    private boolean rollbackArmed;
    private BigDecimal previousWalletCrystals;


    public UniqueRunesGuiScreen() { this(sessionSelected); }

    static void resetSessionSelection() { sessionSelected = 0; }

    /** Open the specific special collection selected from the main rune strip. */
    public UniqueRunesGuiScreen(int selected) {
        if (selected < 0 || selected >= COLLECTIONS.length)
            throw new IllegalArgumentException("Unknown unique rune collection index: " + selected);
        this.selected = selected;
        sessionSelected = selected;
    }

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
        previousWalletCrystals = ClientPlayerStats.getCrystals();
        ClientUniqueRunes.sync();
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.UNIQUE_RUNES, mc)) return;
        if (button.id >= 510 && button.id <= 512) {
            selected = button.id - 510; sessionSelected = selected;
            rollbackArmed = false; return;
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
            // No implicit first-rune awakening in this alternate UI: E-menu handles explicit selected runes.
        } else if (button.id == ROLLBACK) {
            if (!rollbackArmed) { rollbackArmed = true; return; }
            // Never emit an untargeted rollback; E-menu enforces two gestures on the same tile.
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
        UnifiedMainMenuChrome.draw(mc, width, height, mouseX, mouseY, "УНИКАЛЬНЫЕ РУНЫ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        // Main wallet updates can arrive independently of Unique Rune snapshots.
        // Re-request the authoritative snapshot when the displayed Crystal wallet changes.
        BigDecimal wallet = ClientPlayerStats.getCrystals();
        if (previousWalletCrystals != null && wallet.compareTo(previousWalletCrystals) != 0)
            ClientUniqueRunes.sync();
        previousWalletCrystals = wallet;
        UniqueRuneSnapshotPacket snapshot = ClientUniqueRunes.snapshot();
        String collection = COLLECTIONS[selected];
        UniqueRuneSnapshotPacket.Collection state = state(snapshot, collection);
        List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        updateActionButtons(snapshot, state, owned, collection);
        int left = left(), top = UnifiedMainMenuChrome.contentTop(width, height) + 28;
        if (state != null) {
            String status = state.unlocked ? "" : "  [ЗАКРЫТО]";
            String lower = state.rarity + " " + state.progress + "/26"
                    + "   IMPROVED " + state.improvedProgress + "/26";
            String upper = "ADVANCED " + state.advancedProgress + "/26"
                    + "   MYTHIC " + state.mythicProgress + "/26";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(lower + status, right() - left),
                    left, top, GuiTheme.TEXT_COLOR);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(upper, right() - left),
                    left, top + 12, GuiTheme.TEXT_COLOR);
        }
        String resources = "Кристаллы: " + snapshot.getCrystals()
                + "   Жемчуг: " + snapshot.getPearl()
                + "   Подношения: " + snapshot.getOfferings();
        fontRenderer.drawString(fontRenderer.trimStringToWidth(resources, right() - left),
                left, top + 27, GuiTheme.SILVER_COLOR);
        String cost = "CRYSTAL_COLLECTION".equals(collection)
                ? "Цена: " + snapshot.getCrystalPurchaseCost() + " кристаллов"
                : "DAILY_COLLECTION".equals(collection)
                ? "Ежедневно: уникальная руна и дополнительная награда"
                : "Цена: " + snapshot.getOfferingsPurchaseCost() + " подношений";
        fontRenderer.drawString(fontRenderer.trimStringToWidth(cost, right() - left),
                left, top + 44, GuiTheme.DISABLED_TEXT_COLOR);
        if ("CRYSTAL_COLLECTION".equals(collection)) {
            String enhancementPrice = "Случайное усиление: " + snapshot.getCrystalEnhancementCost() + " жемчуг";
            fontRenderer.drawString(fontRenderer.trimStringToWidth(enhancementPrice, right() - left),
                    left, top + 56, GuiTheme.DISABLED_TEXT_COLOR);
        }

        for (int slot = 0; slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            UniqueRuneSnapshotPacket.Entry entry = runeAtSlot(owned, slot);
            boolean lastRolled = "CRYSTAL_COLLECTION".equals(collection)
                    && entry != null && entry.runeId.equals(snapshot.getLastCrystalRollId());
            GuiDrawing.drawSurface(x, y, size, size,
                    GuiTheme.SLOT_BG_COLOR, lastRolled ? GuiTheme.CRYSTAL_COLOR : GuiTheme.SLOT_BORDER_COLOR);
            // Number every position so a C-14 roll is visibly in slot 14, not slot 2.
            String text = String.format(java.util.Locale.ROOT, "%02d", slot + 1);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(text, size-4),
                    x + 2, y + Math.max(2, (size - fontRenderer.FONT_HEIGHT) / 2),
                    entry == null ? GuiTheme.DISABLED_TEXT_COLOR : GuiTheme.TEXT_COLOR);
            // '+' means this factual rank covers the current purchase stage.
            if (entry != null && entry.acquiredCommon)
                fontRenderer.drawString("+", x + size - 9, y + size - 11, GuiTheme.CRYSTAL_COLOR);
        }
        if (rollbackArmed) {
            fontRenderer.drawString(fontRenderer.trimStringToWidth(
                    "Внимание: откат тратит кристаллы, компенсация не выдаётся. Повторите для подтверждения.",
                    Math.max(8, right()-left)), left, height - 49, 0xFFFF7777);
        } else {
            String purchaseResult = "ZBT: " + snapshot.getZbtStatus();
            if ("CRYSTAL_COLLECTION".equals(collection) && !snapshot.getLastCrystalRollId().isEmpty()) {
                purchaseResult = "Выпала: " + snapshot.getLastCrystalRollId().replace("crystal_", "");
                if (snapshot.isLastCrystalRollDuplicate())
                    purchaseResult += " (дубликат: +" + snapshot.getLastCrystalRollCompensation() + " жемчуг)";
            }
            fontRenderer.drawString(fontRenderer.trimStringToWidth(purchaseResult, right() - left),
                    left, height - 49, GuiTheme.DISABLED_TEXT_COLOR);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int slot = 0; slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            UniqueRuneSnapshotPacket.Entry e = runeAtSlot(owned, slot);
            if (e != null && mouseX >= x && mouseX < x+size && mouseY >= y && mouseY < y+size) {
                drawHoveringText(Arrays.asList(e.name, e.rarity + " +" + e.enhancement,
                        e.effects,
                        e.acquiredCommon ? "Текущий ранг: зачтена" : "Текущий ранг: не зачтена",
                        "ЛКМ: поделиться", "Shift+ЛКМ: случайное усиление"), mouseX, mouseY);
                break;
            }
        }
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        String collection = COLLECTIONS[selected];
        List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        for (int slot = 0; slot < 26; slot++) {
            int x = slotX(slot), y = slotY(slot), size = slotSize();
            UniqueRuneSnapshotPacket.Entry e = runeAtSlot(owned, slot);
            if (e != null && mouseX >= x && mouseX < x+size && mouseY >= y && mouseY < y+size) {
                rollbackArmed = false;
                if (isShiftKeyDown() && "CRYSTAL_COLLECTION".equals(collection))
                    ClientUniqueRunes.action("enhance", collection, "", false);
                else ClientUniqueRunes.action("share", collection, e.runeId, false);
                return;
            }
        }
    }

    private void updateActionButtons(UniqueRuneSnapshotPacket snapshot,
                                     UniqueRuneSnapshotPacket.Collection state,
                                     List<UniqueRuneSnapshotPacket.Entry> owned, String collection) {
        for (GuiButton button : buttonList) {
            if (button.id >= 510 && button.id <= 512) button.enabled = button.id != 510 + selected;
            if (button.id == ACQUIRE) {
                boolean affordable = true;
                if ("CRYSTAL_COLLECTION".equals(collection))
                    affordable = hasEnough(ClientPlayerStats.getCrystals(), snapshot.getCrystalPurchaseCost());
                else if ("OFFERINGS_COLLECTION".equals(collection))
                    affordable = hasEnough(ClientPlayerStats.getOfferings(), snapshot.getOfferingsPurchaseCost());
                button.enabled = state != null && state.unlocked && affordable
                        && state.progress < 26
                        && (!"DAILY_COLLECTION".equals(collection)
                            || snapshot.getDailyNextClaimMillis() == 0L);
                button.displayString = "DAILY_COLLECTION".equals(collection) ? "Ежедневная награда" : "Купить руну";
            } else if (button.id == AWAKEN) {
                button.enabled = false; // Targeted awakening is available through RunesGuiScreen only.
                button.displayString = "Пробудить";
            } else if (button.id == ROLLBACK) {
                button.enabled = false; // Targeted rollback is available through RunesGuiScreen only.
                button.displayString = rollbackArmed ? "ПОДТВЕРДИТЬ ОТКАТ" : "Откат";
            }
        }
    }

    private static boolean hasEnough(BigDecimal balance, String cost) {
        try { return balance.compareTo(new BigDecimal(cost)) >= 0; }
        catch (RuntimeException invalidCost) { return false; }
    }

    /** Fixed numbered slots prevent newly acquired random runes from shifting existing slots. */
    private static UniqueRuneSnapshotPacket.Entry runeAtSlot(List<UniqueRuneSnapshotPacket.Entry> owned, int slot) {
        String suffix = String.format(java.util.Locale.ROOT, "_%02d", slot + 1);
        for (UniqueRuneSnapshotPacket.Entry entry : owned)
            if (entry.runeId.endsWith(suffix)) return entry;
        return null;
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
