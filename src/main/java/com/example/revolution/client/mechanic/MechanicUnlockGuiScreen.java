package com.example.revolution.client.mechanic;

import com.example.revolution.client.UnifiedMainMenuChrome;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Server-authoritative Unlock Journal, presented inside shared E-menu chrome. */
public final class MechanicUnlockGuiScreen extends GuiScreen {
    private static final AtomicLong NONCE = new AtomicLong(System.currentTimeMillis());
    private static final int FIRST_UNLOCK_BUTTON = 4100;
    private static final int PREVIOUS_PAGE = 4997;
    private static final int NEXT_PAGE = 4998;
    private int page;

    @Override public void initGui() {
        super.initGui();
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        refreshButtons();
    }

    private int visibleRows() {
        return Math.max(1, (height - UnifiedMainMenuChrome.contentTop(width, height) - 58) / 28);
    }

    private int lastPage() {
        int entries = ClientMechanicUnlocks.entries().size();
        return Math.max(0, (entries - 1) / visibleRows());
    }

    public void refreshButtons() {
        buttonList.clear();
        UnifiedMainMenuChrome.installNavigation(buttonList, width, height, UiMenuSection.MECHANIC_UNLOCKS);
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        page = Math.min(page, lastPage());
        int start = page * visibleRows();
        int left = UnifiedMainMenuChrome.contentLeft(width, height);
        int right = UnifiedMainMenuChrome.contentRight(width, height);
        int top = UnifiedMainMenuChrome.contentTop(width, height);
        for (int row = 0; row < visibleRows() && start + row < entries.size(); row++) {
            int index = start + row;
            MechanicUnlockSnapshotPacket.Entry entry = entries.get(index);
            if (entry.eligible && !entry.unlocked) {
                buttonList.add(UnifiedMainMenuChrome.panelButton(
                        FIRST_UNLOCK_BUTTON + index, Math.max(left, right - 94),
                        top + row * 28 + 2, 84, 21, "Открыть"));
            }
        }
        GuiButton prev = UnifiedMainMenuChrome.panelButton(PREVIOUS_PAGE, Math.max(left, right - 126),
                height - 30, 56, 20, "<");
        prev.enabled = page > 0;
        buttonList.add(prev);
        GuiButton next = UnifiedMainMenuChrome.panelButton(NEXT_PAGE, Math.max(left, right - 63),
                height - 30, 56, 20, ">");
        next.enabled = page < lastPage();
        buttonList.add(next);
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.MECHANIC_UNLOCKS, mc)) return;
        if (button.id == PREVIOUS_PAGE) { page = Math.max(0, page - 1); refreshButtons(); return; }
        if (button.id == NEXT_PAGE) { page = Math.min(lastPage(), page + 1); refreshButtons(); return; }
        int index = button.id - FIRST_UNLOCK_BUTTON;
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        if (index >= 0 && index < entries.size()) {
            MechanicUnlockSnapshotPacket.Entry entry = entries.get(index);
            if (entry.eligible && !entry.unlocked)
                NetworkHandler.INSTANCE.sendToServer(
                        new MechanicUnlockActionPacket("unlock", entry.unlockId, NONCE.incrementAndGet()));
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        UnifiedMainMenuChrome.draw(mc, width, height, mouseX, mouseY, "РАЗБЛОКИРОВКИ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        int left = UnifiedMainMenuChrome.contentLeft(width, height);
        int right = UnifiedMainMenuChrome.contentRight(width, height);
        int top = UnifiedMainMenuChrome.contentTop(width, height);
        fontRenderer.drawString("Пороговые значения ZBT: " + ClientMechanicUnlocks.zbtStatus(),
                left, Math.max(25, top - 19), 0xFF909090);
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        page = Math.min(page, lastPage());
        int start = page * visibleRows();
        for (int row = 0; row < visibleRows() && start + row < entries.size(); row++) {
            MechanicUnlockSnapshotPacket.Entry entry = entries.get(start + row);
            int y = top + row * 28;
            UnifiedMainMenuChrome.panel(left, y, Math.max(10, right - left), 25);
            String state = entry.unlocked ? "ОТКРЫТО" : entry.eligible ? "ГОТОВО К ОТКРЫТИЮ" : "ЗАКРЫТО";
            String desc = entry.displayName + "  " + entry.rarity
                    + "  " + entry.progress + "/" + entry.target + "  " + state;
            int usable = right - left - (entry.eligible && !entry.unlocked ? 105 : 14);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(desc, Math.max(8, usable)),
                    left + 8, y + 8, entry.unlocked ? 0xFF8FD4A5
                            : entry.eligible ? 0xFFFFDD55 : 0xFF909090);
        }
        fontRenderer.drawString("Стр. " + (page + 1) + "/" + (lastPage() + 1),
                left, height - 25, 0xFFC9CDD2);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
