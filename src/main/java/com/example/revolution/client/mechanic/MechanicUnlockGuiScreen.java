package com.example.revolution.client.mechanic;

import com.example.revolution.client.UnifiedMainMenuChrome;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/**
 * Server-authored mechanic unlock tree, arranged as category cards in columns.
 * Missing/hidden server entries never appear, and every action is rechecked.
 */
public final class MechanicUnlockGuiScreen extends GuiScreen {
    private static final AtomicLong NONCE = new AtomicLong(System.currentTimeMillis());
    private static final int GAP = 10;
    private static final int CHILD_H = 33;
    private static final int HEADER_H = 116;
    private int firstRow;

    private int left() { return UnifiedMainMenuChrome.contentLeft(width, height); }
    private int right() { return UnifiedMainMenuChrome.contentRight(width, height); }
    private int top() { return UnifiedMainMenuChrome.contentTop(width, height); }
    private int bottom() { return height - 42; }
    private int columns() { return Math.max(1, Math.min(3, (right() - left() + GAP) / 210)); }
    private int columnWidth() { return Math.max(60, (right() - left() - GAP * (columns() - 1)) / columns()); }

    private static final class Group {
        final MechanicUnlockSnapshotPacket.Entry root;
        final List<MechanicUnlockSnapshotPacket.Entry> children = new ArrayList<>();
        Group(MechanicUnlockSnapshotPacket.Entry root) { this.root = root; }
        int height() { return HEADER_H + children.size() * CHILD_H; }
    }

    /**
     * Build the tree only from entries the v2.12 server chose to send.
     * Orphans remain top-level rather than conjuring any hidden parent.
     */
    private List<Group> groups() {
        List<MechanicUnlockSnapshotPacket.Entry> entries = ClientMechanicUnlocks.entries();
        Map<String, MechanicUnlockSnapshotPacket.Entry> known = new LinkedHashMap<>();
        for (MechanicUnlockSnapshotPacket.Entry entry : entries) {
            // Unique rune collections are normal Rune-strip tabs, not a journal-unlock action.
            if (BalanceMechanicUnlockPolicy.UNIQUE_RUNE_COLLECTIONS.equals(entry.unlockId)) continue;
            known.put(entry.unlockId, entry);
        }
        Map<String, Group> roots = new LinkedHashMap<>();
        for (MechanicUnlockSnapshotPacket.Entry entry : entries) {
            if (!known.containsKey(entry.unlockId)) continue;
            String parent = entry.parentUnlockId;
            String id = entry.unlockId;
            int safety = 0;
            while (parent != null && !parent.isEmpty()
                    && known.containsKey(parent) && safety++ < entries.size()) {
                id = parent;
                parent = known.get(parent).parentUnlockId;
            }
            if (!roots.containsKey(id)) roots.put(id, new Group(known.get(id)));
        }
        for (MechanicUnlockSnapshotPacket.Entry entry : entries) {
            if (!known.containsKey(entry.unlockId)) continue;
            for (Map.Entry<String,Group> root : roots.entrySet()) {
                if (!entry.unlockId.equals(root.getKey()) && isDescendant(entry, root.getKey(), known)) {
                    root.getValue().children.add(entry);
                    break;
                }
            }
        }
        return new ArrayList<>(roots.values());
    }

    private static boolean isDescendant(MechanicUnlockSnapshotPacket.Entry entry, String root,
                                         Map<String,MechanicUnlockSnapshotPacket.Entry> known) {
        String parent = entry.parentUnlockId;
        int count = 0;
        while (parent != null && !parent.isEmpty() && count++ < known.size()) {
            if (root.equals(parent)) return true;
            MechanicUnlockSnapshotPacket.Entry ancestor = known.get(parent);
            if (ancestor == null) return false;
            parent = ancestor.parentUnlockId;
        }
        return false;
    }

    private int rows(List<Group> groups) {
        return (groups.size() + columns() - 1) / columns();
    }

