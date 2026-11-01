package com.example.revolution.client.mechanic;

import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Functional v2.12.0 Unlock Journal; final art polish is intentionally out of scope. */
public final class MechanicUnlockGuiScreen extends GuiScreen {
    private static final AtomicLong NONCE = new AtomicLong(System.currentTimeMillis());
    private static final int FIRST_UNLOCK_BUTTON = 4100;
    @Override public void initGui() {
        super.initGui();
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        refreshButtons();
    }
    public void refreshButtons() {
        buttonList.clear();
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        int y = 48;
        for (int i = 0; i < entries.size() && i < 10; i++) {
            MechanicUnlockSnapshotPacket.Entry e = entries.get(i);
            if (e.eligible && !e.unlocked) buttonList.add(new GuiButton(FIRST_UNLOCK_BUTTON + i, width / 2 + 95, y - 5, 72, 20, "Unlock"));
            y += 22;
        }
        buttonList.add(new GuiButton(4999, width / 2 - 40, Math.min(height - 28, 48 + entries.size() * 22), 80, 20, "Back"));
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 4999) { mc.displayGuiScreen(null); return; }
        int index = button.id - FIRST_UNLOCK_BUTTON;
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        if (index >= 0 && index < entries.size()) NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("unlock", entries.get(index).unlockId, NONCE.incrementAndGet()));
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "Mechanic Unlocks", width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRenderer, "ZBT thresholds: " + ClientMechanicUnlocks.zbtStatus(), width / 2, 29, 0xAAAAAA);
        int y = 48;
        for (MechanicUnlockSnapshotPacket.Entry e : ClientMechanicUnlocks.entries()) {
            String state = e.unlocked ? "UNLOCKED" : e.eligible ? "READY TO UNLOCK" : "LOCKED";
            String text = e.displayName + "  " + e.rarity + " " + e.progress + "/" + e.target + "  " + state;
            fontRenderer.drawString(text, width / 2 - 165, y, e.eligible && !e.unlocked ? 0xFFFF55 : e.unlocked ? 0x55FF55 : 0xFFFFFF);
            y += 22;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
