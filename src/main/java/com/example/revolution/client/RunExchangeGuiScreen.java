package com.example.revolution.client;

import com.example.revolution.client.network.RunExchangeClientState;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RunExchangeActionPacket;
import com.example.revolution.network.RunExchangeSnapshotPacket;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Minimal Unified UI shell over the pre-existing RunExchangeService. */
public final class RunExchangeGuiScreen extends GuiScreen {
    private int page;
    private String armedOffer = "";
    private String armedRun = "";

    private int panelWidth() { return Math.min(550, Math.max(1, width - 16)); }
    private int panelHeight() { return Math.min(330, Math.max(1, height - 12)); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int rowsPerPage() { return Math.max(1, (panelHeight() - 111) / 31); }

    private List<RunExchangeSnapshotPacket.OfferEntry> offers() {
        RunExchangeSnapshotPacket state = RunExchangeClientState.snapshot();
        return state == null ? Collections.<RunExchangeSnapshotPacket.OfferEntry>emptyList() : state.getOffers();
    }

    @Override public void initGui() { refreshButtons(); }

    public void refreshButtons() {
        buttonList.clear();
        List<RunExchangeSnapshotPacket.OfferEntry> offers = offers();
        int rows = rowsPerPage();
        page = Math.min(page, Math.max(0, (offers.size() - 1) / rows));
        int first = page * rows;
        int x = left(), y = top(), w = panelWidth(), h = panelHeight();
        RunExchangeSnapshotPacket state = RunExchangeClientState.snapshot();
        boolean runActive = state != null && !state.runId.isEmpty();
        for (int i = 0; i < rows && first + i < offers.size(); i++) {
            RunExchangeSnapshotPacket.OfferEntry entry = offers.get(first + i);
            GuiButton buy = new GuiControls.PanelButton(100 + i, x + w - 99, y + 55 + i * 31,
                    91, 21, entry.offerId.equals(armedOffer) && state.runId.equals(armedRun)
                        ? "Подтвердить" : "Купить");
            buy.enabled = entry.available && runActive && !RunExchangeClientState.pending();
            buttonList.add(buy);
        }
        GuiButton prev = new GuiControls.PanelButton(20, x + 9, y + h - 51, 32, 19, "<");
        GuiButton next = new GuiControls.PanelButton(21, x + 46, y + h - 51, 32, 19, ">");
        prev.enabled = page > 0;
        next.enabled = (page + 1) * rows < offers.size();
        buttonList.add(prev);
        buttonList.add(next);
        GuiButton sync = new GuiControls.PanelButton(22, x + 9, y + h - 26, (w - 27) / 2, 20, "Обновить");
        sync.enabled = !RunExchangeClientState.pending();
        buttonList.add(sync);
        buttonList.add(new GuiControls.PanelButton(23, x + 18 + (w - 27) / 2, y + h - 26,
                (w - 27) / 2, 20, "Назад к мастерству"));
    }

    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 23) { mc.displayGuiScreen(new MasteryGuiScreen()); return; }
        if (button.id == 20) { page = Math.max(0, page - 1); clearArmed(); refreshButtons(); return; }
        if (button.id == 21) { page++; clearArmed(); refreshButtons(); return; }
        if (button.id == 22) { clearArmed(); request("sync", ""); return; }
        int row = button.id - 100;
        List<RunExchangeSnapshotPacket.OfferEntry> offers = offers();
        int position = page * rowsPerPage() + row;
        if (row < 0 || row >= rowsPerPage() || position >= offers.size()) return;
        RunExchangeSnapshotPacket state = RunExchangeClientState.snapshot();
        if (state == null || !state.unlocked || state.runId.isEmpty() || RunExchangeClientState.pending()) return;
        RunExchangeSnapshotPacket.OfferEntry offer = offers.get(position);
        if (!offer.available) return;
        if (offer.offerId.equals(armedOffer) && state.runId.equals(armedRun)) {
            clearArmed();
            request("purchase", offer.offerId);
        } else {
            armedOffer = offer.offerId;
            armedRun = state.runId;
            refreshButtons();
        }
    }

    private void request(String action, String offerId) {
        RunExchangeSnapshotPacket state = RunExchangeClientState.snapshot();
        String run = state == null ? "" : state.runId;
        NetworkHandler.INSTANCE.sendToServer(new RunExchangeActionPacket(
                action, run, offerId, RunExchangeClientState.nextRequestId()));
        refreshButtons();
    }

    private void clearArmed() { armedOffer = ""; armedRun = ""; }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = left(), y = top(), w = panelWidth(), h = panelHeight();
        drawRect(x, y, x + w, y + h, 0xE018212B);
        drawRect(x, y, x + w, y + 2, 0xFF5E8FB3);
        drawCenteredString(fontRenderer, "ОБМЕН ЗАБЕГА", width / 2, y + 9, 0xFFF1E2BD);
        RunExchangeSnapshotPacket state = RunExchangeClientState.snapshot();
        if (state == null) {
            line("Ожидание состояния сервера...", x + 9, y + 27, w - 18, 0xFFD9C48B);
        } else {
            String status = !state.unlocked ? "Система заблокирована"
                    : state.runId.isEmpty() ? "Требуется активный забег" : "Активный забег";
            line(status + "  |  Фрагменты: " + state.fragments + "  |  Кристаллы: " + state.crystals,
                    x + 9, y + 26, w - 18, 0xFFD8E1E9);
            if (!state.response.isEmpty())
                line("Сервер: " + state.response, x + 9, y + 40, w - 18,
                        "APPLIED".equals(state.response) ? 0xFF89DBA4 : 0xFFE9CA8D);
        }
        List<RunExchangeSnapshotPacket.OfferEntry> offers = offers();
        int first = page * rowsPerPage();
        for (int i = 0; i < rowsPerPage() && first + i < offers.size(); i++) {
            RunExchangeSnapshotPacket.OfferEntry e = offers.get(first + i);
            int ry = y + 54 + i * 31;
            drawRect(x + 7, ry, x + w - 7, ry + 28, 0x990B1119);
            line(e.offerId + " — " + e.target, x + 13, ry + 3, w - 125,
                    e.available ? 0xFFE5EAF0 : 0xFFACB5BD);
            String detail = e.available || e.unavailableReason.isEmpty()
                    ? e.cost + " " + e.paymentCurrency + " → " + e.reward
                    : e.unavailableReason;
            line(detail, x + 13, ry + 15, w - 125,
                    e.available ? 0xFF89DBA4 : 0xFFE9CA8D);
        }
        if (state != null && !state.runId.equals(armedRun) && !armedRun.isEmpty()) clearArmed();
        line("Страница " + (page + 1) + " / " + Math.max(1, (offers.size() + rowsPerPage() - 1) / rowsPerPage()),
                x + 89, y + h - 45, w - 100, 0xFFADB7C3);
        if (!armedOffer.isEmpty())
            line("Повторное нажатие подтверждает покупку: " + armedOffer,
                    x + 9, y + h - 66, w - 18, 0xFFE9CA8D);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void line(String text, int x, int y, int maxWidth, int color) {
        fontRenderer.drawStringWithShadow(fontRenderer.trimStringToWidth(text, Math.max(1, maxWidth)), x, y, color);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
