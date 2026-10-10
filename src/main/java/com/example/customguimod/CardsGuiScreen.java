package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.customguimod.GuiControls.*;
import static com.example.customguimod.GuiDrawing.drawSurface;
import static com.example.customguimod.GuiTheme.*;

public class CardsGuiScreen extends GuiScreen {

    private static final int STORAGE_SLOT_COUNT = RuneInventory.LEGACY_RECORD_CAPACITY;
    private static final int VISIBLE_RUNE_SLOTS = RuneInventory.BOOST_TYPE_COUNT;

    private static final int STAT_ICON_SIZE = 10;
    private static final int STAT_ICON_GAP = 5;

    private static final int BUY_BUTTON_ID = 0;
    private static final int UPGRADE_PLACEHOLDER_ID = 2;
    private static final int RUNE_UNIT_PRICE = 100;
    private static final int ELEMENT_BUTTON_START_ID = 100;
    private static final int MAX_ELEMENTS = 8;

    private static final int NAV_PLACEHOLDER_START_ID = 201;

    private static final String[] CARD_NAMES = {
            "Каменный молот", "Теневой клинок", "Сердце леса", "Печать кузнеца",
            "Сердце голема", "Мшистый осколок", "Древний корень",
            "Обсидиановый шип", "Семя древолеса", "Треснувшая печать"
    };

    private static final String[] RUNE_TYPES = {
            "Клик", "Клик", "Земля", "Земля", "Земля",
            "Вода", "Земля", "Ресурс", "Земля", "Земля"
    };

