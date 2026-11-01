package com.example.revolution.client;

import com.example.revolution.network.UniqueRuneSnapshotPacket;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional-first v2.11.0 screen: 3 fixed collections, hidden unknowns and server actions. */
public final class UniqueRunesGuiScreen extends GuiScreen {
    private static final String[] COLLECTIONS = {"CRYSTAL_COLLECTION", "DAILY_COLLECTION", "OFFERINGS_COLLECTION"};
    private int selected;
    private boolean rollbackArmed;

    @Override public void initGui() {
        buttonList.clear();
        for (int i = 0; i < COLLECTIONS.length; i++) buttonList.add(new GuiButton(100 + i, 16 + i * 92, 20, 88, 20, label(COLLECTIONS[i])));
        buttonList.add(new GuiButton(200, 16, height - 48, 100, 20, "Acquire"));
        buttonList.add(new GuiButton(201, 120, height - 48, 100, 20, "Awaken"));
        buttonList.add(new GuiButton(202, 224, height - 48, 126, 20, "Rollback"));
        ClientUniqueRunes.sync();
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 100 && button.id <= 102) {
            selected = button.id - 100;
            rollbackArmed = false;
            return;
        }
        String collection = COLLECTIONS[selected];
        if (button.id == 200) {
            rollbackArmed = false;
            if ("CRYSTAL_COLLECTION".equals(collection)) ClientUniqueRunes.action("purchase_crystal", collection, "", false);
            else if ("DAILY_COLLECTION".equals(collection)) ClientUniqueRunes.action("daily", collection, "", false);
            else ClientUniqueRunes.action("purchase_offerings", collection, "", false);
        } else if (button.id == 201) {
            rollbackArmed = false;
            List<UniqueRuneSnapshotPacket.Entry> entries = ClientUniqueRunes.entriesFor(collection);
            if (!entries.isEmpty()) ClientUniqueRunes.action("awaken", collection, entries.get(0).runeId, false);
        } else if (button.id == 202) {
            if (!rollbackArmed) {
                rollbackArmed = true;
                return;
            }
            ClientUniqueRunes.action("rollback", collection, "", true);
            rollbackArmed = false;
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        UniqueRuneSnapshotPacket snapshot = ClientUniqueRunes.snapshot();
        String collection = COLLECTIONS[selected]; UniqueRuneSnapshotPacket.Collection state = state(snapshot, collection);
        List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        updateActionButtons(state, owned, collection);

        drawCenteredString(fontRenderer, "Unique Rune Collections", width / 2, 6, 0xFFFFFF);
        int y = 48;
        if (state != null) {
            drawString(fontRenderer, "Rarity: " + state.rarity + "   Progress: " + state.progress + " / 26" + (state.unlocked ? "" : "   [LOCKED]"), 16, y, 0xFFFFFF);
            y += 14;
        }
        drawString(fontRenderer, "Crystals: " + snapshot.getCrystals() + "   Pearl: " + snapshot.getPearl() + "   Offerings: " + snapshot.getOfferings(), 16, y, 0xDDDDDD); y += 18;
        if ("CRYSTAL_COLLECTION".equals(collection)) { drawString(fontRenderer, "Purchase: " + snapshot.getCrystalPurchaseCost() + " Crystals", 16, y, 0xCCCCCC); y += 14; }
        if ("DAILY_COLLECTION".equals(collection)) { drawString(fontRenderer, "Daily: non-duplicate Rune + configured extra reward", 16, y, 0xCCCCCC); y += 14; }
        if ("OFFERINGS_COLLECTION".equals(collection)) { drawString(fontRenderer, "Purchase: " + snapshot.getOfferingsPurchaseCost() + " Offerings", 16, y, 0xCCCCCC); y += 14; }

        for (int slot = 0; slot < 26; slot++) {
            int col = slot % 13, row = slot / 13; int x = 16 + col * 38, sy = y + row * 34;
            String text = slot < owned.size() ? owned.get(slot).runeId.replaceFirst("^[^_]+_", "") : "?";
            drawRect(x, sy, x + 32, sy + 28, 0x66000000); drawCenteredString(fontRenderer, text, x + 16, sy + 10, 0xFFFFFF);
            if (slot < owned.size() && mouseX >= x && mouseX <= x + 32 && mouseY >= sy && mouseY <= sy + 28) {
                UniqueRuneSnapshotPacket.Entry entry = owned.get(slot);
                drawHoveringText(java.util.Arrays.asList(entry.name, entry.rarity + "  +" + entry.enhancement, entry.effects,
                        "Click: Share", "Shift-click: Enhance (Crystal only)"), mouseX, mouseY);
            }
        }
        if (rollbackArmed) drawString(fontRenderer, "WARNING: rollback is destructive, costs Crystals and gives no compensation. Click Rollback again to confirm.", 16, height - 64, 0xFF7777);
        else drawString(fontRenderer, "ZBT: " + snapshot.getZbtStatus(), 16, height - 64, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        String collection = COLLECTIONS[selected]; List<UniqueRuneSnapshotPacket.Entry> owned = ClientUniqueRunes.entriesFor(collection);
        int baseY = gridY(collection);
        for (int slot = 0; slot < owned.size() && slot < 26; slot++) {
            int x = 16 + (slot % 13) * 38, y = baseY + (slot / 13) * 34;
            if (mouseX >= x && mouseX <= x + 32 && mouseY >= y && mouseY <= y + 28) {
                UniqueRuneSnapshotPacket.Entry entry = owned.get(slot);
                rollbackArmed = false;
                if (isShiftKeyDown() && "CRYSTAL_COLLECTION".equals(collection)) ClientUniqueRunes.action("enhance", collection, entry.runeId, false);
                else ClientUniqueRunes.action("share", collection, entry.runeId, false);
                return;
            }
        }
    }

    private void updateActionButtons(UniqueRuneSnapshotPacket.Collection state, List<UniqueRuneSnapshotPacket.Entry> owned, String collection) {
        for (GuiButton button : buttonList) {
            if (button.id == 200) {
                button.enabled = state != null && state.unlocked;
                button.displayString = "CRYSTAL_COLLECTION".equals(collection) ? "Buy Rune" : "DAILY_COLLECTION".equals(collection) ? "Claim Daily" : "Buy Rune";
            } else if (button.id == 201) {
                button.enabled = state != null && state.unlocked && state.canAwaken && !owned.isEmpty();
            } else if (button.id == 202) {
                button.enabled = state != null && state.unlocked && state.canRollback;
                button.displayString = rollbackArmed ? "CONFIRM ROLLBACK" : "Rollback";
            }
        }
    }

    private static UniqueRuneSnapshotPacket.Collection state(UniqueRuneSnapshotPacket snapshot, String collection) {
        for (UniqueRuneSnapshotPacket.Collection item : snapshot.getCollections()) if (collection.equals(item.collectionId)) return item;
        return null;
    }

    private static int gridY(String collection) { return 94; }
    private static String label(String id) { return id.startsWith("CRYSTAL") ? "Crystal" : id.startsWith("DAILY") ? "Daily" : "Offerings"; }
}
