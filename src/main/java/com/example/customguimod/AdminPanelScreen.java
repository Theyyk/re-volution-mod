package com.example.customguimod;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AdminPanelScreen extends GuiScreen {
    private static final int W = 360, H = 20, PAGE = 6;
    private static final int MOB_HP_FIELD_W = 119, MOB_HP_UNIT_W = 52;
    private final List<String> players = new ArrayList<>(), resources = new ArrayList<>(), mobs = new ArrayList<>(), balances = new ArrayList<>();
    private int tab, playerIndex, resourceIndex, mobIndex, mobTypeIndex, mobHpUnitIndex;
    private int playerPage, resourcePage, mobPage, mobHpUnitPage;
    private boolean playerDropdown, resourceDropdown, mobDropdown, mobTypeDropdown, mobHpUnitDropdown;
    private int confirmAction = -1;
    private long confirmUntil;
    private GuiTextField amountField, mobNameField, mobHpField, mobRewardField;
    private String amountText = "1000", mobNameText = "TestMob", mobHpText = "1000", mobRewardText = "100";

    public AdminPanelScreen(AdminPanelStatePacket state) { readState(state); }

    void applyState(AdminPanelStatePacket state) {
        snapshotInputs();
        readState(state);
        if (mc != null) initGui();
    }

    private void readState(AdminPanelStatePacket state) {
        String p = playerUuid(), r = resource(), m = mobName();
        players.clear(); players.addAll(state.getPlayers());
        resources.clear(); resources.addAll(state.getResources());
        mobs.clear(); mobs.addAll(state.getMobs());
        balances.clear(); balances.addAll(state.getBalances());
        playerIndex = findByFirstColumn(players, p);
        resourceIndex = findExact(resources, r);
        mobIndex = findByFirstColumn(mobs, m);
        playerPage = clampPage(playerPage, players.size());
        resourcePage = clampPage(resourcePage, resources.size());
        mobPage = clampPage(mobPage, mobs.size());
        mobHpUnitPage = clampPage(mobHpUnitPage, LargeNumberValue.unitCount());
    }

    @Override public void initGui() {
        snapshotInputs();
        buttonList.clear();
        amountField = mobNameField = mobHpField = mobRewardField = null;

        int x = width / 2 - W / 2;
        int y = panelY();
        int tabsY = tabsY(y);
        int refreshY = tabsY + 26;
        int cy = refreshY + 56;

        buttonList.add(btn(1, x, y, W, "Игрок: " + playerName() + (players.size() > 1 ? "  v" : "")));
        buttonList.add(tabBtn(2, x, tabsY, 116, "РУНЫ", tab == 0));
        buttonList.add(tabBtn(3, x + 122, tabsY, 116, "РЕСУРСЫ", tab == 1));
        buttonList.add(tabBtn(4, x + 244, tabsY, 116, "МОБЫ", tab == 2));
        buttonList.add(btn(5, x, refreshY, W, "Обновить данные"));

        if (tab == 0) buildRunes(x, cy);
        else if (tab == 1) buildResources(x, cy);
        else buildMobs(x, cy);

        buildDropdown(x, y, cy);
    }

    private int panelY() { return Math.max(8, Math.min(height / 2 - 160, Math.max(8, height - 318))); }
    private int tabsY(int y) { return y + 26 + (playerDropdown ? dropdownHeight(players.size()) : 0); }
    private int dropdownHeight(int size) {
        int rows = Math.min(PAGE, Math.max(0, size));
        int pages = Math.max(1, (size + PAGE - 1) / PAGE);
        return rows * 22 + (pages > 1 ? 22 : 0);
    }
    private int resourceFormY(int y) { return y + 44 + (resourceDropdown ? dropdownHeight(resources.size()) : 0); }
    private int mobTopDropdownHeight() {
        if (resourceDropdown) return dropdownHeight(resources.size());
        if (mobTypeDropdown) return 44;
        return 0;
    }
    private int mobNameY(int y) { return y + 44 + mobTopDropdownHeight(); }
    private int mobHpY(int y) { return mobNameY(y) + 40; }
    private int mobCreateY(int y) { return mobHpY(y) + 26; }
    private int mobSelectorY(int y) { return mobCreateY(y) + 28; }
    private int mobActionsY(int y) { return mobSelectorY(y) + 26 + (mobDropdown ? dropdownHeight(mobs.size()) : 0); }
    private int mobDetailsY(int y) { return mobActionsY(y) + 27; }

    private GuiControls.PanelButton btn(int id, int x, int y, int w, String text) { return btn(id, x, y, w, text, false); }
    private GuiControls.PanelButton btn(int id, int x, int y, int w, String text, boolean selected) {
        GuiControls.PanelButton b = new GuiControls.PanelButton(id, x, y, w, H, text);
        b.setSelected(selected);
        return b;
    }
    private GuiControls.PanelButton tabBtn(int id, int x, int y, int w, String text, boolean selected) { return btn(id, x, y, w, text, selected); }

    private void buildRunes(int x, int y) {
        boolean clear = confirming(20);
        buttonList.add(btn(20, x, y, 175, clear ? "Повторить: очистить" : "Очистить все", clear));
        buttonList.add(btn(21, x + 185, y, 175, "Все NORMAL"));
        buttonList.add(btn(22, x, y + 26, 175, "Все SILVER"));
        buttonList.add(btn(23, x + 185, y + 26, 175, "Все GOLD"));
        boolean resetEnhancements = confirming(24);
        buttonList.add(btn(24, x, y + 52, W,
                resetEnhancements ? "Повторить: сбросить усиления" : "Сбросить усиления рун",
                resetEnhancements));
    }

    private void buildResources(int x, int y) {
        String balance = playerUuid().isEmpty() || resources.isEmpty() ? "-" : String.valueOf(selectedBalance());
        buttonList.add(btn(30, x, y, W, "Ресурс: " + displayResource(resource()) + " | Баланс: " + balance + "  v"));
        int formY = resourceFormY(y);
        amountField = field(31, x, formY, W, amountText);
        buttonList.add(btn(32, x, formY + 26, 114, "Добавить"));
        buttonList.add(btn(33, x + 123, formY + 26, 114, "Установить"));
        buttonList.add(btn(34, x + 246, formY + 26, 114, "Обнулить"));
        boolean c = confirming(35);
        buttonList.add(btn(35, x, formY + 52, W, c ? "Повторить: обнулить все" : "Обнулить все ресурсы", c));
    }

    private void buildMobs(int x, int y) {
        buttonList.add(btn(40, x, y, 175, "Тип: " + (mobTypeIndex == 0 ? "zombie" : "skeleton") + "  v"));
        buttonList.add(btn(41, x + 185, y, 175, "Награда: " + displayResource(resource()) + "  v"));

        int nameY = mobNameY(y);
        int hpY = mobHpY(y);
        int createY = mobCreateY(y);
        int selectorY = mobSelectorY(y);
        int actionsY = mobActionsY(y);

        mobNameField = field(42, x, nameY, W, mobNameText);
        mobHpField = field(43, x, hpY, MOB_HP_FIELD_W, mobHpText);
        buttonList.add(btn(49, x + MOB_HP_FIELD_W + 4, hpY, MOB_HP_UNIT_W, unitLabel(mobHpUnitIndex) + "  v", mobHpUnitDropdown));
        mobRewardField = field(44, x + 185, hpY, 175, mobRewardText);
        buttonList.add(btn(45, x, createY, W, "Создать у выбранного игрока"));
        buttonList.add(btn(46, x, selectorY, W, "Моб: " + mobLabel() + (mobs.size() > 1 ? "  v" : "")));
        buttonList.add(btn(47, x, actionsY, 175, "Установить спавн"));
        boolean c = confirming(48);
        buttonList.add(btn(48, x + 185, actionsY, 175, c ? "Повторить: удалить" : "Удалить", c));
    }

    private GuiTextField field(int id, int x, int y, int w, String text) {
        GuiTextField f = new GuiTextField(id, fontRenderer, x, y, w, H);
        f.setMaxStringLength(256);
        f.setText(text);
        f.setTextColor(GuiTheme.TEXT_COLOR);
        f.setDisabledTextColour(GuiTheme.DISABLED_TEXT_COLOR);
        f.setEnableBackgroundDrawing(false);
        return f;
    }

    private void buildDropdown(int x, int y, int cy) {
        if (playerDropdown) addPlayerDropdown(x, y + 22, W);
        else if (resourceDropdown) {
            if (tab == 1) addResourceDropdown(x, cy + 22, W);
            else addResourceDropdown(x + 185, cy + 22, 175);
        } else if (mobTypeDropdown) {
            buttonList.add(btn(4000, x, cy + 22, 175, "zombie", mobTypeIndex == 0));
            buttonList.add(btn(4001, x, cy + 44, 175, "skeleton", mobTypeIndex == 1));
        } else if (mobHpUnitDropdown) {
            addHpUnitDropdown(x + MOB_HP_FIELD_W + 4, mobHpY(cy) + 22, MOB_HP_UNIT_W);
        } else if (mobDropdown) addMobDropdown(x, mobSelectorY(cy) + 22, W);
    }

    private void addPlayerDropdown(int x, int y, int w) {
        int start = playerPage * PAGE, end = Math.min(players.size(), start + PAGE), row = 0;
        for (int i = start; i < end; i++) buttonList.add(btn(1000 + i, x, y + row++ * 22, w, split(players.get(i), 1), i == playerIndex));
        addPageButtons(x, y + row * 22, w, 5001, 5002, playerPage, players.size());
    }
    private void addResourceDropdown(int x, int y, int w) {
        int start = resourcePage * PAGE, end = Math.min(resources.size(), start + PAGE), row = 0;
        for (int i = start; i < end; i++) buttonList.add(btn(2000 + i, x, y + row++ * 22, w, displayResource(resources.get(i)), i == resourceIndex));
        addPageButtons(x, y + row * 22, w, 5011, 5012, resourcePage, resources.size());
    }
    private void addMobDropdown(int x, int y, int w) {
        int start = mobPage * PAGE, end = Math.min(mobs.size(), start + PAGE), row = 0;
        for (int i = start; i < end; i++) {
            String v = mobs.get(i);
            buttonList.add(btn(3000 + i, x, y + row++ * 22, w,
                    split(v, 0) + " | " + split(v, 1) + " | HP " + compactHp(split(v, 2)), i == mobIndex));
        }
        addPageButtons(x, y + row * 22, w, 5021, 5022, mobPage, mobs.size());
    }
    private void addHpUnitDropdown(int x, int y, int w) {
        int size = LargeNumberValue.unitCount();
        int start = mobHpUnitPage * PAGE, end = Math.min(size, start + PAGE), row = 0;
        for (int i = start; i < end; i++) {
            buttonList.add(btn(6000 + i, x, y + row++ * 22, w, unitLabel(i), i == mobHpUnitIndex));
        }
        int pages = Math.max(1, (size + PAGE - 1) / PAGE);
        if (pages > 1) {
            GuiButton prev = btn(5031, x, y + row * 22, (w - 2) / 2, "<");
            GuiButton next = btn(5032, x + (w + 2) / 2, y + row * 22, (w - 2) / 2, ">");
            prev.enabled = mobHpUnitPage > 0;
            next.enabled = mobHpUnitPage + 1 < pages;
            buttonList.add(prev);
            buttonList.add(next);
        }
    }
    private void addPageButtons(int x, int y, int w, int prevId, int nextId, int page, int size) {
        int pages = Math.max(1, (size + PAGE - 1) / PAGE);
        if (pages <= 1) return;
        GuiButton prev = btn(prevId, x, y, (w - 4) / 2, "< Назад");
        GuiButton next = btn(nextId, x + (w + 4) / 2, y, (w - 4) / 2, "Далее >");
        prev.enabled = page > 0;
        next.enabled = page + 1 < pages;
        buttonList.add(prev); buttonList.add(next);
    }

    @Override protected void actionPerformed(GuiButton b) throws IOException {
        snapshotInputs();
        if (b.id >= 1000 && b.id < 2000) { playerIndex = b.id - 1000; playerDropdown = false; initGui(); return; }
        if (b.id >= 2000 && b.id < 3000) { resourceIndex = b.id - 2000; resourceDropdown = false; initGui(); return; }
        if (b.id >= 3000 && b.id < 4000) { mobIndex = b.id - 3000; mobDropdown = false; initGui(); return; }
        if (b.id == 4000 || b.id == 4001) { mobTypeIndex = b.id - 4000; mobTypeDropdown = false; initGui(); return; }
        if (b.id >= 6000 && b.id < 7000) {
            int index = b.id - 6000;
            if (index < LargeNumberValue.unitCount()) mobHpUnitIndex = index;
            mobHpUnitDropdown = false;
            initGui();
            return;
        }
        if (pageButton(b.id)) return;
        if (b.id == 1) { toggle(0); return; }
        if (b.id >= 2 && b.id <= 4) { tab = b.id - 2; closeDropdowns(); clearConfirm(); initGui(); return; }
        if (b.id == 5) { closeDropdowns(); send("refresh"); return; }
        if (b.id == 20) { if (!confirm(20)) return; send("runes_clear\t" + playerUuid()); }
        else if (b.id >= 21 && b.id <= 23) { clearConfirm(); send("runes_fill\t" + playerUuid() + "\t" + (b.id - 20)); }
        else if (b.id == 24) { if (!confirm(24)) return; send("runes_reset_enhancements\t" + playerUuid()); }
        else if (b.id == 30 || b.id == 41) { toggle(1); return; }
        else if (b.id == 32) { clearConfirm(); resourceAction("resource_add"); }
        else if (b.id == 33) { clearConfirm(); resourceAction("resource_set"); }
        else if (b.id == 34) { clearConfirm(); send("resource_zero\t" + playerUuid() + "\t" + resource()); }
        else if (b.id == 35) { if (!confirm(35)) return; send("resource_zero_all\t" + playerUuid()); }
        else if (b.id == 40) { toggle(2); return; }
        else if (b.id == 49) { toggle(4); return; }
        else if (b.id == 45) { clearConfirm(); createMob(); }
        else if (b.id == 46) { toggle(3); return; }
        else if (b.id == 47 && !mobs.isEmpty()) { clearConfirm(); send("mob_setspawn\t" + mobName() + "\t" + playerUuid()); }
        else if (b.id == 48 && !mobs.isEmpty()) { if (!confirm(48)) return; send("mob_delete\t" + mobName()); }
    }

    private boolean pageButton(int id) {
        if (id == 5001) playerPage = Math.max(0, playerPage - 1);
        else if (id == 5002) playerPage++;
        else if (id == 5011) resourcePage = Math.max(0, resourcePage - 1);
        else if (id == 5012) resourcePage++;
        else if (id == 5021) mobPage = Math.max(0, mobPage - 1);
        else if (id == 5022) mobPage++;
        else if (id == 5031) mobHpUnitPage = Math.max(0, mobHpUnitPage - 1);
        else if (id == 5032) mobHpUnitPage++;
        else return false;
        initGui(); return true;
    }
    private void toggle(int kind) {
        boolean p = kind == 0 && !playerDropdown;
        boolean r = kind == 1 && !resourceDropdown;
        boolean t = kind == 2 && !mobTypeDropdown;
        boolean m = kind == 3 && !mobDropdown;
        boolean u = kind == 4 && !mobHpUnitDropdown;
        closeDropdowns();
        playerDropdown = p;
        resourceDropdown = r;
        mobTypeDropdown = t;
        mobDropdown = m;
        mobHpUnitDropdown = u;
        if (u) mobHpUnitPage = mobHpUnitIndex / PAGE;
        initGui();
    }
    private void closeDropdowns() {
        playerDropdown = resourceDropdown = mobDropdown = mobTypeDropdown = mobHpUnitDropdown = false;
    }
    private boolean confirm(int id) {
        long now = System.currentTimeMillis();
        if (confirmAction == id && now <= confirmUntil) { clearConfirm(); return true; }
        confirmAction = id; confirmUntil = now + 4000L; initGui(); return false;
    }
    private boolean confirming(int id) {
        if (confirmAction == id && System.currentTimeMillis() <= confirmUntil) return true;
        if (System.currentTimeMillis() > confirmUntil) confirmAction = -1;
        return false;
    }
    private void clearConfirm() { confirmAction = -1; confirmUntil = 0L; }
    private void resourceAction(String action) { send(action + "\t" + playerUuid() + "\t" + resource() + "\t" + number(amountText)); }
    private void createMob() {
        int reward = number(mobRewardText);
        String name = mobNameText.trim();
        BigDecimal hp;
        try { hp = LargeNumberValue.parseWithUnit(mobHpText, mobHpUnitIndex); }
        catch (RuntimeException e) { return; }
        if (name.isEmpty() || hp.signum() <= 0 || reward <= 0) return;
        send("mob_create\t" + name + "\t" + (mobTypeIndex == 0 ? "zombie" : "skeleton") + "\t" + resource()
                + "\t" + reward + "\t" + LargeNumberValue.canonical(hp) + "\t" + playerUuid());
    }
    private void send(String action) {
        if (playerUuid().isEmpty() && !"refresh".equals(action)) return;
        NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket(action));
    }

    @Override protected void keyTyped(char c, int key) throws IOException {
        if (amountField != null && amountField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobNameField != null && mobNameField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobHpField != null && mobHpField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobRewardField != null && mobRewardField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        super.keyTyped(c, key);
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (amountField != null) amountField.mouseClicked(x, y, button);
        if (mobNameField != null) mobNameField.mouseClicked(x, y, button);
        if (mobHpField != null) mobHpField.mouseClicked(x, y, button);
        if (mobRewardField != null) mobRewardField.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(0, 0, width, height, GuiTheme.OVERLAY_COLOR);

        int x = width / 2 - W / 2;
        int y = panelY();
        int tabsY = tabsY(y);
        int cy = tabsY + 82;
        boolean details = height >= 340;

        int bottom;
        if (tab == 0) bottom = cy + 88;
        else if (tab == 1) bottom = resourceFormY(cy) + 84;
        else bottom = mobActionsY(cy) + (details ? 64 : 34);

        GuiDrawing.drawSurface(x - 12, y - 28, W + 24, bottom - y + 28, GuiTheme.PANEL_BG_COLOR, GuiTheme.PANEL_BORDER_COLOR);
        FontRenderer body = GuiFontRenderer.get(mc), heading = GuiFontRenderer.heading(mc);
        drawCenteredString(heading, "ADMIN / TEST PANEL", width / 2, y - 20, GuiTheme.CRYSTAL_HIGHLIGHT_COLOR);
        drawRect(x, y - 5, x + W, y - 4, GuiTheme.PANEL_LINE_COLOR);

        int summaryY = tabsY + 53;
        if (!players.isEmpty()) {
            body.drawString("Цель: " + playerName() + " | dim " + playerColumn(2) + " | XYZ " + playerColumn(3) + " / " + playerColumn(4) + " / " + playerColumn(5), x, summaryY, GuiTheme.HEADING_TEXT_COLOR, false);
            body.drawString("Руны: " + playerColumn(6) + "/26 | NORMAL " + playerColumn(7) + " | SILVER " + playerColumn(8) + " | GOLD " + playerColumn(9), x, summaryY + 12, GuiTheme.MUTED_TEXT_COLOR, false);
        } else body.drawString("Нет доступных игроков онлайн", x, summaryY + 6, 0xFFFF7777, false);

        if (tab == 1) {
            body.drawString("Количество:", x, resourceFormY(cy) - 15, GuiTheme.HEADING_TEXT_COLOR, false);
        } else if (tab == 2) {
            int nameY = mobNameY(cy), hpY = mobHpY(cy);
            body.drawString("Имя моба:", x, nameY - 15, GuiTheme.HEADING_TEXT_COLOR, false);
            body.drawString("HP:", x, hpY - 15, GuiTheme.HEADING_TEXT_COLOR, false);
            body.drawString("Награда:", x + 185, hpY - 15, GuiTheme.HEADING_TEXT_COLOR, false);
            if (details && !mobs.isEmpty()) {
                int detailsY = mobDetailsY(cy);
                body.drawString("Выбран: " + mobName() + " | " + split(mobs.get(mobIndex), 1) + " | HP "
                        + compactHp(split(mobs.get(mobIndex), 2)) + " | " + displayResource(split(mobs.get(mobIndex), 3))
                        + " x" + split(mobs.get(mobIndex), 4), x, detailsY, GuiTheme.MUTED_TEXT_COLOR, false);
                body.drawString("Spawn: " + split(mobs.get(mobIndex), 5) + " / " + split(mobs.get(mobIndex), 6) + " / "
                        + split(mobs.get(mobIndex), 7) + " | " + (Boolean.parseBoolean(split(mobs.get(mobIndex), 8)) ? "активен" : "выключен"),
                        x, detailsY + 14, GuiTheme.MUTED_TEXT_COLOR, false);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawField(amountField); drawField(mobNameField); drawField(mobHpField); drawField(mobRewardField);
    }

    private void drawField(GuiTextField f) {
        if (f == null) return;
        int originalX = f.x, originalY = f.y, originalWidth = f.width;
        GuiDrawing.drawSurface(originalX, originalY, originalWidth, f.height, GuiTheme.BUTTON_BG_COLOR,
                f.isFocused() ? GuiTheme.CRYSTAL_COLOR : GuiTheme.PANEL_BORDER_COLOR);
        f.x = originalX + 4;
        f.y = originalY + (f.height - 8) / 2;
        f.width = Math.max(1, originalWidth - 8);
        try { f.drawTextBox(); }
        finally { f.x = originalX; f.y = originalY; f.width = originalWidth; }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private void snapshotInputs() {
        if (amountField != null) amountText = amountField.getText();
        if (mobNameField != null) mobNameText = mobNameField.getText();
        if (mobHpField != null) mobHpText = mobHpField.getText();
        if (mobRewardField != null) mobRewardText = mobRewardField.getText();
    }
    private int clampPage(int page, int size) { int max = Math.max(0, (size - 1) / PAGE); return Math.max(0, Math.min(page, max)); }
    private int selectedBalance() {
        String u = playerUuid(), r = resource();
        for (String row : balances) if (u.equals(split(row, 0)) && r.equals(split(row, 1))) return number(split(row, 2));
        return 0;
    }
    private String compactHp(String raw) {
        try { return LargeNumberFormatter.format(LargeNumberValue.parseExact(raw)); }
        catch (RuntimeException e) { return raw; }
    }
    private String unitLabel(int index) {
        String suffix = LargeNumberValue.unitSuffix(index);
        return suffix.isEmpty() ? "x1" : suffix;
    }
    private String playerUuid() { return players.isEmpty() ? "" : split(players.get(playerIndex), 0); }
    private String playerName() { return players.isEmpty() ? "нет игроков онлайн" : split(players.get(playerIndex), 1); }
    private String playerColumn(int c) { return players.isEmpty() ? "-" : split(players.get(playerIndex), c); }
    private String resource() { return resources.isEmpty() ? "нет" : resources.get(resourceIndex); }
    private String mobName() { return mobs.isEmpty() ? "нет мобов" : split(mobs.get(mobIndex), 0); }
    private String mobLabel() { return mobs.isEmpty() ? "нет мобов" : mobName(); }
    private String displayResource(String v) { return v == null ? "" : ResourceManager.getDisplayName(v); }
    private String split(String v, int i) { String[] p = v.split("\\t", -1); return i < p.length ? p[i] : ""; }
    private int number(String v) { try { return Integer.parseInt(v == null ? "0" : v.trim()); } catch (NumberFormatException e) { return 0; } }
    private int findExact(List<String> values, String selected) {
        if (values.isEmpty() || selected == null || selected.isEmpty()) return 0;
        for (int i = 0; i < values.size(); i++) if (selected.equals(values.get(i))) return i;
        return 0;
    }
    private int findByFirstColumn(List<String> values, String selected) {
        if (values.isEmpty() || selected == null || selected.isEmpty()) return 0;
        for (int i = 0; i < values.size(); i++) if (selected.equals(split(values.get(i), 0))) return i;
        return 0;
    }
}
