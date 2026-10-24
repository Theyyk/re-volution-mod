package com.example.revolution.client;

import com.example.revolution.network.FastAutoTogglePacket;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.StageTravelMenuPacket;
import com.example.revolution.network.StageTravelTeleportRequestPacket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Player navigation menu for already-unlocked configured mob stages. */
public final class StageTravelGuiScreen extends GuiScreen {
    private static final int ITEM_BASE = 100;
    private static final int BACK = 1;
    private static final int PREVIOUS_PAGE = 2;
    private static final int NEXT_PAGE = 3;
    private static final int CLOSE = 4;
    private static final int FAST_AUTO = 5;
    private static final int ITEMS_PER_PAGE = 6;

    private final List<StageTravelMenuPacket.Entry> entries = new ArrayList<>();
    private final List<StageTravelMenuPacket.Entry> visibleStageEntries = new ArrayList<>();
    private final List<Integer> visibleLocations = new ArrayList<>();
    private int selectedLocation = -1;
    private int page = 0;
    private boolean fastAutoEnabled;

    public StageTravelGuiScreen(List<StageTravelMenuPacket.Entry> entries, boolean fastAutoEnabled) {
        if (entries != null) this.entries.addAll(entries);
        this.fastAutoEnabled = fastAutoEnabled;
        Collections.sort(this.entries, Comparator
                .comparingInt(StageTravelMenuPacket.Entry::getWorldLocationId)
                .thenComparingInt(StageTravelMenuPacket.Entry::getMobSlotId));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        visibleStageEntries.clear();
        visibleLocations.clear();

        if (selectedLocation < 0) buildLocationButtons();
        else buildStageButtons();

        int bottom = height - 28;
        buttonList.add(new GuiButton(CLOSE, width / 2 + 65, bottom, 90, 20, "Закрыть"));
        if (selectedLocation >= 0) {
            buttonList.add(new GuiButton(BACK, width / 2 - 155, bottom, 90, 20, "Назад"));
        } else {
            buttonList.add(new GuiButton(FAST_AUTO, width / 2 - 155, bottom, 130, 20,
                    "Fast-auto: " + (fastAutoEnabled ? "ON" : "OFF")));
        }
    }

    private void buildLocationButtons() {
        Set<Integer> unique = new LinkedHashSet<>();
        for (StageTravelMenuPacket.Entry entry : entries) unique.add(entry.getWorldLocationId());
        List<Integer> locations = new ArrayList<>(unique);
        int pages = pageCount(locations.size());
        if (page >= pages) page = Math.max(0, pages - 1);
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(locations.size(), from + ITEMS_PER_PAGE);
        for (int i = from; i < to; i++) {
            int worldLocationId = locations.get(i);
            visibleLocations.add(worldLocationId);
            buttonList.add(new GuiButton(ITEM_BASE + visibleLocations.size() - 1,
                    width / 2 - 110, 48 + (visibleLocations.size() - 1) * 24,
                    220, 20, "Локация " + worldLocationId));
        }
        addPageButtons(pages);
    }

    private void buildStageButtons() {
        List<StageTravelMenuPacket.Entry> stages = new ArrayList<>();
        for (StageTravelMenuPacket.Entry entry : entries) {
            if (entry.getWorldLocationId() == selectedLocation) stages.add(entry);
        }
        int pages = pageCount(stages.size());
        if (page >= pages) page = Math.max(0, pages - 1);
        int from = page * ITEMS_PER_PAGE;
        int to = Math.min(stages.size(), from + ITEMS_PER_PAGE);
        for (int i = from; i < to; i++) {
            StageTravelMenuPacket.Entry entry = stages.get(i);
            visibleStageEntries.add(entry);
            String label = "Моб " + entry.getMobSlotId();
            if (!entry.isUnlocked()) label += " - закрыт";
            else if (!entry.hasTransitionPoint()) label += " - нет точки";
            GuiButton button = new GuiButton(ITEM_BASE + visibleStageEntries.size() - 1,
                    width / 2 - 110, 48 + (visibleStageEntries.size() - 1) * 24,
                    220, 20, label);
            button.enabled = entry.isUnlocked() && entry.hasTransitionPoint();
            buttonList.add(button);
        }
        addPageButtons(pages);
    }

    private void addPageButtons(int pages) {
        if (pages <= 1) return;
        GuiButton previous = new GuiButton(PREVIOUS_PAGE, width / 2 - 110, height - 52, 100, 20, "< Страница");
        previous.enabled = page > 0;
        GuiButton next = new GuiButton(NEXT_PAGE, width / 2 + 10, height - 52, 100, 20, "Страница >");
        next.enabled = page + 1 < pages;
        buttonList.add(previous);
        buttonList.add(next);
    }

    private int pageCount(int size) {
        return Math.max(1, (size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == CLOSE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (button.id == FAST_AUTO) {
            fastAutoEnabled = !fastAutoEnabled;
            NetworkHandler.INSTANCE.sendToServer(new FastAutoTogglePacket(fastAutoEnabled));
            initGui();
            return;
        }
        if (button.id == BACK) {
            selectedLocation = -1;
            page = 0;
            initGui();
            return;
        }
        if (button.id == PREVIOUS_PAGE) {
            page--;
            initGui();
            return;
        }
        if (button.id == NEXT_PAGE) {
            page++;
            initGui();
            return;
        }
        int index = button.id - ITEM_BASE;
        if (selectedLocation < 0) {
            if (index >= 0 && index < visibleLocations.size()) {
                selectedLocation = visibleLocations.get(index);
                page = 0;
                initGui();
            }
            return;
        }
        if (index >= 0 && index < visibleStageEntries.size()) {
            StageTravelMenuPacket.Entry entry = visibleStageEntries.get(index);
            NetworkHandler.INSTANCE.sendToServer(new StageTravelTeleportRequestPacket(
                    entry.getWorldLocationId(), entry.getMobSlotId()));
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        String title = selectedLocation < 0 ? "Телепорт: выберите локацию" : "Локация " + selectedLocation + ": выберите моба";
        drawCenteredString(fontRenderer, title, width / 2, 22, 0xFFFFFF);
        if (entries.isEmpty()) {
            drawCenteredString(fontRenderer, "Нет настроенных стадий", width / 2, 60, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
