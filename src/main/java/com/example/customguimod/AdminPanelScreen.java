package com.example.customguimod;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AdminPanelScreen extends GuiScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int DROPDOWN_PAGE_SIZE = 6;

    private final List<String> players = new ArrayList<>();
    private final List<String> resources = new ArrayList<>();
    private final List<String> mobs = new ArrayList<>();
    private final List<String> balances = new ArrayList<>();

    private int tab, playerIndex, resourceIndex, mobIndex, mobTypeIndex;
    private int playerPage, resourcePage, mobPage;
    private boolean playerDropdown, resourceDropdown, mobDropdown, mobTypeDropdown;
    private int confirmAction = -1;
    private long confirmUntil;

    private GuiTextField amountField, mobNameField, mobHpField, mobRewardField;
    private String amountText = "1000";
    private String mobNameText = "TestMob";
    private String mobHpText = "1000";
    private String mobRewardText = "100";

    public AdminPanelScreen(AdminPanelStatePacket state) { readState(state); }

    void applyState(AdminPanelStatePacket state) {
        snapshotInputs();
        readState(state);
        if (mc != null) initGui();
    }

    private void readState(AdminPanelStatePacket state) {
        String selectedPlayer = playerUuid();
        String selectedResource = resource();
        String selectedMob = mobName();

        players.clear(); players.addAll(state.getPlayers());
        resources.clear(); resources.addAll(state.getResources());
        mobs.clear(); mobs.addAll(state.getMobs());
        balances.clear(); balances.addAll(state.getBalances());

        playerIndex = findByFirstColumn(players, selectedPlayer);
        resourceIndex = findExact(resources, selectedResource);
        mobIndex = findByFirstColumn(mobs, selectedMob);
        clampPages();
    }

    @Override
    public void initGui() {
        snapshotInputs();
        buttonList.clear();
        amountField = null;
        mobNameField = null;
        mobHpField = null;
        mobRewardField = null;

        int x = width / 2 - PANEL_WIDTH / 2;
        int y = Math.max(24, height / 2 - 160);
        int contentY = y + 108;

        buttonList.add(new GuiButton(1, x, y, PANEL_WIDTH, 20,
                "Игрок: " + playerName() + (players.size() > 1 ? "  ▼" : "")));
        buttonList.add(new GuiButton(2, x, y + 26, 116, 20, tab == 0 ? "[ РУНЫ ]" : "РУНЫ"));
        buttonList.add(new GuiButton(3, x + 122, y + 26, 116, 20, tab == 1 ? "[ РЕСУРСЫ ]" : "РЕСУРСЫ"));
        buttonList.add(new GuiButton(4, x + 244, y + 26, 116, 20, tab == 2 ? "[ МОБЫ ]" : "МОБЫ"));
        buttonList.add(new GuiButton(5, x, y + 52, PANEL_WIDTH, 20, "Обновить данные"));

        if (tab == 0) buildRunes(x, contentY);
        else if (tab == 1) buildResources(x, contentY);
        else buildMobs(x, contentY);

        buildOpenDropdown(x, y, contentY);
    }

    private void buildRunes(int x, int y) {
        buttonList.add(new GuiButton(20, x, y, 175, 20,
                confirming(20) ? "Повторить: очистить" : "Очистить все"));
        buttonList.add(new GuiButton(21, x + 185, y, 175, 20, "Все NORMAL"));
        buttonList.add(new GuiButton(22, x, y + 26, 175, 20, "Все SILVER"));
        buttonList.add(new GuiButton(23, x + 185, y + 26, 175, 20, "Все GOLD"));
    }

    private void buildResources(int x, int y) {
        String balance = playerUuid().isEmpty() || resources.isEmpty() ? "-" : String.valueOf(selectedBalance());
        buttonList.add(new GuiButton(30, x, y, PANEL_WIDTH, 20,
                "Ресурс: " + displayResource(resource()) + " | Баланс: " + balance + "  ▼"));
        amountField = new GuiTextField(31, fontRenderer, x, y + 42, PANEL_WIDTH, 20);
        amountField.setText(amountText);
        buttonList.add(new GuiButton(32, x, y + 68, 114, 20, "Добавить"));
        buttonList.add(new GuiButton(33, x + 123, y + 68, 114, 20, "Установить"));
        buttonList.add(new GuiButton(34, x + 246, y + 68, 114, 20, "Обнулить"));
        buttonList.add(new GuiButton(35, x, y + 94, PANEL_WIDTH, 20,
                confirming(35) ? "Повторить: обнулить все" : "Обнулить все ресурсы"));
    }

    private void buildMobs(int x, int y) {
        buttonList.add(new GuiButton(40, x, y, 175, 20,
                "Тип: " + (mobTypeIndex == 0 ? "zombie" : "skeleton") + "  ▼"));
        buttonList.add(new GuiButton(41, x + 185, y, 175, 20,
                "Награда: " + displayResource(resource()) + "  ▼"));
        mobNameField = field(42, x, y + 42, PANEL_WIDTH, mobNameText);
        mobHpField = field(43, x, y + 80, 175, mobHpText);
        mobRewardField = field(44, x + 185, y + 80, 175, mobRewardText);
        buttonList.add(new GuiButton(45, x, y + 106, PANEL_WIDTH, 20, "Создать у выбранного игрока"));
        buttonList.add(new GuiButton(46, x, y + 138, PANEL_WIDTH, 20,
                "Моб: " + mobLabel() + (mobs.size() > 1 ? "  ▼" : "")));
        buttonList.add(new GuiButton(47, x, y + 164, 175, 20, "Установить спавн"));
        buttonList.add(new GuiButton(48, x + 185, y + 164, 175, 20,
                confirming(48) ? "Повторить: удалить" : "Удалить"));
    }

    private GuiTextField field(int id, int x, int y, int w, String text) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, w, 20);
        field.setText(text);
        return field;
    }

    private void buildOpenDropdown(int x, int topY, int contentY) {
        if (playerDropdown) {
            addPlayerDropdown(x, topY + 22, PANEL_WIDTH);
        } else if (resourceDropdown) {
            if (tab == 1) addResourceDropdown(x, contentY + 22, PANEL_WIDTH);
            else addResourceDropdown(x + 185, contentY + 22, 175);
        } else if (mobTypeDropdown) {
            buttonList.add(new GuiButton(4000, x, contentY + 22, 175, 20, "zombie"));
            buttonList.add(new GuiButton(4001, x, contentY + 44, 175, 20, "skeleton"));
        } else if (mobDropdown) {
            addMobDropdown(x, contentY + 160, PANEL_WIDTH);
        }
    }

    private void addPlayerDropdown(int x, int y, int w) {
        int start = playerPage * DROPDOWN_PAGE_SIZE;
        int end = Math.min(players.size(), start + DROPDOWN_PAGE_SIZE);
        int row = 0;
        for (int i = start; i < end; i++) {
            buttonList.add(new GuiButton(1000 + i, x, y + row++ * 22, w, 20, split(players.get(i), 1)));
        }
        addPageButtons(x, y + row * 22, w, 5001, 5002, playerPage, players.size());
    }

    private void addResourceDropdown(int x, int y, int w) {
        int start = resourcePage * DROPDOWN_PAGE_SIZE;
        int end = Math.min(resources.size(), start + DROPDOWN_PAGE_SIZE);
        int row = 0;
        for (int i = start; i < end; i++) {
            buttonList.add(new GuiButton(2000 + i, x, y + row++ * 22, w, 20, displayResource(resources.get(i))));
        }
        addPageButtons(x, y + row * 22, w, 5011, 5012, resourcePage, resources.size());
    }

    private void addMobDropdown(int x, int y, int w) {
        int start = mobPage * DROPDOWN_PAGE_SIZE;
        int end = Math.min(mobs.size(), start + DROPDOWN_PAGE_SIZE);
        int row = 0;
        for (int i = start; i < end; i++) {
            String value = mobs.get(i);
            String label = split(value, 0) + " | " + split(value, 1) + " | HP " + split(value, 2);
            buttonList.add(new GuiButton(3000 + i, x, y + row++ * 22, w, 20, label));
        }
        addPageButtons(x, y + row * 22, w, 5021, 5022, mobPage, mobs.size());
    }

    private void addPageButtons(int x, int y, int w, int prevId, int nextId, int page, int size) {
        int pages = Math.max(1, (size + DROPDOWN_PAGE_SIZE - 1) / DROPDOWN_PAGE_SIZE);
        if (pages <= 1) return;
        GuiButton prev = new GuiButton(prevId, x, y, (w - 4) / 2, 20, "< Назад");
        GuiButton next = new GuiButton(nextId, x + (w + 4) / 2, y, (w - 4) / 2, 20, "Далее >");
        prev.enabled = page > 0;
        next.enabled = page + 1 < pages;
        buttonList.add(prev);
        buttonList.add(next);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        snapshotInputs();

        if (button.id >= 1000 && button.id < 2000) {
            playerIndex = button.id - 1000; playerDropdown = false; initGui(); return;
        }
        if (button.id >= 2000 && button.id < 3000) {
            resourceIndex = button.id - 2000; resourceDropdown = false; initGui(); return;
        }
        if (button.id >= 3000 && button.id < 4000) {
            mobIndex = button.id - 3000; mobDropdown = false; initGui(); return;
        }
        if (button.id == 4000 || button.id == 4001) {
            mobTypeIndex = button.id - 4000; mobTypeDropdown = false; initGui(); return;
        }
        if (handlePageButton(button.id)) return;

        if (button.id == 1) { toggleDropdown(0); return; }
        if (button.id >= 2 && button.id <= 4) { tab = button.id - 2; closeDropdowns(); clearConfirm(); initGui(); return; }
        if (button.id == 5) { closeDropdowns(); send("refresh"); return; }

        if (button.id == 20) {
            if (!confirm(20)) return;
            send("runes_clear\t" + playerUuid());
        } else if (button.id >= 21 && button.id <= 23) {
            clearConfirm();
            send("runes_fill\t" + playerUuid() + "\t" + (button.id - 20));
        } else if (button.id == 30 || button.id == 41) {
            toggleDropdown(1); return;
        } else if (button.id == 32) {
            clearConfirm(); resourceAction("resource_add");
        } else if (button.id == 33) {
            clearConfirm(); resourceAction("resource_set");
        } else if (button.id == 34) {
            clearConfirm(); send("resource_zero\t" + playerUuid() + "\t" + resource());
        } else if (button.id == 35) {
            if (!confirm(35)) return;
            send("resource_zero_all\t" + playerUuid());
        } else if (button.id == 40) {
            toggleDropdown(2); return;
        } else if (button.id == 45) {
            clearConfirm(); createMob();
        } else if (button.id == 46) {
            toggleDropdown(3); return;
        } else if (button.id == 47 && !mobs.isEmpty()) {
            clearConfirm(); send("mob_setspawn\t" + mobName() + "\t" + playerUuid());
        } else if (button.id == 48 && !mobs.isEmpty()) {
            if (!confirm(48)) return;
            send("mob_delete\t" + mobName());
        }
    }

    private boolean handlePageButton(int id) {
        if (id == 5001) playerPage = Math.max(0, playerPage - 1);
        else if (id == 5002) playerPage++;
        else if (id == 5011) resourcePage = Math.max(0, resourcePage - 1);
        else if (id == 5012) resourcePage++;
        else if (id == 5021) mobPage = Math.max(0, mobPage - 1);
        else if (id == 5022) mobPage++;
        else return false;
        initGui();
        return true;
    }

    private void toggleDropdown(int kind) {
        boolean player = kind == 0 && !playerDropdown;
        boolean resource = kind == 1 && !resourceDropdown;
        boolean type = kind == 2 && !mobTypeDropdown;
        boolean mob = kind == 3 && !mobDropdown;
        closeDropdowns();
        playerDropdown = player;
        resourceDropdown = resource;
        mobTypeDropdown = type;
        mobDropdown = mob;
        initGui();
    }

    private void closeDropdowns() {
        playerDropdown = false;
        resourceDropdown = false;
        mobDropdown = false;
        mobTypeDropdown = false;
    }

    private boolean confirm(int id) {
        long now = System.currentTimeMillis();
        if (confirmAction == id && now <= confirmUntil) {
            clearConfirm();
            return true;
        }
        confirmAction = id;
        confirmUntil = now + 4000L;
        initGui();
        return false;
    }

    private boolean confirming(int id) {
        if (confirmAction == id && System.currentTimeMillis() <= confirmUntil) return true;
        if (System.currentTimeMillis() > confirmUntil) confirmAction = -1;
        return false;
    }

    private void clearConfirm() {
        confirmAction = -1;
        confirmUntil = 0L;
    }

    private void resourceAction(String action) {
        send(action + "\t" + playerUuid() + "\t" + resource() + "\t" + number(amountText));
    }

    private void createMob() {
        int hp = number(mobHpText), reward = number(mobRewardText);
        String name = mobNameText.trim();
        if (name.isEmpty() || hp <= 0 || reward <= 0) return;
        send("mob_create\t" + name + "\t" + (mobTypeIndex == 0 ? "zombie" : "skeleton")
                + "\t" + resource() + "\t" + reward + "\t" + hp + "\t" + playerUuid());
    }

    private void send(String action) {
        if (playerUuid().isEmpty() && !"refresh".equals(action)) return;
        NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket(action));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (amountField != null && amountField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobNameField != null && mobNameField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobHpField != null && mobHpField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        if (mobRewardField != null && mobRewardField.textboxKeyTyped(c, key)) { snapshotInputs(); return; }
        super.keyTyped(c, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        if (amountField != null) amountField.mouseClicked(x, y, button);
        if (mobNameField != null) mobNameField.mouseClicked(x, y, button);
        if (mobHpField != null) mobHpField.mouseClicked(x, y, button);
        if (mobRewardField != null) mobRewardField.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = width / 2 - PANEL_WIDTH / 2;
        int y = Math.max(24, height / 2 - 160);
        int contentY = y + 108;

        drawCenteredString(fontRenderer, "ADMIN / TEST PANEL", width / 2, y - 16, 0xFFFFFF);
        if (!players.isEmpty()) {
            drawString(fontRenderer,
                    "Цель: " + playerName() + " | dim " + playerColumn(2)
                            + " | XYZ " + playerColumn(3) + " / " + playerColumn(4) + " / " + playerColumn(5),
                    x, y + 78, 0xD0D0D0);
            drawString(fontRenderer,
                    "Руны: " + playerColumn(6) + "/26 | NORMAL " + playerColumn(7)
                            + " | SILVER " + playerColumn(8) + " | GOLD " + playerColumn(9),
                    x, y + 90, 0xB8B8B8);
        } else {
            drawString(fontRenderer, "Нет доступных игроков онлайн", x, y + 84, 0xFF7777);
        }

        if (tab == 1) {
            drawString(fontRenderer, "Количество:", x, contentY + 30, 0xC8C8C8);
        } else if (tab == 2) {
            drawString(fontRenderer, "Имя моба:", x, contentY + 30, 0xC8C8C8);
            drawString(fontRenderer, "HP:", x, contentY + 68, 0xC8C8C8);
            drawString(fontRenderer, "Награда:", x + 185, contentY + 68, 0xC8C8C8);
            if (!mobs.isEmpty()) {
                drawString(fontRenderer,
                        "Выбран: " + mobName() + " | " + split(mobs.get(mobIndex), 1)
                                + " | HP " + split(mobs.get(mobIndex), 2)
                                + " | " + displayResource(split(mobs.get(mobIndex), 3)) + " x" + split(mobs.get(mobIndex), 4),
                        x, contentY + 190, 0xB8B8B8);
                drawString(fontRenderer,
                        "Spawn: " + split(mobs.get(mobIndex), 5) + " / " + split(mobs.get(mobIndex), 6)
                                + " / " + split(mobs.get(mobIndex), 7)
                                + " | " + (Boolean.parseBoolean(split(mobs.get(mobIndex), 8)) ? "активен" : "выключен"),
                        x, contentY + 202, 0xB8B8B8);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        if (amountField != null) amountField.drawTextBox();
        if (mobNameField != null) { mobNameField.drawTextBox(); mobHpField.drawTextBox(); mobRewardField.drawTextBox(); }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private void snapshotInputs() {
        if (amountField != null) amountText = amountField.getText();
        if (mobNameField != null) mobNameText = mobNameField.getText();
        if (mobHpField != null) mobHpText = mobHpField.getText();
        if (mobRewardField != null) mobRewardText = mobRewardField.getText();
    }

    private void clampPages() {
        playerPage = clampPage(playerPage, players.size());
        resourcePage = clampPage(resourcePage, resources.size());
        mobPage = clampPage(mobPage, mobs.size());
    }

    private int clampPage(int page, int size) {
        int max = Math.max(0, (size - 1) / DROPDOWN_PAGE_SIZE);
        return Math.max(0, Math.min(page, max));
    }

    private int selectedBalance() {
        String uuid = playerUuid(), selectedResource = resource();
        for (String row : balances) {
            if (uuid.equals(split(row, 0)) && selectedResource.equals(split(row, 1))) {
                return number(split(row, 2));
            }
        }
        return 0;
    }

    private String playerUuid() { return players.isEmpty() ? "" : split(players.get(playerIndex), 0); }
    private String playerName() { return players.isEmpty() ? "нет игроков онлайн" : split(players.get(playerIndex), 1); }
    private String playerColumn(int column) { return players.isEmpty() ? "-" : split(players.get(playerIndex), column); }
    private String resource() { return resources.isEmpty() ? "нет" : resources.get(resourceIndex); }
    private String mobName() { return mobs.isEmpty() ? "нет мобов" : split(mobs.get(mobIndex), 0); }
    private String mobLabel() { return mobs.isEmpty() ? "нет мобов" : mobName(); }
    private String displayResource(String value) { return value == null ? "" : ResourceManager.getDisplayName(value); }
    private String split(String value, int index) { String[] p = value.split("\\t", -1); return index < p.length ? p[index] : ""; }
    private int number(String value) { try { return Integer.parseInt(value == null ? "0" : value.trim()); } catch (NumberFormatException e) { return 0; } }

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
