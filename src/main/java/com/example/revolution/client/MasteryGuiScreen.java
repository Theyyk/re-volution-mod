package com.example.revolution.client;

import com.example.revolution.client.network.MasteryClientState;
import com.example.revolution.network.MasteryActionPacket;
import com.example.revolution.network.MasterySnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Separate Mastery/Runs screen; deliberately not a tab in the E-menu. */
public final class MasteryGuiScreen extends GuiScreen {
    private static final int PANEL_BG = 0xE018212B;
    private String selectedBranch = "";
    private String armedPreview = "";
    private String armedRunId = "";
    private boolean premiumSelected;

    private int panelWidth() { return Math.min(520, Math.max(175, width - 20)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return Math.max(8, (height - 365) / 2); }
    private int panelBottom() { return Math.min(height - 8, panelTop() + 365); }

    @Override public void initGui() { refreshButtons(); }

    public void refreshButtons() {
        buttonList.clear();
        MasterySnapshotPacket state = MasteryClientState.snapshot();
        List<MasterySnapshotPacket.BranchEntry> branches = MasteryClientState.branches();
        int x = panelLeft(), y = panelTop(), w = panelWidth(), bottom = panelBottom();
        if (state != null && state.runId.isEmpty()) {
            premiumSelected = false;
            armedPreview = "";
            armedRunId = "";
        }
        boolean unlocked = state != null && state.artsUnlocked;
        boolean active = state != null && !state.runId.isEmpty();
        boolean available = unlocked && !active && !MasteryClientState.pending();
        int branchW = Math.max(75, w / 2 - 16);
        for (int i = 0; i < branches.size() && i < 8; i++) {
            String id = branches.get(i).id;
            GuiButton button = new GuiButton(100 + i, x + 9, y + 43 + i * 21, branchW, 18,
                    (id.equals(selectedBranch) ? "§a> " : "") + branchName(id));
            button.enabled = available;
            buttonList.add(button);
        }
        int footerY = bottom - 27;
        int half = (w - 26) / 2;
        if (!active) {
            GuiButton start = new GuiButton(10, x + 9, footerY, half, 20, "Начать забег");
            start.enabled = available && !selectedBranch.isEmpty() && contains(branches, selectedBranch);
            buttonList.add(start);
        } else {
            int actionWidth = Math.max(65, (w - 30) / 3);
            GuiButton collect = new GuiButton(11, x + 9, footerY, actionWidth, 20, "Собрать");
            collect.enabled = unlocked && !MasteryClientState.pending() && !"0".equals(state.masteryPending);
            buttonList.add(collect);
            GuiButton reset = new GuiButton(12, x + 12 + actionWidth, footerY, actionWidth, 20,
                    (premiumSelected ? "reset_premium" : "reset").equals(armedPreview) ? "Подтвердить" : "Сброс забега");
            reset.enabled = unlocked && !MasteryClientState.pending();
            buttonList.add(reset);
            GuiButton mastery = new GuiButton(13, x + 15 + actionWidth * 2, footerY, actionWidth, 20,
                    (premiumSelected ? "premium" : "mastery").equals(armedPreview) ? "Подтвердить" : "Mastery Reset");
            mastery.enabled = unlocked && state.masteryResetAvailable && !MasteryClientState.pending();
            buttonList.add(mastery);
            int premiumWidth = Math.min(180, Math.max(65, w - 120));
            GuiButton premium = new GuiButton(14, x + 9, footerY - 26, premiumWidth, 20,
                    state.premiumPreserveAvailable
                            ? (premiumSelected ? "Сохранение: ВКЛ" : "Сохранение: ВЫКЛ")
                            : "Премиум недоступен");
            premium.enabled = unlocked && state.premiumPreserveAvailable && !MasteryClientState.pending();
            buttonList.add(premium);
        }
        buttonList.add(new GuiButton(20, x + w - half - 12, active ? footerY - 26 : footerY, half, 20, "Обновить"));
        // Premium selector and Refresh share the same row without overlapping.
        GuiButton refresh = buttonList.get(buttonList.size() - 1);
        if (active) {
            int premiumWidth = Math.min(180, Math.max(65, w - 120));
            refresh.width = Math.min(100, Math.max(60, w - premiumWidth - 27));
            refresh.x = x + w - refresh.width - 9;
            refresh.enabled = !MasteryClientState.pending();
        }
    }

    private static boolean contains(List<MasterySnapshotPacket.BranchEntry> list, String id) {
        for (MasterySnapshotPacket.BranchEntry branch : list)
            if (branch.id.equals(id)) return true;
        return false;
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if (MasteryClientState.pending()) return;
        List<MasterySnapshotPacket.BranchEntry> branches = MasteryClientState.branches();
        if (button.id >= 100 && button.id < 100 + branches.size()) {
            selectedBranch = branches.get(button.id - 100).id;
            armedPreview = ""; refreshButtons(); return;
        }
        MasterySnapshotPacket state = MasteryClientState.snapshot();
        if (button.id == 20) { armedPreview = ""; request("sync", "", ""); return; }
        if (state == null || !state.artsUnlocked) return;
        if (button.id == 10 && state.runId.isEmpty() && contains(branches, selectedBranch)) {
            request("start", selectedBranch, ""); return;
        }
        if (state.runId.isEmpty()) return;
        if (button.id == 11) { armedPreview = ""; request("collect", "", state.runId); }
        else if (button.id == 12) previewOrConfirm(premiumSelected ? "reset_premium" : "reset", state.runId);
        else if (button.id == 13 && state.masteryResetAvailable)
            previewOrConfirm(premiumSelected ? "premium" : "mastery", state.runId);
        else if (button.id == 14 && state.premiumPreserveAvailable) {
            premiumSelected = !premiumSelected;
            armedPreview = ""; armedRunId = "";
            refreshButtons();
        }
    }
    private void previewOrConfirm(String kind, String runId) {
        MasterySnapshotPacket state = MasteryClientState.snapshot();
        if (kind.equals(armedPreview) && runId.equals(armedRunId)
                && state != null && kind.equals(state.previewKind)) {
            armedPreview = ""; request(kind, "", runId);
        } else {
            armedPreview = kind; armedRunId = runId;
            request("preview_" + kind, "", runId);
        }
    }
    private void request(String action, String branch, String runId) {
        NetworkHandler.INSTANCE.sendToServer(
                new MasteryActionPacket(action, branch, runId, MasteryClientState.nextRequestId()));
        refreshButtons();
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = panelLeft(), y = panelTop(), w = panelWidth(), bottom = panelBottom();
        drawRect(x, y, x + w, bottom, PANEL_BG);
        drawRect(x, y, x + w, y + 2, 0xFF5E8FB3);
        drawCenteredString(fontRenderer, "ИСКУССТВА / МАСТЕРСТВО", width / 2, y + 12, 0xFFF1E2BD);
        MasterySnapshotPacket state = MasteryClientState.snapshot();
        if (state == null) {
            line("Загрузка состояния с сервера...", x + 10, y + 28, w - 20, 0xFFD9C48B);
        } else {
            line(state.artsUnlocked ? "ARTS: разблокировано" : "ARTS: заблокировано", x + 10, y + 28,
                    w - 20, state.artsUnlocked ? 0xFF89DBA4 : 0xFFE6A18B);
            int infoX = x + w / 2 + 8, infoW = w / 2 - 18;
            int row = y + 46;
            if (!state.artsUnlocked) {
                line("Требования:", infoX, row, infoW, 0xFFE9CA8D);
                line(state.unlockRequirement, infoX, row + 14, infoW, 0xFFD5DBE2);
                line("Открыть через разблокировки рун", infoX, row + 28, infoW, 0xFFB0B7C0);
            } else if (state.runId.isEmpty()) {
                line("Активного забега нет", infoX, row, infoW, 0xFFE9CA8D);
                line("Выберите направление слева.", infoX, row + 15, infoW, 0xFFD5DBE2);
                line("Запуск подтверждается сервером.", infoX, row + 30, infoW, 0xFFB0B7C0);
                if (!selectedBranch.isEmpty())
                    line("Выбрано: " + branchName(selectedBranch), infoX, row + 47, infoW, 0xFF89DBA4);
            } else {
                line("Забег: " + branchName(state.activeBranch), infoX, row, infoW, 0xFF89DBA4);
                line("Уровень: " + state.level + "  Прогресс: " + state.progress, infoX, row + 15, infoW, 0xFFD8E1E9);
                line("Фрагменты: " + state.fragments, infoX, row + 30, infoW, 0xFFD8E1E9);
                line("Mastery pending: " + state.masteryPending, infoX, row + 45, infoW, 0xFFD8E1E9);
                line("Pets pending: " + state.petPending, infoX, row + 60, infoW, 0xFFD8E1E9);
                line("Ранги: " + (state.ranks.isEmpty() ? "нет" : state.ranks), infoX, row + 75, infoW, 0xFFB8C6D1);
                line("Run ID: " + state.runId, infoX, row + 90, infoW, 0xFF82929F);
            }
            // Canonical server-authored permanent ranks for the selected or active direction.
            String rankBranch = state.runId.isEmpty() ? selectedBranch : state.activeBranch;
            int rankY = y + 164;
            if (!rankBranch.isEmpty()) {
                line("Постоянные ранги: " + branchName(rankBranch), x + 10, rankY, w - 20, 0xFFE9CA8D);
                int rowIndex = 0;
                for (MasterySnapshotPacket.RankEntry rank : state.getPermanentRanks()) {
                    if (!rankBranch.equals(rank.branch)) continue;
                    String reached = !state.runId.isEmpty() && state.ranks.contains(rank.id) ? " ✓" : "";
                    line(rank.id + " | " + rank.metaLevel + "/" + rank.metaCap
                            + " | порог " + rank.threshold + reached,
                            x + 10, rankY + 15 + rowIndex * 13, w - 20, 0xFFD8E1E9);
                    rowIndex++;
                }
            }
            // Keep server feedback below the final permanent-rank row.
            // Keep rank rows (ending at y+214), selection (y+221) and
            // the preview (y+240..301) separate from footer buttons (y+312).
            int foot = bottom - 125;
            if (!state.runId.isEmpty() && state.premiumPreserveAvailable) {
                line(premiumSelected
                        ? "Сохранение: " + state.premiumCrystalCost + " крист. | руны: "
                            + state.premiumPreserveRunes + " | питомцы: " + state.premiumPreservePets
                        : "Сохранение: ВЫКЛ (стандартный сброс)",
                        x + 9, y + 221, w - 18, 0xFFE9CA8D);
            }
            if (!state.previewDetails.isEmpty() && state.previewKind.equals(armedPreview)) {
                line("ПРЕДПРОСМОТР: нажмите «Подтвердить» для выполнения", x + 9, foot, w - 18, 0xFFFFCC77);
                String[] details = state.previewDetails.split(" \\| ");
                for (int i = 0; i < details.length && i < 4; i++)
                    line(details[i], x + 12, foot + 13 + 13 * i, w - 24, 0xFFFFB18F);
            } else if (!state.response.isEmpty()) {
                line("Сервер: " + resultName(state.response), x + 9, foot, w - 18, 0xFFFFCA8B);
                if (state.runId.isEmpty() && resetApplied(state.response))
                    line("Забег завершён. Выберите новое направление слева.", x + 9,
                            foot + 14, w - 18, 0xFF89DBA4);
            }
            if (MasteryClientState.pending())
                line("Запрос к серверу...", x + 9, foot - 12, w - 18, 0xFFCCDBE5);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void line(String value, int x, int y, int maxWidth, int color) {
        if (y > panelBottom() - 3) return;
        fontRenderer.drawStringWithShadow(fontRenderer.trimStringToWidth(value, Math.max(10, maxWidth)),
                x, y, color);
    }
    private static boolean resetApplied(String value) {
        return "RESET_APPLIED".equals(value) || "RESET_PREMIUM_APPLIED".equals(value)
                || "MASTERY_APPLIED".equals(value) || "PREMIUM_APPLIED".equals(value);
    }
    private static String resultName(String value) {
        if ("RUN_STARTED".equals(value)) return "Забег запущен";
        if ("ARTS_LOCKED".equals(value)) return "Сначала разблокируйте ARTS";
        if ("NO_ACTIVE_RUN".equals(value)) return "Нет активного забега";
        if ("STALE_RUN".equals(value)) return "Состояние забега устарело";
        if ("REPLAY_REJECTED".equals(value)) return "Повторный запрос отклонён";
        if ("RESET_APPLIED".equals(value)) return "Забег завершён";
        if ("RESET_PREMIUM_APPLIED".equals(value)) return "Премиальный сброс выполнен";
        if ("RESET_PREMIUM_INSUFFICIENT_CRYSTALS".equals(value)) return "Недостаточно кристаллов";
        if ("RESET_PREMIUM_OPTION_UNAVAILABLE".equals(value)) return "Премиальное сохранение недоступно";
        if ("MASTERY_APPLIED".equals(value) || "PREMIUM_APPLIED".equals(value)) return "Mastery Reset применён";
        if (value.startsWith("COLLECTED:")) return "Собрано фрагментов: " + value.substring(10);
        return value;
    }
    private static String branchName(String id) {
        switch (id) {
            case "CLICK": return "Урон клика";
            case "POISON": return "Яд";
            case "COINS": return "Монеты";
            case "RUNE_RESOURCES": return "Ресурсы рун";
            case "ALL_DAMAGE": return "Общий урон";
            default: return id;
        }
    }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void onGuiClosed() { MasteryClientState.cancelOpen(); }
}
