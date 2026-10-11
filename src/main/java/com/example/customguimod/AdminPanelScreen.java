package com.example.customguimod;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AdminPanelScreen extends GuiScreen {
    private final List<String> players = new ArrayList<>();
    private final List<String> resources = new ArrayList<>();
    private final List<String> mobs = new ArrayList<>();
    private int tab, playerIndex, resourceIndex, mobIndex, mobTypeIndex;
    private GuiTextField amountField, mobNameField, mobHpField, mobRewardField;

    public AdminPanelScreen(AdminPanelStatePacket state) { readState(state); }

    void applyState(AdminPanelStatePacket state) {
        readState(state);
        if (mc != null) initGui();
    }

    private void readState(AdminPanelStatePacket state) {
        players.clear(); players.addAll(state.getPlayers());
        resources.clear(); resources.addAll(state.getResources());
        mobs.clear(); mobs.addAll(state.getMobs());
        if (playerIndex >= players.size()) playerIndex = 0;
        if (resourceIndex >= resources.size()) resourceIndex = 0;
        if (mobIndex >= mobs.size()) mobIndex = 0;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int x = width / 2 - 150, y = Math.max(30, height / 2 - 145);
        buttonList.add(new GuiButton(1, x, y, 300, 20, "Игрок: " + playerName()));
        buttonList.add(new GuiButton(2, x, y + 26, 96, 20, tab == 0 ? "[ РУНЫ ]" : "РУНЫ"));
        buttonList.add(new GuiButton(3, x + 102, y + 26, 96, 20, tab == 1 ? "[ РЕСУРСЫ ]" : "РЕСУРСЫ"));
        buttonList.add(new GuiButton(4, x + 204, y + 26, 96, 20, tab == 2 ? "[ МОБЫ ]" : "МОБЫ"));
        buttonList.add(new GuiButton(5, x, y + 52, 300, 20, "Обновить данные"));
        if (tab == 0) buildRunes(x, y + 82);
        else if (tab == 1) buildResources(x, y + 82);
        else buildMobs(x, y + 82);
    }

    private void buildRunes(int x, int y) {
        buttonList.add(new GuiButton(20, x, y, 145, 20, "Очистить все"));
        buttonList.add(new GuiButton(21, x + 155, y, 145, 20, "Все NORMAL"));
        buttonList.add(new GuiButton(22, x, y + 26, 145, 20, "Все SILVER"));
        buttonList.add(new GuiButton(23, x + 155, y + 26, 145, 20, "Все GOLD"));
    }

    private void buildResources(int x, int y) {
        buttonList.add(new GuiButton(30, x, y, 300, 20, "Ресурс: " + resource()));
        amountField = new GuiTextField(31, fontRenderer, x, y + 28, 300, 20);
        amountField.setText("1000");
        buttonList.add(new GuiButton(32, x, y + 54, 94, 20, "Добавить"));
        buttonList.add(new GuiButton(33, x + 103, y + 54, 94, 20, "Установить"));
        buttonList.add(new GuiButton(34, x + 206, y + 54, 94, 20, "Обнулить"));
        buttonList.add(new GuiButton(35, x, y + 80, 300, 20, "Обнулить все ресурсы"));
    }

    private void buildMobs(int x, int y) {
        buttonList.add(new GuiButton(40, x, y, 145, 20, "Тип: " + (mobTypeIndex == 0 ? "zombie" : "skeleton")));
        buttonList.add(new GuiButton(41, x + 155, y, 145, 20, "Награда: " + resource()));
        mobNameField = field(42, x, y + 28, 300, "TestMob");
        mobHpField = field(43, x, y + 54, 145, "1000");
        mobRewardField = field(44, x + 155, y + 54, 145, "100");
        buttonList.add(new GuiButton(45, x, y + 80, 300, 20, "Создать у выбранного игрока"));
        buttonList.add(new GuiButton(46, x, y + 112, 300, 20, "Моб: " + mobName()));
        buttonList.add(new GuiButton(47, x, y + 138, 145, 20, "Установить спавн"));
        buttonList.add(new GuiButton(48, x + 155, y + 138, 145, 20, "Удалить"));
    }

    private GuiTextField field(int id, int x, int y, int w, String text) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, w, 20);
        field.setText(text);
        return field;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) { cyclePlayer(); return; }
        if (button.id >= 2 && button.id <= 4) { tab = button.id - 2; initGui(); return; }
        if (button.id == 5) { send("refresh"); return; }
        if (button.id == 20) send("runes_clear\t" + playerUuid());
        else if (button.id >= 21 && button.id <= 23) send("runes_fill\t" + playerUuid() + "\t" + (button.id - 20));
        else if (button.id == 30 || button.id == 41) { cycleResource(); return; }
        else if (button.id == 32) resourceAction("resource_add");
        else if (button.id == 33) resourceAction("resource_set");
        else if (button.id == 34) send("resource_zero\t" + playerUuid() + "\t" + resource());
        else if (button.id == 35) send("resource_zero_all\t" + playerUuid());
        else if (button.id == 40) { mobTypeIndex = 1 - mobTypeIndex; initGui(); }
        else if (button.id == 45) createMob();
        else if (button.id == 46) { if (!mobs.isEmpty()) mobIndex = (mobIndex + 1) % mobs.size(); initGui(); }
        else if (button.id == 47 && !mobs.isEmpty()) send("mob_setspawn\t" + mobName() + "\t" + playerUuid());
        else if (button.id == 48 && !mobs.isEmpty()) send("mob_delete\t" + mobName());
    }

    private void cyclePlayer() { if (!players.isEmpty()) playerIndex = (playerIndex + 1) % players.size(); initGui(); }
    private void cycleResource() { if (!resources.isEmpty()) resourceIndex = (resourceIndex + 1) % resources.size(); initGui(); }

    private void resourceAction(String action) {
        send(action + "\t" + playerUuid() + "\t" + resource() + "\t" + number(amountField));
    }

    private void createMob() {
        int hp = number(mobHpField), reward = number(mobRewardField);
        String name = mobNameField == null ? "" : mobNameField.getText().trim();
        if (name.isEmpty() || hp <= 0 || reward <= 0) return;
        send("mob_create\t" + name + "\t" + (mobTypeIndex == 0 ? "zombie" : "skeleton")
                + "\t" + resource() + "\t" + reward + "\t" + hp + "\t" + playerUuid());
    }

    private void send(String action) {
        NetworkHandler.INSTANCE.sendToServer(new AdminPanelRequestPacket(action));
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (amountField != null && amountField.textboxKeyTyped(c, key)) return;
        if (mobNameField != null && mobNameField.textboxKeyTyped(c, key)) return;
        if (mobHpField != null && mobHpField.textboxKeyTyped(c, key)) return;
        if (mobRewardField != null && mobRewardField.textboxKeyTyped(c, key)) return;
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
        drawCenteredString(fontRenderer, "ADMIN / TEST PANEL", width / 2, Math.max(12, height / 2 - 165), 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (amountField != null) amountField.drawTextBox();
        if (mobNameField != null) { mobNameField.drawTextBox(); mobHpField.drawTextBox(); mobRewardField.drawTextBox(); }
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private String playerUuid() { return players.isEmpty() ? "" : split(players.get(playerIndex), 0); }
    private String playerName() { return players.isEmpty() ? "нет игроков онлайн" : split(players.get(playerIndex), 1); }
    private String resource() { return resources.isEmpty() ? "нет" : resources.get(resourceIndex); }
    private String mobName() { return mobs.isEmpty() ? "нет мобов" : split(mobs.get(mobIndex), 0); }
    private String split(String value, int index) { String[] p = value.split("\\t", -1); return index < p.length ? p[index] : ""; }
    private int number(GuiTextField field) { try { return Integer.parseInt(field == null ? "0" : field.getText().trim()); } catch (NumberFormatException e) { return 0; } }
}