    private int rowHeight(List<Group> groups, int row) {
        int height = HEADER_H;
        for (int c = 0; c < columns(); c++) {
            int i = row * columns() + c;
            if (i < groups.size()) height = Math.max(height, groups.get(i).height());
        }
        return height + GAP;
    }

    private int maxFirstRow(List<Group> groups) {
        return Math.max(0, rows(groups) - 1);
    }

    @Override public void initGui() {
        super.initGui();
        NetworkHandler.INSTANCE.sendToServer(new MechanicUnlockActionPacket("sync", "", 0L));
        refreshButtons();
    }

    /** Retained for the packet handler; actual entries are rendered from live snapshots. */
    public void refreshButtons() {
        buttonList.clear();
        UnifiedMainMenuChrome.installNavigation(buttonList, width, height, UiMenuSection.MECHANIC_UNLOCKS);
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled) UnifiedMainMenuChrome.handleNavigation(button.id, UiMenuSection.MECHANIC_UNLOCKS, mc);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (mouseX < left() || mouseX >= right() || mouseY < top() || mouseY > bottom()) return;
        List<Group> current = groups();
        firstRow = Math.max(0, Math.min(maxFirstRow(current), firstRow + (wheel < 0 ? 1 : -1)));
    }

    /** Only visible eligible server entries can send the existing unlock action. */
    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && mouseX >= left() && mouseX < right()
                && mouseY >= top() && mouseY < bottom()) {
            List<Group> current = groups();
            firstRow = Math.min(firstRow, maxFirstRow(current));
            int cursorY = top();
            for (int row = firstRow; row < rows(current); row++) {
                int h = rowHeight(current, row);
                if (cursorY + h > bottom()) break;
                for (int col = 0; col < columns(); col++) {
                    int i = row * columns() + col;
                    if (i >= current.size()) break;
                    int x = left() + col * (columnWidth() + GAP);
                    if (mouseX < x || mouseX >= x + columnWidth()) continue;
                    Group group = current.get(i);
                    int buttonX = x + columnWidth() - 80;
                    if (mouseX >= buttonX && mouseX < buttonX + 70) {
                        if (mouseY >= cursorY + 84 && mouseY < cursorY + 104) {
                            unlockIfEligible(group.root);
                            return;
                        }
                        for (int child = 0; child < group.children.size(); child++) {
                            int cy = cursorY + HEADER_H + child * CHILD_H;
                            if (mouseY >= cy + 6 && mouseY < cy + 26) {
                                unlockIfEligible(group.children.get(child));
                                return;
                            }
                        }
                    }
                }
                cursorY += h;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void unlockIfEligible(MechanicUnlockSnapshotPacket.Entry entry) {
        if (entry.eligible && !entry.unlocked)
            NetworkHandler.INSTANCE.sendToServer(
                    new MechanicUnlockActionPacket("unlock", entry.unlockId, NONCE.incrementAndGet()));
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        UnifiedMainMenuChrome.draw(mc, width, height, mouseX, mouseY, "РАЗБЛОКИРОВКИ");
        UnifiedMainMenuChrome.refreshNavigation(buttonList);
        int left = left(), right = right();
        List<Group> current = groups();
        firstRow = Math.min(firstRow, maxFirstRow(current));
        fontRenderer.drawString(fontRenderer.trimStringToWidth(
                "Прогресс открытия механик   |   ZBT: " + ClientMechanicUnlocks.zbtStatus(),
                Math.max(8,right-left)), left, Math.max(25,top()-20), 0xFFACB4C1);

        int cursorY = top();
        int lastShown = firstRow;
        for (int row = firstRow; row < rows(current); row++) {
            int h = rowHeight(current, row);
            if (cursorY + h > bottom()) break;
            for (int col = 0; col < columns(); col++) {
                int index = row * columns() + col;
                if (index >= current.size()) break;
                Group group = current.get(index);
                int x = left + col * (columnWidth() + GAP), w = columnWidth();
                renderGroup(x, cursorY, w, group);
            }
            lastShown = row + 1;
            cursorY += h;
        }
        String footer = current.isEmpty() ? "Нет видимых механик"
                : "Группы: " + current.size() + "   Ряды: "
                + (firstRow + 1) + "-" + lastShown + "/" + rows(current)
                + "   Колёсико мыши: прокрутка";
        fontRenderer.drawString(fontRenderer.trimStringToWidth(footer, Math.max(10,right-left)),
                left, height-25, 0xFFC9CDD2);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderGroup(int x, int y, int width, Group group) {
        UnifiedMainMenuChrome.panel(x, y, width, group.height());
        MechanicUnlockSnapshotPacket.Entry root = group.root;
        int maxText = width - 20;
        fontRenderer.drawString(fontRenderer.trimStringToWidth(localName(root), maxText),
                x+9, y+11, root.unlocked ? 0xFF8FE3A5 : 0xFFE5C887);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(
                "Требование: " + rarityName(root.rarity) + " руны", maxText),
                x+9, y+31, 0xFFB9C1CA);
        drawProgress(x+9, y+49, width-18, root.progress, root.target);
        fontRenderer.drawString(root.progress + " / " + root.target,
                x+9, y+61, 0xFFD6DCE3);
        fontRenderer.drawString(root.unlocked ? "Открыто"
                : root.eligible ? "Можно открыть" : "Заблокировано",
                x+9, y+90, root.unlocked ? 0xFF8FE3A5 : root.eligible ? 0xFFFFDF81 : 0xFF8B929C);
        drawUnlockButton(x+width-80, y+84, root);
        for (int child = 0; child < group.children.size(); child++) {
            MechanicUnlockSnapshotPacket.Entry entry = group.children.get(child);
            int cy = y + HEADER_H + child * CHILD_H;
            drawRect(x+8, cy, x+9, cy+CHILD_H-2, 0xFF526983);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(
                    "↳ " + localName(entry), Math.max(8,width-100)),
                    x+14, cy+5, entry.unlocked ? 0xFF8FE3A5 : 0xFFBAC4CF);
            fontRenderer.drawString(entry.progress + "/" + entry.target
                    + " " + rarityName(entry.rarity), x+14, cy+18, 0xFF8B929C);
            drawUnlockButton(x+width-80, cy+6, entry);
        }
    }

    private void drawUnlockButton(int x, int y, MechanicUnlockSnapshotPacket.Entry entry) {
        if (entry.unlocked) {
            fontRenderer.drawString("✓", x+34, y+5, 0xFF8FE3A5);
            return;
        }
        if (!entry.eligible) return;
        drawRect(x, y, x+70, y+20, 0xFF28583F);
        fontRenderer.drawString("Открыть", x+14, y+6, 0xFFE3F3E7);
    }

    private void drawProgress(int x, int y, int w, int progress, int target) {
        int width = Math.max(8,w);
        drawRect(x, y, x+width, y+7, 0xFF303944);
        double fraction = target <= 0 ? 0D
                : Math.max(0D, Math.min(1D, progress / (double)target));
        drawRect(x, y, x+(int)Math.round(width*fraction), y+7, 0xFF69A4DE);
    }

    private static String rarityName(String raw) {
        if ("IMPROVED".equals(raw)) return "улучшенные";
        if ("ADVANCED".equals(raw)) return "продвинутые";
        if ("MYTHIC".equals(raw)) return "мифические";
        if ("ANCIENT".equals(raw)) return "древние";
        if ("COMMON".equals(raw)) return "обычные";
        return raw;
    }

    private static String localName(MechanicUnlockSnapshotPacket.Entry entry) {
        if ("ARTS".equals(entry.unlockId)) return "Искусства и мастерство";
        if ("RUN_EXCHANGE".equals(entry.unlockId)) return "Обменник";
        if ("GOD_WORSHIP".equals(entry.unlockId)) return "Боги";
        if ("AURA_SYSTEM".equals(entry.unlockId)) return "Аура";
        if ("ACHIEVEMENTS".equals(entry.unlockId)) return "Достижения";
        if ("UNIQUE_RUNE_COLLECTIONS".equals(entry.unlockId)) return "Уникальные руны";
        return entry.displayName;
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
