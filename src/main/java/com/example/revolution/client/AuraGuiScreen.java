package com.example.revolution.client;

import com.example.revolution.client.network.AuraClientState;
import com.example.revolution.network.AuraActionPacket;
import com.example.revolution.network.AuraSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional v2.9 Aura UI. Intentionally structured/plain; final art and animations are post-v2.9 polish. */
public final class AuraGuiScreen extends GuiScreen {
    private static long nonce = System.currentTimeMillis();
    private final List<AuraSnapshotPacket.Entry> visible = new ArrayList<>();
    private String rarityFilter = "ALL";
    private int selectedIndex = -1;

    @Override public void initGui() {
        buttonList.clear();
        int left = width / 2 - 155, top = height / 2 - 105;
        buttonList.add(new GuiButton(1, left, top + 28, 48, 20, "Open x1"));
        buttonList.add(new GuiButton(2, left + 52, top + 28, 48, 20, "Open x3"));
        buttonList.add(new GuiButton(3, left + 104, top + 28, 48, 20, "Open x5"));
        buttonList.add(new GuiButton(4, left + 156, top + 28, 52, 20, "Open x10"));
        buttonList.add(new GuiButton(5, left + 212, top + 28, 98, 20, AuraClientState.skipAnimation() ? "Skip: ON" : "Skip: OFF"));
        buttonList.add(new GuiButton(10, left, top + 53, 82, 20, "Filter: " + rarityFilter));
        buttonList.add(new GuiButton(11, left + 86, top + 53, 56, 20, "Equip"));
        buttonList.add(new GuiButton(12, left + 146, top + 53, 64, 20, "Unequip"));
        buttonList.add(new GuiButton(13, left + 214, top + 53, 46, 20, "Pin"));
        buttonList.add(new GuiButton(14, left + 264, top + 53, 46, 20, "Unpin"));
        buttonList.add(new GuiButton(20, left, top + 184, 90, 20, "Craft Auto"));
        buttonList.add(new GuiButton(21, left + 94, top + 184, 90, 20, "Dismantle"));
        buttonList.add(new GuiButton(22, left + 188, top + 184, 58, 20, "Refresh"));
        buttonList.add(new GuiButton(23, left + 250, top + 184, 60, 20, "Close"));
        rebuildVisible();
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= 1 && button.id <= 4) {
            int[] amounts = {1, 3, 5, 10}; send("open", String.valueOf(amounts[button.id - 1]), ""); return;
        }
        if (button.id == 5) { send("skip_animation", String.valueOf(!AuraClientState.skipAnimation()), ""); return; }
        if (button.id == 10) { cycleFilter(); initGui(); return; }
        AuraSnapshotPacket.Entry selected = selected();
        if (button.id == 11 && selected != null) send("equip", selected.instanceId, "");
        else if (button.id == 12) send("unequip", "", "");
        else if (button.id == 13 && selected != null) send("pin", selected.instanceId, "");
        else if (button.id == 14 && selected != null) send("unpin", selected.instanceId, "");
        else if (button.id == 20 && selected != null && !"MYTHIC".equals(selected.rarity)) sendAutoCraft(selected.rarity);
        else if (button.id == 21 && selected != null && !"COMMON".equals(selected.rarity)) send("dismantle", selected.instanceId, "");
        else if (button.id == 22) send("sync", "", "");
        else if (button.id == 23) mc.displayGuiScreen(null);
    }

    private void sendAutoCraft(String rarity) {
        List<String> ids = new ArrayList<>();
        for (AuraSnapshotPacket.Entry entry : AuraClientState.entries()) {
            if (rarity.equals(entry.rarity) && !entry.pinned && !entry.active) ids.add(entry.instanceId);
        }
        int required = "COMMON".equals(rarity) ? 3 : "RARE".equals(rarity) ? 4 : "EPIC".equals(rarity) ? 5 : 6;
        if (ids.size() < required) return;
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < required; i++) { if (joined.length() > 0) joined.append(','); joined.append(ids.get(i)); }
        send("craft", rarity, joined.toString());
    }

    private void send(String action, String value, String ids) {
        NetworkHandler.INSTANCE.sendToServer(new AuraActionPacket(action, value, ids, ++nonce));
    }

    private void cycleFilter() {
        String[] values = {"ALL", "COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC"};
        int index = 0; for (int i = 0; i < values.length; i++) if (values[i].equals(rarityFilter)) index = i;
        rarityFilter = values[(index + 1) % values.length]; selectedIndex = -1; rebuildVisible();
    }

    private void rebuildVisible() {
        visible.clear();
        for (AuraSnapshotPacket.Entry entry : AuraClientState.entries()) if ("ALL".equals(rarityFilter) || rarityFilter.equals(entry.rarity)) visible.add(entry);
        visible.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
        if (selectedIndex >= visible.size()) selectedIndex = -1;
    }

    private AuraSnapshotPacket.Entry selected() { return selectedIndex >= 0 && selectedIndex < visible.size() ? visible.get(selectedIndex) : null; }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int left = width / 2 - 155, top = height / 2 - 105;
        if (mouseX < left || mouseX > left + 310 || mouseY < top + 78 || mouseY > top + 174) return;
        int row = (mouseY - (top + 78)) / 12;
        if (row >= 0 && row < Math.min(8, visible.size())) selectedIndex = row;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 155, top = height / 2 - 105;
        drawCenteredString(fontRenderer, "Aura System", width / 2, top + 5, 0xFFFFFF);
        drawString(fontRenderer, "Keys: " + AuraClientState.auraKeys() + "   Crystals: " + AuraClientState.crystals()
                + "   Missing key price: " + AuraClientState.keyCrystalPrice(), left, top + 16, 0xDDDDDD);
        drawString(fontRenderer, "Chest opens: " + AuraClientState.totalChestOpens() + "   Chest drops: " + AuraClientState.totalChestAuraDrops(), left, top + 76, 0xAAAAAA);
        rebuildVisible();
        int y = top + 90;
        for (int i = 0; i < visible.size() && i < 8; i++) {
            AuraSnapshotPacket.Entry e = visible.get(i);
            String prefix = i == selectedIndex ? "> " : "  ";
            String flags = (e.active ? " [ACTIVE]" : "") + (e.pinned ? " [PIN]" : "");
            drawString(fontRenderer, prefix + e.rarity + flags + "  " + e.aggregatedEffects, left, y, i == selectedIndex ? 0xFFFFFF : 0xCCCCCC);
            y += 12;
        }
        AuraSnapshotPacket.Entry selected = selected();
        if (selected != null) drawString(fontRenderer, "Raw: " + selected.rawRolls, left, top + 166, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