    private static final ItemStack[] CARD_ICONS = {
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.LEATHER_CHESTPLATE),
            new ItemStack(Blocks.SAPLING),
            new ItemStack(Items.SHIELD),
            new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.FISH),
            new ItemStack(Blocks.LEAVES),
            new ItemStack(Items.BED),
            new ItemStack(Items.STICK),
            new ItemStack(Items.BOOK),
            new ItemStack(Items.GOLD_NUGGET)
    };

    private static final int[] CARD_STATS = {
            4080, 2140, 2920, 1940, 2550,
            3050, 2720, 2410, 3300, 2500
    };

    private final RuneInventory inventory = new RuneInventory();
    private final List<String> deckNames = new ArrayList<>();
    private final List<FlyingCard> flyingCards = new ArrayList<>();

    private int activeDeck = 0;
    private int buyAmount = 1;
    private static RenderItem renderItem;

    private final GuiLayout layout = new GuiLayout(VISIBLE_RUNE_SLOTS);

    private net.minecraft.client.gui.GuiTextField searchField;
    private net.minecraft.client.shader.ShaderGroup guiBlur;

    private boolean matchesSearch(int slot) {
        if (searchField == null) return true;

        String query = searchField.getText().trim().toLowerCase(java.util.Locale.ROOT);
        if (query.isEmpty()) return true;

        String name = "";
        String type = "";

        if (activeDeck == 0) {
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(slot);
            if (definition != null) {
                name = definition.name;
                type = definition.type;
            }
        } else {
            RuneInventory.Entry entry = getRuneForVisualSlot(slot);
            if (entry != null && entry.cardIndex >= 0 && entry.cardIndex < CARD_NAMES.length) {
                name = CARD_NAMES[entry.cardIndex];
                type = entry.cardIndex < RUNE_TYPES.length
                        ? RUNE_TYPES[entry.cardIndex]
                        : "Ресурс";
            }
        }

        return name.toLowerCase(java.util.Locale.ROOT).contains(query)
                || type.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    int canvasMouseX(int mouseX) {
        return layout.canvasMouseX(mouseX);
    }

    int canvasMouseY(int mouseY) {
        return layout.canvasMouseY(mouseY);
    }

    int awakeningRuneAt(int screenX, int screenY) {
        return RuneAwakeningInput.runeAt(layout, inventory, activeDeck,
                screenX, screenY, this::matchesSearch);
    }

    boolean runeMatchesSearch(int slot) {
        return matchesSearch(slot);
    }

    private void drawCanvasTooltip(List<String> lines, int x, int y) {
        int oldWidth = width, oldHeight = height;
        width = GuiLayout.CANVAS_WIDTH; height = GuiLayout.CANVAS_HEIGHT;
        try { drawHoveringText(lines, x, y); }
        finally { width = oldWidth; height = oldHeight; }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        x = layout.canvasMouseX(x);
        y = layout.canvasMouseY(y);
        searchField.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (!searchField.textboxKeyTyped(character, key)) super.keyTyped(character, key);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        fontRenderer = GuiFontRenderer.get(mc);

        if (renderItem == null) {
            renderItem = Minecraft.getMinecraft().getRenderItem();
        }

        inventory.clear();

        updateLayout();
        rebuildControls();

        if (!mc.entityRenderer.isShaderActive()) {
            try {
                mc.entityRenderer.loadShader(
                        new ResourceLocation("customguimod", "shaders/post/gui_blur.json")
                );
                guiBlur = mc.entityRenderer.getShaderGroup();
            } catch (RuntimeException e) {
                guiBlur = null;
                CustomGuiMod.logger.warn("Could not enable GUI blur shader", e);
            }
        }

        NetworkHandler.INSTANCE.sendToServer(new PingPacket("get_player_stats"));
        NetworkHandler.INSTANCE.sendToServer(new PingPacket("load_decks"));
        NetworkHandler.INSTANCE.sendToServer(new PingPacket("load_cards"));
    }

    private int scaled(int base, int minimum) {
        return layout.scaled(base, minimum);
    }

    private String equipmentTooltip;

    private void updateLayout() {
        layout.update(width, height);
    }

    private void rebuildControls() {
        buttonList.clear();
        updateLayout();

        int pad = scaled(7, 3);
        int buttonX = layout.runePanelX + pad;
        int buttonWidth = layout.runePanelWidth - pad * 2;
        int amountHeight = 24;
        int buyHeight = 36;

        int buyY = layout.purchaseTop;
        int amountY = buyY + buyHeight + 6;
        int purchaseWidth = (buttonWidth - 5) / 2;
        buttonList.add(new PanelButton(BUY_BUTTON_ID, buttonX, buyY,
                purchaseWidth, buyHeight, "Купить руну"));
        PanelButton upgrade = new PanelButton(UPGRADE_PLACEHOLDER_ID,
                buttonX + purchaseWidth + 5, buyY, buttonWidth - purchaseWidth - 5,
                buyHeight, "Улучшить руну:\nЦена не задана");
        upgrade.enabled = false;
        buttonList.add(upgrade);
        buttonList.add(new PanelButton(AMOUNT_BUTTON_START_ID, buttonX, amountY,
                amountHeight, amountHeight, "x" + buyAmount));
        String search = searchField == null ? "" : searchField.getText();
        searchField = new net.minecraft.client.gui.GuiTextField(400, fontRenderer,
                layout.runePanelX + 16, layout.runePanelY + 30, layout.runePanelWidth - 32, 19);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setTextColor(TEXT_COLOR);
        searchField.setDisabledTextColour(MUTED_TEXT_COLOR);
        searchField.setMaxStringLength(64);
        searchField.setText(search);
        rebuildElementButtons();
        rebuildNavigationButtons();
        updateAmountButtonSelection();
        updatePurchaseAvailability();
    }

    private void rebuildElementButtons() {
        int count = Math.min(deckNames.size(), MAX_ELEMENTS);
        if (count <= 0) return;

        int x = layout.runePanelX + scaled(8, 4);
        int y = layout.runePanelY + 57;
        int gap = scaled(3, 2);
        int availableRight = layout.runePanelX + layout.runePanelWidth - scaled(8, 4);

        for (int i = 0; i < count; i++) {
            String name = deckNames.get(i);
            int desired = Math.max(scaled(38, 28), fontRenderer.getStringWidth(name) + scaled(14, 7));
            int remaining = count - i;
            int maxForThis = Math.max(1, (availableRight - x - gap * (remaining - 1)) / remaining);
            int buttonWidth = Math.min(desired, maxForThis);
            if (x + buttonWidth > availableRight) break;

            buttonList.add(new ElementButton(
                    ELEMENT_BUTTON_START_ID + i,
                    x, y, buttonWidth, scaled(18, 15),
                    name, i == activeDeck
            ));
            x += buttonWidth + gap;
        }
    }

    private void rebuildNavigationButtons() {
        int pad = 2;
        int buttonSize = layout.navPanelWidth - pad * 2;
        int x = layout.navPanelX + pad;
        int y = layout.navPanelY + pad;
        int gap = 10;

        buttonList.add(new NavButton(
                NAV_RUNES_ID, x, y, buttonSize, buttonSize,
                new ItemStack(Items.PRISMARINE_SHARD), true, true
        ));

        int closeSize = buttonSize;
        int closeY = layout.navPanelY + layout.navPanelHeight - pad - closeSize;
        buttonList.add(new PanelButton(
                CLOSE_BUTTON_ID, x, closeY, closeSize, closeSize, "X"
        ));
    }

    void updatePurchaseAvailability() {
        boolean full = inventory.isPurchaseLimitReached();

        for (GuiButton button : buttonList) {
            if (button.id == BUY_BUTTON_ID) {
                int collectionRank = RuneInventory.GOLD_RANK;

                for (int slot = 0; slot < RuneInventory.BOOST_TYPE_COUNT; slot++) {
                    RuneInventory.Entry entry = inventory.get(slot);
                    if (entry != null) {
                        collectionRank = Math.min(collectionRank, entry.layer);
                    }
                }

                button.enabled = !full;

                if (full) {
                    button.displayString = collectionRank >= RuneInventory.GOLD_RANK
                            ? "Максимальный ранг"
                            : "Пробудить руну";
                } else {
                    button.displayString = "Купить руну:\n"
                            + (RUNE_UNIT_PRICE * buyAmount)
                            + " монет";
                }
            } else if (button.id == AMOUNT_BUTTON_START_ID) {
                button.enabled = !full;
            }
        }
    }

    private void updateAmountButtonSelection() {
        for (GuiButton button : buttonList) {
            if (button.id == AMOUNT_BUTTON_START_ID) {
                button.displayString = "x" + buyAmount;
            }
        }
        updatePurchaseAvailability();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == CLOSE_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        if (button.id == NAV_RUNES_ID) {
            return;
        }

        if (button.id >= ELEMENT_BUTTON_START_ID && button.id < ELEMENT_BUTTON_START_ID + MAX_ELEMENTS) {
            int targetDeck = button.id - ELEMENT_BUTTON_START_ID;
            if (targetDeck >= 0 && targetDeck < deckNames.size() && targetDeck != activeDeck) {
                clearCards();
                NetworkHandler.INSTANCE.sendToServer(new PingPacket("switch_deck:" + targetDeck));
            }
            return;
        }

        if (button.id == BUY_BUTTON_ID) {
            NetworkHandler.INSTANCE.sendToServer(
                    new PingPacket("buy_cards:" + buyAmount)
            );
            return;
        }

        if (button.id == AMOUNT_BUTTON_START_ID) {
            buyAmount = buyAmount == 1 ? 5
                    : buyAmount == 5 ? 10
                    : buyAmount == 10 ? 100
                    : 1;
            updateAmountButtonSelection();
        }
    }

    private void drawPanel(int x, int y, int w, int h) {
        drawSurface(x, y, w, h, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
    }

    private void drawSection(int x, int y, int w, int h) {
        drawSurface(x, y, w, h, PANEL_BG_SOFT, PANEL_BORDER_COLOR);
    }

    private void drawDivider(int x, int y, int w) {
        drawRect(x, y, x + Math.max(1, w), y + 1, PANEL_LINE_COLOR);
    }

    private void drawHeadingString(String text, int x, int y, int maxWidth, int color, boolean shadow) {
        drawFittedString(GuiFontRenderer.heading(mc), text, x, y, maxWidth, color, shadow);
    }

    private void drawFittedString(String text, int x, int y, int maxWidth, int color, boolean shadow) {
        drawFittedString(fontRenderer, text, x, y, maxWidth, color, shadow);
    }

    private void drawFittedString(net.minecraft.client.gui.FontRenderer font, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        int width = font.getStringWidth(text);
        if (width <= maxWidth) {
            if (shadow) font.drawStringWithShadow(text, x, y, color);
            else font.drawString(text, x, y, color);
            return;
        }

        float scale = Math.max(0.55F, maxWidth / (float) Math.max(1, width));
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        if (shadow) font.drawStringWithShadow(text, 0, 0, color);
        else font.drawString(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    private void drawLeftPanelContent() {
        int pad = scaled(8, 4);
        int x = layout.leftPanelX + pad;
        int textW = Math.max(10, layout.leftPanelWidth - pad * 2);
        int y = layout.leftPanelY + pad;

        drawHeadingString("СТАТИСТИКА", x, y, textW, HEADING_TEXT_COLOR, true);
        y += scaled(21, 16);
        double baseDamage = mc.player == null ? 1.0D
                : mc.player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        double damage = baseDamage + ClientPlayerStats.getTotalDamage();
        String damageText = damage == Math.rint(damage) ? String.valueOf((long) damage)
                : String.format(java.util.Locale.ROOT, "%.2f", damage).replaceAll("0+$", "").replaceAll("\\.$", "");
        drawStatLine(x, y, "Урон за клик", damageText, textW);

        int resourcesY = layout.leftPanelY + layout.leftPanelHeight + 11;
        drawPanel(layout.leftPanelX, resourcesY, layout.leftPanelWidth, 68);
        y = resourcesY + pad;
        drawHeadingString("РЕСУРСЫ", x, y, textW, HEADING_TEXT_COLOR, true);
        y += scaled(21, 16);
        drawStatLine(x, y, "Монеты", String.valueOf(ClientPlayerStats.getCoins()), textW);
        y += scaled(16, 12);
        drawStatLine(x, y, "Кристаллы", String.valueOf(ClientPlayerStats.getCrystals()), textW);
    }

    private void drawStatLine(int x, int y, String name, String value, int availableWidth) {
        int valueWidth = fontRenderer.getStringWidth(value);
        int valueX = x + availableWidth - valueWidth;
        drawStatIcon(x, y, name);
        int nameX = x + STAT_ICON_SIZE + STAT_ICON_GAP;
        int nameMax = Math.max(8, valueX - nameX - scaled(4, 2));
        drawFittedString(name, nameX, y, nameMax, TEXT_COLOR, false);
        fontRenderer.drawString(value, valueX, y, TEXT_COLOR);
    }

    private void drawStatIcon(int x, int y, String name) {
        if ("Монеты".equals(name)) {
            drawRect(x + 2, y + 1, x + 8, y + 10, COIN_COLOR);
            drawRect(x + 1, y + 3, x + 9, y + 8, COIN_COLOR);
            drawRect(x + 3, y + 3, x + 5, y + 7, COIN_HIGHLIGHT_COLOR);
        } else if ("Кристаллы".equals(name)) {
            drawRect(x + 3, y + 1, x + 7, y + 10, CRYSTAL_COLOR);
            drawRect(x + 1, y + 4, x + 9, y + 7, CRYSTAL_COLOR);
            drawRect(x + 2, y + 2, x + 8, y + 9, CRYSTAL_COLOR);
            drawRect(x + 3, y + 2, x + 5, y + 6, CRYSTAL_HIGHLIGHT_COLOR);
        } else {
            for (int i = 0; i < 6; i++)
                drawRect(x + 7 - i, y + 1 + i, x + 9 - i, y + 3 + i, SWORD_BLADE_COLOR);
            drawRect(x + 1, y + 6, x + 5, y + 8, SWORD_HILT_COLOR);
            drawRect(x, y + 8, x + 2, y + 10, SWORD_GRIP_COLOR);
        }
    }

    private void drawCenterPanelContent(int mouseX, int mouseY) {
        equipmentTooltip = null;
        int x = layout.centerPanelX, y = layout.centerPanelY;
        drawPanel(x, y, layout.centerPanelWidth, 63);
        // Progression and donation data will replace these presentation placeholders.
        fontRenderer.drawString("#1", x + 8, y + 9, TEXT_COLOR);
        int rankSeparatorX = x + 8 + fontRenderer.getStringWidth("#1") + 7;
        drawRect(rankSeparatorX, y + 10, rankSeparatorX + 1, y + 20, MUTED_TEXT_COLOR);
        drawFittedString("Моб #1", rankSeparatorX + 8, y + 9,
                x + layout.centerPanelWidth - 8 - rankSeparatorX - 8, TEXT_COLOR, true);
        drawFittedString("Базовая", x + 8, y + 29, 303, LINK_TEXT_COLOR, false);
        drawFittedString("—", x + 8, y + 47, 24, MUTED_TEXT_COLOR, false);
        int separatorX = x + 8 + fontRenderer.getStringWidth("—") + 7;
        drawRect(separatorX, y + 48, separatorX + 1, y + 58, MUTED_TEXT_COLOR);
        int nameX = separatorX + 8;
        drawFittedString(mc.player == null ? "Игрок" : mc.player.getName(),
                nameX, y + 47, x + layout.centerPanelWidth - 8 - nameX, TEXT_COLOR, true);

        int top = y + 74;
        drawPanel(x, top, layout.centerPanelWidth, layout.centerPanelHeight - 74);
        int slot = 50, gap = 8, left = x + 9;
        // The reference has five positions across, three side rows and a bottom row.
        String[] upper = {"Основная рука", "Дополнительная рука", "Талисман", "Реликвия", "Артефакт"};
        for (int i = 0; i < 5; i++) {
            drawEquipmentSlot(left + i * (slot + gap), top + 9, slot, upper[i], mouseX, mouseY);
        }
        String[] armor = {"Шлем", "Нагрудник", "Поножи"};
        for (int i = 0; i < 3; i++) {
            drawEquipmentSlot(left, top + 67 + i * 58, slot, armor[i], mouseX, mouseY);
            drawEquipmentSlot(left + 232, top + 67 + i * 58, slot,
                    i == 0 ? "Ботинки" : i == 1 ? "Аксессуар" : "Дополнительный артефакт", mouseX, mouseY);
        }
        for (int i = 0; i < 5; i++) {
            drawEquipmentSlot(left + i * 58, top + 241, slot,
                    i == 2 ? "Особый предмет" : "Дополнительное снаряжение", mouseX, mouseY);
        }
        int previewX = left + 58, previewY = top + 67;
        drawSection(previewX, previewY, 166, 166);
        if (mc.player != null) {
            GlStateManager.color(1, 1, 1, 1);
            GuiInventory.drawEntityOnScreen(previewX + 83, previewY + 153, 76,
                    previewX + 83 - mouseX, previewY + 55 - mouseY, mc.player);
            GlStateManager.color(1, 1, 1, 1);
        }
        int buildsY = top + 303;
        drawHeadingString("СБОРКИ ЭКИПИРОВКИ", left, buildsY, 205, HEADING_TEXT_COLOR, false);
        drawFittedString("1 из 5 открыто", left + 202, buildsY, 80, MUTED_TEXT_COLOR, false);
        for (int i = 0; i < 5; i++) {
            PanelButton tab = new PanelButton(-1, left + i * 58, buildsY + 16, 53, 18, i == 0 ? "1" : "");
            tab.setSelected(i == 0);
            tab.enabled = i == 0;
            tab.drawButton(mc, mouseX, mouseY, 0);
            if (i > 0) {
                String number = String.valueOf(i + 1);
                int groupWidth = fontRenderer.getStringWidth(number) + 4 + 8;
                int groupX = left + i * 58 + (53 - groupWidth) / 2;
                fontRenderer.drawString(number, groupX, buildsY + 19, TEXT_COLOR);
                drawSmallLock(groupX + fontRenderer.getStringWidth(number) + 4, buildsY + 20);
            }
        }
        drawFittedString("Сборка 1 - пусто", left, buildsY + 39, 301, TEXT_COLOR, false);
        String[] actions = {"Надеть", "Записать", "Название"};
        for (int i = 0; i < actions.length; i++) {
            PanelButton action = new PanelButton(-1, left + i * 96, buildsY + 52, 90, 18, actions[i]);
            action.enabled = false;
            action.drawButton(mc, mouseX, mouseY, 0);
        }
    }

    private void drawEquipmentSlot(int x, int y, int size, String name, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        drawSurface(x, y, size, size, SLOT_BG_COLOR, hover ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        drawLock(x + size / 2 - 6, y + size / 2 - 6, EQUIPMENT_LOCK_COLOR);
        if (hover) equipmentTooltip = name + " — недоступно";
    }
    private void drawSmallLock(int x, int y) {
        drawRect(x + 2, y, x + 6, y + 1, LOCK_COLOR);
        drawRect(x + 1, y + 1, x + 3, y + 4, LOCK_COLOR);
        drawRect(x + 5, y + 1, x + 7, y + 4, LOCK_COLOR);
        drawRect(x, y + 4, x + 8, y + 9, LOCK_COLOR);
        drawRect(x + 3, y + 5, x + 5, y + 7, SMALL_LOCK_KEYHOLE_COLOR);
    }

    private void drawLock(int x, int y, int color) {
        // Stepped arch and compact square keyhole measured from the reference.
        drawRect(x + 3, y, x + 9, y + 1, color);
        drawRect(x + 2, y + 1, x + 10, y + 3, color);
        drawRect(x + 1, y + 3, x + 4, y + 6, color);
        drawRect(x + 8, y + 3, x + 11, y + 6, color);
        drawRect(x + 4, y + 2, x + 8, y + 6, LOCK_KEYHOLE_COLOR);
        drawRect(x - 1, y + 6, x + 13, y + 12, color);
        drawRect(x + 4, y + 8, x + 8, y + 11, LOCK_KEYHOLE_COLOR);
    }
    private void drawRunePanelContent(int mouseX, int mouseY) {
        int pad = scaled(8, 4);
        int titleX = layout.runePanelX + pad;
        int titleY = layout.runePanelY + pad;
        drawHeadingString("РУНЫ", titleX, titleY, layout.runePanelWidth / 2, HEADING_TEXT_COLOR, true);
        String count = activeDeck == 0
                ? inventory.ownedCatalogSlotCount() + " из " + EarthRuneCatalog.size()
                : inventory.occupiedTypeCount(CARD_NAMES.length) + " из " + CARD_NAMES.length;
        fontRenderer.drawString(count,
                layout.runePanelX + layout.runePanelWidth - pad - fontRenderer.getStringWidth(count), titleY, MUTED_TEXT_COLOR);
        drawSurface(titleX, layout.runePanelY + 30, layout.runePanelWidth - pad * 2, 19,
                BUTTON_BG_COLOR, searchField.isFocused() ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 4, 0);
        searchField.drawTextBox();
        GlStateManager.popMatrix();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            drawFittedString("Поиск...", titleX + 8, layout.runePanelY + 34,
                    layout.runePanelWidth - 24, MUTED_TEXT_COLOR, false);
        }
        drawPanel(layout.runePanelX, layout.purchasePanelY, layout.runePanelWidth, GuiLayout.PURCHASE_PANEL_HEIGHT);
        drawHeadingString("ПОКУПКА РУН", titleX, layout.purchasePanelY + 8, 300, HEADING_TEXT_COLOR, false);
        drawRuneActionHints(titleX, layout.runePanelY + layout.runePanelHeight - 17,
                layout.runePanelWidth - pad * 2);
        for (int i = 0; i < VISIBLE_RUNE_SLOTS; i++) {
            int[] pos = getRuneSlotPosition(i);
            if (matchesSearch(i)) drawRuneSlot(pos[0], pos[1], i, mouseX, mouseY);
        }
    }

    private void drawRuneActionHints(int x, int y, int availableWidth) {
        String[] labels = {"Пробудить", "+ CTRL Поделиться в чате", "+ CTRL Ослабить"};
        int[] buttons = {2, 0, 1};
        int totalWidth = 0;
        for (String label : labels) totalWidth += 13 + fontRenderer.getStringWidth(label);
        totalWidth += 20;
        float scale = Math.min(1.0F, availableWidth / (float) totalWidth);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        int offset = 0;
        for (int i = 0; i < labels.length; i++) {
            drawMouseHintIcon(offset, 0, buttons[i]);
            fontRenderer.drawString(labels[i], offset + 13, 0, ACTION_HINT_TEXT_COLOR);
            offset += 13 + fontRenderer.getStringWidth(labels[i]) + 10;
        }
        GlStateManager.popMatrix();
    }

    private void drawMouseHintIcon(int x, int y, int button) {
        int white = MOUSE_HINT_COLOR, dark = MOUSE_HINT_BACKGROUND_COLOR, red = MOUSE_HINT_ACTIVE_COLOR;
        drawRect(x + 1, y, x + 7, y + 10, white);
        drawRect(x, y + 2, x + 8, y + 8, white);
        drawRect(x + 1, y + 1, x + 7, y + 8, dark);
        drawRect(x + 3, y + 1, x + 5, y + 4, white);
        if (button == 0) drawRect(x + 1, y + 1, x + 3, y + 5, red);
        else if (button == 1) drawRect(x + 5, y + 1, x + 7, y + 5, red);
        else drawRect(x + 3, y + 1, x + 5, y + 4, red);
    }

    private void drawRuneSlot(int x, int y, int visualIndex, int mouseX, int mouseY) {
        if (activeDeck == 0) {
            drawEarthRuneSlot(x, y, visualIndex, mouseX, mouseY);
            return;
        }
        boolean hovered = mouseX >= x && mouseX < x + layout.runeSlotWidth
                && mouseY >= y && mouseY < y + layout.runeSlotHeight;
        boolean futureType = visualIndex >= CARD_NAMES.length;
        int border = futureType ? PANEL_BORDER_COLOR : (hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        drawSurface(x, y, layout.runeSlotWidth, layout.runeSlotHeight, hovered ? RUNE_HOVER_BG_COLOR : SLOT_BG_COLOR, border);
        if (futureType) {
            int markSize = Math.max(3, Math.min(scaled(8, 3), layout.runeSlotWidth - 4));
            int markX = x + (layout.runeSlotWidth - markSize) / 2;
            int markY = y + layout.runeSlotHeight / 2;
            drawRect(markX, markY, markX + markSize, markY + 1, MUTED_TEXT_COLOR);
            return;
        }
        RuneInventory.Entry rune = getRuneForVisualSlot(visualIndex);
        if (rune == null) return;
        drawRankCorners(x, y, layout.runeSlotWidth, layout.runeSlotHeight, getRankColor(rune.layer));
        if (rune.cardIndex >= 0 && rune.cardIndex < CARD_ICONS.length) {
            String type = rune.cardIndex < RUNE_TYPES.length ? RUNE_TYPES[rune.cardIndex] : "Ресурс";
            drawRuneIcon(CARD_ICONS[rune.cardIndex], x, y, layout.runeSlotWidth, layout.runeSlotHeight, type);
            drawSurface(x, y, layout.runeSlotWidth, layout.runeSlotHeight, 0, border);
            drawRankCorners(x, y, layout.runeSlotWidth, layout.runeSlotHeight, getRankColor(rune.layer));
        }
    }

    private void drawEarthRuneSlot(int x, int y, int slot, int mouseX, int mouseY) {
        EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(slot);
        if (definition == null) return;
        boolean hovered = mouseX >= x && mouseX < x + layout.runeSlotWidth
                && mouseY >= y && mouseY < y + layout.runeSlotHeight;
        drawSurface(x, y, layout.runeSlotWidth, layout.runeSlotHeight, hovered ? RUNE_HOVER_BG_COLOR : SLOT_BG_COLOR,
                hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        RuneInventory.Entry owned = inventory.get(slot);
        if (owned == null) return;
        drawCustomRuneIcon(definition, x, y, layout.runeSlotWidth, layout.runeSlotHeight);
        drawSurface(x, y, layout.runeSlotWidth, layout.runeSlotHeight, 0,
                hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        drawRankCorners(x, y, layout.runeSlotWidth, layout.runeSlotHeight, getRankColor(owned.layer));
    }

    private void drawCustomRuneIcon(EarthRuneCatalog.Definition definition, int x, int y, int w, int h) {
        ResourceLocation texture = new ResourceLocation(definition.iconTexture);
        int badgeSize = Math.max(3, Math.min(8, Math.min(w, h) / 5));
        int padding = Math.max(1, Math.round(Math.min(w, h) * 0.07F));
        int size = Math.max(1, Math.min(w - padding * 2, h - padding * 2));
        int drawX = x + (w - size) / 2;
        int drawY = y + (h - size) / 2;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(texture);
        drawScaledCustomSizeModalRect(drawX, drawY, 0, 0,
                128, 128, size, size, 128, 128);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawRuneTypeBadge(definition.type, x + w - 2, y + h - badgeSize - 2, badgeSize, w - 4);
    }

    private void drawRuneIcon(ItemStack icon, int x, int y, int w, int h, String type) {
        int badgeSize = Math.max(3, Math.min(8, Math.min(w, h) / 5));
        int iconSize = Math.max(1, Math.min(24, Math.min(w - 4, h - badgeSize - 3)));
        int iconX = x + (w - iconSize) / 2;
        int iconY = y + Math.max(1, Math.min((h - iconSize) / 2, h - badgeSize - iconSize - 3));
        GlStateManager.pushMatrix();
        GlStateManager.translate(iconX, iconY, 0);
        GlStateManager.scale(iconSize / 16.0F, iconSize / 16.0F, 1);
        GlStateManager.enableRescaleNormal();
        renderItem.renderItemIntoGUI(icon, 0, 0);
        GlStateManager.popMatrix();
        drawRuneTypeBadge(type, x + w - 2, y + h - badgeSize - 2, badgeSize, w - 4);
    }

    private void drawRuneTypeBadge(String type, int x, int y, int size, int availableWidth) {
        boolean hybrid = "Клик / Яд".equals(type);
        int glyphSize = hybrid ? Math.max(2, Math.min(size, (availableWidth - 1) / 2)) : size;
        int badgeWidth = hybrid ? glyphSize * 2 + 1 : glyphSize;
        x -= badgeWidth;
        drawRect(x - 1, y - 1, x + badgeWidth + 1, y + glyphSize + 1, TYPE_BADGE_BG_COLOR);
        drawTypeGlyph(hybrid ? "Клик" : type, x, y, glyphSize);
        if (hybrid) drawTypeGlyph("Яд", x + glyphSize + 1, y, glyphSize);
    }

    private void drawTypeGlyph(String type, int x, int y, int size) {
        String[] rows;
        int color;
        if ("Клик".equals(type)) {
            rows = new String[]{"0001100", "0011000", "0110000", "1111110", "0001100", "0011000", "0110000"};
            color = CLICK_TYPE_COLOR;
        } else if ("Яд".equals(type)) {
            rows = new String[]{"0001000", "0011100", "0111110", "1111111", "1111111", "0111110", "0011100"};
            color = POISON_TYPE_COLOR;
        } else if ("Усиление".equals(type)) {
            rows = new String[]{"0001000", "0011100", "0111110", "1111111", "0001000", "0001000", "0001000"};
            color = BOOST_TYPE_COLOR;
        } else if ("Вода".equals(type)) {
            rows = new String[]{"0001000", "0001000", "0011100", "0011100", "0111110", "0111110", "0011100"};
            color = WATER_TYPE_COLOR;
        } else {
            rows = new String[]{"0011100", "0111110", "1111111", "1101011", "1111111", "0111110", "0011100"};
            color = RESOURCE_TYPE_COLOR;
        }
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                if (rows[row].charAt(col) != '1') continue;
                int left = x + col * size / 7;
                int right = x + (col + 1) * size / 7;
                int top = y + row * size / 7;
                int bottom = y + (row + 1) * size / 7;
                if (right > left && bottom > top) drawRect(left, top, right, bottom, color);
            }
        }
    }

    private void drawRankCorners(int x, int y, int w, int h, int color) {
        int length = Math.max(3, Math.min(scaled(8, 4), w / 5));
        int thickness = 1;

        drawRect(x + 2, y + 2, x + 2 + length, y + 2 + thickness, color);
        drawRect(x + 2, y + 2, x + 2 + thickness, y + 2 + length, color);
    }

    private int[] getRuneSlotPosition(int visualIndex) {
        return layout.runeSlotPosition(visualIndex);
    }

    private RuneInventory.Entry getRuneForVisualSlot(int visualIndex) {
        return activeDeck == 0 ? inventory.get(visualIndex) : inventory.getBoost(visualIndex);
    }

    private void drawRuneTooltip(int mouseX, int mouseY) {
        for (int i = 0; i < VISIBLE_RUNE_SLOTS; i++) {
            if (!matchesSearch(i)) continue;
            int[] pos = getRuneSlotPosition(i);
            int x = pos[0];
            int y = pos[1];
            if (mouseX < x || mouseX >= x + layout.runeSlotWidth
                    || mouseY < y || mouseY >= y + layout.runeSlotHeight) continue;

            if (activeDeck == 0) {
                if (inventory.get(i) == null) return;
                EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(i);
                List<String> tooltip = new ArrayList<>();
                tooltip.add("§b" + definition.name);
                tooltip.add("§7Тип: §f" + definition.type);
                drawCanvasTooltip(tooltip, mouseX, mouseY);
                return;
            }
            RuneInventory.Entry rune = getRuneForVisualSlot(i);
            if (rune == null || rune.cardIndex < 0 || rune.cardIndex >= CARD_NAMES.length) return;
            int cardIndex = rune.cardIndex;

            List<String> tooltip = new ArrayList<>();
            tooltip.add("§b" + CARD_NAMES[cardIndex]);
            tooltip.add("");
            tooltip.add("§7Ранг: §f" + getRankName(rune.layer));
            tooltip.add("§7Тип: §f" + (cardIndex < RUNE_TYPES.length ? RUNE_TYPES[cardIndex] : "Неизвестно"));
            tooltip.add("§7Эффект: §a+" + CARD_STATS[cardIndex]);
            tooltip.add("§7Усиление: §f0");
            drawCanvasTooltip(tooltip, mouseX, mouseY);
            return;
        }
    }

    private void drawElementTooltip(int mouseX, int mouseY) {
        for (GuiButton guiButton : buttonList) {
            if (!(guiButton instanceof ElementButton)) continue;
            ElementButton button = (ElementButton) guiButton;
            if (mouseX < button.x || mouseX >= button.x + button.width
                    || mouseY < button.y || mouseY >= button.y + button.height) continue;

            List<String> tooltip = new ArrayList<>();
            tooltip.add("§b" + button.getElementName());
            if (!button.isActiveElement()) {
                tooltip.add("§7Нажмите, чтобы выбрать статус");
            }
            drawCanvasTooltip(tooltip, mouseX, mouseY);
            return;
        }
    }

    private void drawNavigationTooltip(int mouseX, int mouseY) {
        for (GuiButton button : buttonList) {
            if (button.id != NAV_RUNES_ID) continue;
            if (mouseX < button.x || mouseX >= button.x + button.width
                    || mouseY < button.y || mouseY >= button.y + button.height) continue;
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§bРуны");
            tooltip.add("§7Статусы и коллекция рун");
            drawCanvasTooltip(tooltip, mouseX, mouseY);
            return;
        }
    }

    private void drawFlyingRune(FlyingCard card) {
        int x = (int) card.getX();
        int y = (int) card.getY();
        int w = Math.max(14, Math.min(scaled(38, 22), layout.runeSlotWidth));
        int h = Math.max(18, Math.min(scaled(46, 28), layout.runeSlotHeight));

        drawSurface(x - w / 2, y - h / 2, w, h, SLOT_BG_COLOR, SLOT_BORDER_COLOR);
        drawRankCorners(x - w / 2, y - h / 2, w, h, card.color);

        if (activeDeck == 0) {
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(card.cardIndex);
            if (definition != null) {
                drawCustomRuneIcon(definition, x - w / 2, y - h / 2, w, h);
            }
            return;
        }
        if (card.cardIndex >= 0 && card.cardIndex < CARD_ICONS.length && w >= 18 && h >= 18) {
            renderItem.renderItemIntoGUI(CARD_ICONS[card.cardIndex], x - 8, y - 8);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) searchField.updateCursorCounter();
        for (FlyingCard card : new ArrayList<>(flyingCards)) {
            card.update();
            if (card.isDone()) flyingCards.remove(card);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateLayout();
        drawRect(0, 0, width, height, OVERLAY_COLOR);
        mouseX = layout.canvasMouseX(mouseX);
        mouseY = layout.canvasMouseY(mouseY);
        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1);

        drawPanel(layout.leftPanelX, layout.leftPanelY, layout.leftPanelWidth, layout.leftPanelHeight);
        drawPanel(layout.runePanelX, layout.runePanelY, layout.runePanelWidth, layout.runePanelHeight);
        drawSurface(layout.navPanelX, layout.navPanelY, layout.navPanelWidth, layout.navPanelHeight, PANEL_BG_COLOR, PANEL_BORDER_COLOR);

        drawLeftPanelContent();
        drawCenterPanelContent(mouseX, mouseY);
        drawRunePanelContent(mouseX, mouseY);

        for (FlyingCard card : flyingCards) {
            drawFlyingRune(card);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawElementTooltip(mouseX, mouseY);
        drawNavigationTooltip(mouseX, mouseY);
        drawRuneTooltip(mouseX, mouseY);
        // Tooltips use the logical canvas bounds, too.
        if (equipmentTooltip != null) drawCanvasTooltip(java.util.Collections.singletonList(equipmentTooltip), mouseX, mouseY);
        GlStateManager.popMatrix();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        if (guiBlur != null && mc.entityRenderer.getShaderGroup() == guiBlur) {
            mc.entityRenderer.stopUseShader();
        }
        guiBlur = null;
    }

    private int getRankColor(int layer) {
        if (layer == 2) return SILVER_COLOR;
        if (layer == 3) return GOLD_COLOR;
        return NORMAL_COLOR;
    }

    private String getRankName(int layer) {
        if (layer == 2) return "Серебряная";
        if (layer == 3) return "Золотая";
        return "Обычная";
    }

    public void setBalance(int balance) {
        ClientPlayerStats.setCoins(balance);
    }

    public void setDecks(List<String> names, int activeDeck) {
        deckNames.clear();
        deckNames.addAll(names);
        this.activeDeck = activeDeck;
        rebuildControls();

        CustomGuiMod.logger.info("Elements updated on client: " + deckNames.size()
                + ", selected=" + activeDeck);
    }

    public void addCardFromServer(int slot, int cardIndex, int layer, boolean animate) {
        if (slot < 0 || slot >= STORAGE_SLOT_COUNT) return;

        inventory.set(slot, cardIndex, layer);
        updatePurchaseAvailability();

        if (animate && (activeDeck == 0 ? slot < VISIBLE_RUNE_SLOTS : cardIndex >= 0 && cardIndex < CARD_ICONS.length)) {
            int[] pos = getRuneSlotPosition(activeDeck == 0 ? slot : cardIndex);
            flyingCards.add(new FlyingCard(
                    layout.runePanelX + layout.runePanelWidth / 2.0D,
                    layout.purchaseTop,
                    pos[0] + layout.runeSlotWidth / 2.0D,
                    pos[1] + layout.runeSlotHeight / 2.0D,
                    activeDeck == 0 ? slot : cardIndex,
                    getRankColor(layer)
            ));
        }

        CustomGuiMod.logger.info("Добавлена руна от сервера: слот " + slot + ", индекс " + cardIndex
                + " (анимация: " + animate + ")");
    }

    public void clearCards() {
        inventory.clear();
        updatePurchaseAvailability();
        flyingCards.clear();
        CustomGuiMod.logger.info("Руны очищены на клиенте");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
