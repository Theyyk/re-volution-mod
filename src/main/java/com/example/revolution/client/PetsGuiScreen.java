package com.example.revolution.client;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PetActionPacket;
import com.example.revolution.network.PetSnapshotPacket;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional v2.4.0 Pet tab. Final art/naming/polish is intentionally deferred. */
public final class PetsGuiScreen extends GuiScreen {
    private static final int CLOSE = 299;
    private static final int RUNES = 200;
    private static final int BUY_NORMAL_SLOT = 500;
    private static final int BUY_PREMIUM_SLOT = 501;
    private static final int TILE_W = 180;
    private static final int TILE_H = 57;
    private static final int GAP = 7;

    @Override public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(RUNES, 8, 8, 76, 20, "Руны"));
        buttonList.add(new GuiButton(CLOSE, width - 28, 8, 20, 20, "X"));
        buttonList.add(new GuiButton(BUY_NORMAL_SLOT, 96, 8, 190, 20,
                "Normal slot: " + ClientPets.nextNormalSlotCost() + " фрагм."));
        GuiButton premium = new GuiButton(BUY_PREMIUM_SLOT, 292, 8, 180, 20, "Premium slot (Crystals)");
        premium.enabled = ClientPets.premiumSlots() < 2;
        buttonList.add(premium);
        NetworkHandler.INSTANCE.sendToServer(new PetActionPacket("sync"));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == CLOSE) { mc.displayGuiScreen(null); return; }
        if (button.id == RUNES) { ClientUiBridge.beginRuneOpen(); mc.displayGuiScreen(null); return; }
        if (button.id == BUY_NORMAL_SLOT) send("purchase_normal_slot", "");
        if (button.id == BUY_PREMIUM_SLOT) send("purchase_premium_slot", "");
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int index = tileAt(mouseX, mouseY);
        if (index >= 0) {
            List<PetSnapshotPacket.Entry> entries = ClientPets.entries();
            if (index < entries.size()) {
                PetSnapshotPacket.Entry e = entries.get(index);
                if (mouseButton == 1 && isShiftKeyDown()) send("revert_pet", e.petId);
                else if (mouseButton == 0) {
                    if (!e.owned) send("purchase_pet", e.petId);
                    else if (!e.selected) send("select_pet", e.petId);
                    else send("awaken_pet", e.petId);
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void send(String action, String petId) {
        NetworkHandler.INSTANCE.sendToServer(new PetActionPacket(action, petId));
    }

    private int tileAt(int mouseX, int mouseY) {
        int cols = Math.max(1, Math.min(4, (width - 24 + GAP) / (TILE_W + GAP)));
        int gridW = cols * TILE_W + (cols - 1) * GAP;
        int left = (width - gridW) / 2;
        int top = 62;
        for (int i = 0; i < ClientPets.entries().size(); i++) {
            int x = left + (i % cols) * (TILE_W + GAP);
            int y = top + (i / cols) * (TILE_H + GAP);
            if (mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H) return i;
        }
        return -1;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "PETS", width / 2, 13, 0xFFFFFF);
        String slots = "Active slots: " + ClientPets.normalSlots() + "/8 normal + " + ClientPets.premiumSlots() + "/2 premium";
        drawCenteredString(fontRenderer, slots, width / 2, 38, 0xD8D8D8);

        List<PetSnapshotPacket.Entry> entries = ClientPets.entries();
        int cols = Math.max(1, Math.min(4, (width - 24 + GAP) / (TILE_W + GAP)));
        int gridW = cols * TILE_W + (cols - 1) * GAP;
        int left = (width - gridW) / 2;
        int top = 62;
        long now = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); i++) {
            PetSnapshotPacket.Entry e = entries.get(i);
            int x = left + (i % cols) * (TILE_W + GAP);
            int y = top + (i / cols) * (TILE_H + GAP);
            boolean hover = mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H;
            int bg = e.selected && e.premiumSlot ? 0xAA4D2968 : e.selected ? 0xAA254A35 : 0xAA20242A;
            int border = hover ? 0xFFFFFFFF : 0xFF555B66;
            drawRect(x, y, x + TILE_W, y + TILE_H, border);
            drawRect(x + 1, y + 1, x + TILE_W - 1, y + TILE_H - 1, bg);
            fontRenderer.drawString(e.petId + "  " + e.rank, x + 5, y + 5, 0xFFFFFF);
            fontRenderer.drawString("Lvl " + e.level + "  XP " + e.xp, x + 5, y + 17, 0xD0D0D0);
            String state;
            if (e.cooldownUntil > now) state = "CD " + formatCooldown(e.cooldownUntil - now);
            else if (e.selected) state = "ACTIVE" + (e.premiumSlot ? " [P]" : "");
            else if (e.owned) state = "Доступен";
            else state = "Цена " + e.purchaseCost + " coins";
            fontRenderer.drawString(state, x + 5, y + 29, e.cooldownUntil > now ? 0xFFAA66 : 0xA8E6B8);
            String effects = e.effects.length() > 28 ? e.effects.substring(0, 28) + "..." : e.effects;
            fontRenderer.drawString(effects, x + 5, y + 41, 0x9EA7B3);
        }
        if (!entries.isEmpty()) drawString(fontRenderer,
                "ЛКМ: купить / выбрать / пробудить   Shift+ПКМ: ранг -1", 10, height - 16, 0xB8C0CA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static String formatCooldown(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L; long minutes = (seconds % 3600L) / 60L;
        return hours + "h " + minutes + "m";
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
