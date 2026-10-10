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

public class CardsGuiScreen extends GuiScreen {

    private static final int STORAGE_SLOT_COUNT = RuneInventory.LEGACY_RECORD_CAPACITY;
    private static final int VISIBLE_RUNE_SLOTS = RuneInventory.BOOST_TYPE_COUNT;

    private static final int GUI_MAX_WIDTH = 1240;
    private static final int GUI_MAX_HEIGHT = 620;
    private static final int GUI_OUTER_MARGIN = 10;

    private static final int OVERLAY_COLOR = 0xB8000000;
    private static final int PANEL_BG_COLOR = 0xB8080808;
    private static final int PANEL_BG_SOFT = 0x40080808;
    private static final int PANEL_BORDER_COLOR = 0xFF292B30;
    private static final int PANEL_LINE_COLOR = 0xFF202226;
    private static final int SLOT_BG_COLOR = 0x40080808;
    private static final int SLOT_BORDER_COLOR = 0xFF282A30;
    private static final int SLOT_HOVER_COLOR = 0xC0586067;
    private static final int SELECTED_COLOR = 0xFF315F9B;
    private static final int SELECTED_BG_COLOR = 0xA0152945;
    private static final int BUTTON_BG_COLOR = 0x40080808;
    private static final int DISABLED_BG_COLOR = 0x20080808;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HEADING_TEXT_COLOR = 0xFFAAAAAA;
    private static final int MUTED_TEXT_COLOR = 0xFF686868;
    private static final int DISABLED_TEXT_COLOR = 0xFF909090;
    private static final int LINK_TEXT_COLOR = 0xFF4488DD;
    private static final int STAT_ICON_SIZE = 10;
    private static final int STAT_ICON_GAP = 5;
    private static final int LOCK_COLOR = 0xFFFFFFFF;

    private static final int NORMAL_COLOR = 0xFF2D7FD6;
    private static final int SILVER_COLOR = 0xFFC9CDD2;
    private static final int GOLD_COLOR = 0xFFFFC83D;

    private static final int BUY_BUTTON_ID = 0;
    private static final int AMOUNT_BUTTON_START_ID = 1;
    private static final int UPGRADE_PLACEHOLDER_ID = 2;
    private static final int RUNE_UNIT_PRICE = 100;
    private static final int ELEMENT_BUTTON_START_ID = 100;
    private static final int MAX_ELEMENTS = 8;

    private static final int NAV_RUNES_ID = 200;
    private static final int NAV_PLACEHOLDER_START_ID = 201;
    private static final int CLOSE_BUTTON_ID = 299;

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

    private float layoutScale = 1.0F;
    private boolean compactLayout;

    private int guiX;
    private int guiY;
    private int guiWidth;
    private int guiHeight;

    private int mainX;
    private int mainY;
    private int mainWidth;
    private int mainHeight;

    private int leftPanelX;
    private int leftPanelY;
    private int leftPanelWidth;
    private int leftPanelHeight;

    private int centerPanelX;
    private int centerPanelY;
    private int centerPanelWidth;
    private int centerPanelHeight;

    private int runePanelX;
    private int runePanelY;
    private int runePanelWidth;
    private int runePanelHeight;

    private int navPanelX;
    private int navPanelY;
    private int navPanelWidth;
    private int navPanelHeight;

    private int purchaseTop;
    private int purchasePanelY;
    private static final int PURCHASE_PANEL_HEIGHT = 102;
    private int runeColumns = 10;
    private int runeRows = 3;
    private int runeSlotWidth;
    private int runeSlotHeight;
    private int runeSlotGap;
    private int runeGridX;
    private int runeGridY;

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
        return (int) Math.floor((mouseX - canvasX) / canvasScale);
    }

    int canvasMouseY(int mouseY) {
        return (int) Math.floor((mouseY - canvasY) / canvasScale);
    }

    boolean runeMatchesSearch(int slot) {
        return matchesSearch(slot);
    }

    private void drawCanvasTooltip(List<String> lines, int x, int y) {
        int oldWidth = width, oldHeight = height;
        width = 1280; height = 669;
        try { drawHoveringText(lines, x, y); }
        finally { width = oldWidth; height = oldHeight; }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        x = (int) Math.floor((x - canvasX) / canvasScale);
        y = (int) Math.floor((y - canvasY) / canvasScale);
        searchField.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (!searchField.textboxKeyTyped(character, key)) super.keyTyped(character, key);
    }

    private static class FlyingCard {
        double startX;
        double startY;
        double endX;
        double endY;
        double progress;
        int cardIndex;
        int color;

        FlyingCard(double startX, double startY, double endX, double endY,
                   int cardIndex, int color) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.cardIndex = cardIndex;
            this.color = color;
        }

        void update() {
            progress += 0.12D;
            if (progress > 1.0D) progress = 1.0D;
        }

        double getX() {
            return startX + (endX - startX) * progress;
        }

        double getY() {
            return startY + (endY - startY) * progress;
        }

        boolean isDone() {
            return progress >= 1.0D;
        }
    }

    private static class PanelButton extends GuiButton {
        private boolean selected;

        PanelButton(int id, int x, int y, int width, int height, String text) {
            super(id, x, y, width, height, text);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int border = selected ? SELECTED_COLOR : (hovered && enabled ? SLOT_HOVER_COLOR : PANEL_BORDER_COLOR);
            int background = selected ? SELECTED_BG_COLOR : (enabled ? BUTTON_BG_COLOR : DISABLED_BG_COLOR);
            int textColor = id == -1 || enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR;

            if (id != CLOSE_BUTTON_ID) drawSurface(x, y, width, height, background, border);

            net.minecraft.client.gui.FontRenderer font = GuiFontRenderer.get(mc);

            if (id == AMOUNT_BUTTON_START_ID) {
                int textWidth = font.getStringWidth(displayString);
                float textScale = Math.min(
                        1.0F,
                        (width - 4) / (float) Math.max(1, textWidth)
                );

                GlStateManager.pushMatrix();
                GlStateManager.translate(
                        x + width / 2.0F,
                        y + (height - font.FONT_HEIGHT * textScale) / 2.0F,
                        0.0F
                );
                GlStateManager.scale(textScale, textScale, 1.0F);
                font.drawString(
                        displayString,
                        -textWidth / 2.0F,
                        0.0F,
                        textColor,
                        false
                );
                GlStateManager.popMatrix();
                return;
            }

            String[] lines = displayString.split("\n");
            int lineHeight = font.FONT_HEIGHT;
            int textY = y + (height - lines.length * lineHeight) / 2;

            for (String line : lines) {
                String label = font.trimStringToWidth(
                        line,
                        Math.max(1, width - 6)
                );
                drawCenteredString(
                        font,
                        label,
                        x + width / 2,
                        textY,
                        textColor
                );
                textY += lineHeight;
            }
        }
    }

    private static class ElementButton extends PanelButton {
        private final String elementName;
        private final boolean active;

        ElementButton(int id, int x, int y, int width, int height,
                      String elementName, boolean active) {
            super(id, x, y, width, height, elementName);
            this.elementName = elementName;
            this.active = active;
            setSelected(active);
        }

        String getElementName() {
            return elementName;
        }

        boolean isActiveElement() {
            return active;
        }
    }

    private static class NavButton extends GuiButton {
        private final ItemStack icon;
        private final boolean selected;

        NavButton(int id, int x, int y, int width, int height,
                  ItemStack icon, boolean selected, boolean enabled) {
            super(id, x, y, width, height, "");
            this.icon = icon;
            this.selected = selected;
            this.enabled = enabled;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!visible) return;

            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            int border = selected ? SELECTED_COLOR : (hovered && enabled ? SLOT_HOVER_COLOR : PANEL_BORDER_COLOR);
            int background = selected ? SELECTED_BG_COLOR : (enabled ? BUTTON_BG_COLOR : DISABLED_BG_COLOR);

            if (hovered) drawRect(x, y, x + width, y + height, BUTTON_BG_COLOR);
            if (selected) drawRect(x + width - 1, y, x + width, y + height, SELECTED_COLOR);

            if (id == NAV_RUNES_ID) {
                int cx = x + width / 2, cy = y + height / 2;
                int stone = 0xFF487C86, edge = 0xFF87B6BF, rune = 0xFFD0F4F3;
                drawRect(cx - 7, cy - 8, cx + 7, cy + 8, edge);
                drawRect(cx - 8, cy - 6, cx + 8, cy + 6, edge);
                drawRect(cx - 6, cy - 7, cx + 6, cy + 7, stone);
                drawRect(cx - 7, cy - 5, cx + 7, cy + 5, stone);
                drawRect(cx - 1, cy - 5, cx + 1, cy + 6, rune);
                drawRect(cx + 1, cy - 5, cx + 4, cy - 3, rune);
                drawRect(cx + 3, cy - 3, cx + 5, cy - 1, rune);
                drawRect(cx + 1, cy - 1, cx + 4, cy + 1, rune);
                drawRect(cx - 4, cy + 1, cx - 1, cy + 3, rune);
            }

        }
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
        return Math.max(minimum, Math.round(base * layoutScale));
    }

    // Use one logical canvas so panel proportions and hit targets scale together.
    private float canvasScale;
    private int canvasX, canvasY;
    private String equipmentTooltip;

    private void updateLayout() {
        canvasScale = Math.min(width / 1280.0F, height / 669.0F);
        canvasX = Math.round((width - 1280 * canvasScale) / 2);
        canvasY = Math.round((height - 669 * canvasScale) / 2);
        layoutScale = 1.0F;
        compactLayout = false;
        guiX = mainX = 10;
        guiY = mainY = 10;
        guiWidth = 1260;
        guiHeight = mainHeight = 624;
        mainWidth = 1224;
        leftPanelX = 10; leftPanelY = 10;
        leftPanelWidth = 300; leftPanelHeight = 52;
        centerPanelX = 320; centerPanelY = 10;
        centerPanelWidth = 302; centerPanelHeight = 458;
        runePanelX = 632; runePanelY = 10;
        runePanelWidth = 603; runePanelHeight = 624;
        navPanelWidth = 34;
        navPanelHeight = (int) Math.ceil((height - canvasY) / canvasScale) - 26;
        // Anchor the navigation strip to the actual viewport, including wide windows.
        navPanelX = (int) Math.ceil((width - canvasX) / canvasScale) - navPanelWidth;
        navPanelY = 10;
        updateRuneGridLayout();
    }
    private void updateRuneGridLayout() {
        int pad = scaled(8, 4);
        int topReserved = 83;
        int availableWidth = Math.max(1, runePanelWidth - pad * 2);
        runeSlotGap = scaled(5, 2);
        runeColumns = 10;
        runeRows = (VISIBLE_RUNE_SLOTS + runeColumns - 1) / runeColumns;
        int sizeByWidth = (availableWidth - runeSlotGap * (runeColumns - 1)) / runeColumns;
        int maxSlot = compactLayout ? scaled(42, 14) : scaled(50, 18);
        runeSlotWidth = Math.max(1, Math.min(sizeByWidth, maxSlot));
        runeSlotHeight = runeSlotWidth;
        runeGridX = runePanelX + pad;
        runeGridY = runePanelY + topReserved;

        int contentHeight = topReserved
                + runeRows * runeSlotHeight
                + (runeRows - 1) * runeSlotGap
                + scaled(29, 20);
        runePanelHeight = contentHeight;
        purchasePanelY = runePanelY + runePanelHeight + 10;
    }

    private void rebuildControls() {
        buttonList.clear();
        updateLayout();

        int pad = scaled(7, 3);
        int buttonX = runePanelX + pad;
        int buttonWidth = runePanelWidth - pad * 2;
        int amountHeight = 24;
        int buyHeight = 36;

        int buyY = purchasePanelY + 28;
        int amountY = buyY + buyHeight + 6;
        purchaseTop = buyY;
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
                runePanelX + 16, runePanelY + 30, runePanelWidth - 32, 19);
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

        int x = runePanelX + scaled(8, 4);
        int y = runePanelY + 57;
        int gap = scaled(3, 2);
        int availableRight = runePanelX + runePanelWidth - scaled(8, 4);

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
        int buttonSize = navPanelWidth - pad * 2;
        int x = navPanelX + pad;
        int y = navPanelY + pad;
        int gap = 10;

        buttonList.add(new NavButton(
                NAV_RUNES_ID, x, y, buttonSize, buttonSize,
                new ItemStack(Items.PRISMARINE_SHARD), true, true
        ));

        int closeSize = buttonSize;
        int closeY = navPanelY + navPanelHeight - pad - closeSize;
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

    // Fill and stroke separately: a translucent border must not darken the whole interior.
    private static final java.nio.FloatBuffer SURFACE_TRANSFORM = org.lwjgl.BufferUtils.createFloatBuffer(16);

    private static void drawSurface(int x, int y, int w, int h, int background, int border) {
        if (w < 6 || h < 6) return;
        GlStateManager.pushMatrix();
        java.nio.FloatBuffer transform = SURFACE_TRANSFORM;
        transform.clear();
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX, transform);
        float guiScale = new net.minecraft.client.gui.ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        float pixels = Math.max(0.01F, Math.abs(transform.get(0)) * guiScale);
        float originX = transform.get(12) * guiScale;
        float originY = transform.get(13) * guiScale;
        float snappedX = (Math.round(originX + x * pixels) - originX) / pixels;
        float snappedY = (Math.round(originY + y * pixels) - originY) / pixels;
        GlStateManager.translate(snappedX, snappedY, 0);
        GlStateManager.scale(1.0F / pixels, 1.0F / pixels, 1);
        int right = Math.round(w * pixels), bottom = Math.round(h * pixels);
        int stroke = Math.max(1, Math.round(1.25F * pixels));
        boolean itemSlot = w == h && w >= 40 && w <= 60;
        int shoulder = Math.max(stroke + 1,
                Math.round((itemSlot ? 4 : (w > 200 && h > 30) ? 4 : 3) * pixels));
        drawRect(stroke, shoulder, right - stroke, bottom - shoulder, background);
        drawRect(shoulder, stroke, right - shoulder, shoulder, background);
        drawRect(shoulder, bottom - shoulder, right - shoulder, bottom - stroke, background);
        drawRect(shoulder, 0, right - shoulder, stroke, border);
        drawRect(shoulder, bottom - stroke, right - shoulder, bottom, border);
        drawRect(0, shoulder, stroke, bottom - shoulder, border);
        drawRect(right - stroke, shoulder, right, bottom - shoulder, border);
        drawRect(shoulder - stroke, 0, shoulder, shoulder, border);
        drawRect(0, shoulder - stroke, shoulder, shoulder, border);
        drawRect(right - shoulder, 0, right - shoulder + stroke, shoulder, border);
        drawRect(right - shoulder, shoulder - stroke, right, shoulder, border);
        drawRect(0, bottom - shoulder, shoulder, bottom - shoulder + stroke, border);
        drawRect(shoulder - stroke, bottom - shoulder, shoulder, bottom, border);
        drawRect(right - shoulder, bottom - shoulder, right, bottom - shoulder + stroke, border);
        drawRect(right - shoulder, bottom - shoulder, right - shoulder + stroke, bottom, border);
        GlStateManager.popMatrix();
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
        int x = leftPanelX + pad;
        int textW = Math.max(10, leftPanelWidth - pad * 2);
        int y = leftPanelY + pad;

        drawHeadingString("СТАТИСТИКА", x, y, textW, HEADING_TEXT_COLOR, true);
        y += scaled(21, 16);
        double baseDamage = mc.player == null ? 1.0D
                : mc.player.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        double damage = baseDamage + ClientPlayerStats.getTotalDamage();
        String damageText = damage == Math.rint(damage) ? String.valueOf((long) damage)
                : String.format(java.util.Locale.ROOT, "%.2f", damage).replaceAll("0+$", "").replaceAll("\\.$", "");
        drawStatLine(x, y, "Урон за клик", damageText, textW);

        int resourcesY = leftPanelY + leftPanelHeight + 11;
        drawPanel(leftPanelX, resourcesY, leftPanelWidth, 68);
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
            drawRect(x + 2, y + 1, x + 8, y + 10, 0xFFE6B845);
            drawRect(x + 1, y + 3, x + 9, y + 8, 0xFFE6B845);
            drawRect(x + 3, y + 3, x + 5, y + 7, 0xFFFFE59A);
        } else if ("Кристаллы".equals(name)) {
            drawRect(x + 3, y + 1, x + 7, y + 10, 0xFF5EDAD1);
            drawRect(x + 1, y + 4, x + 9, y + 7, 0xFF5EDAD1);
            drawRect(x + 2, y + 2, x + 8, y + 9, 0xFF5EDAD1);
            drawRect(x + 3, y + 2, x + 5, y + 6, 0xFFBAFFF6);
        } else {
            for (int i = 0; i < 6; i++)
                drawRect(x + 7 - i, y + 1 + i, x + 9 - i, y + 3 + i, 0xFFD4D4D4);
            drawRect(x + 1, y + 6, x + 5, y + 8, 0xFF858585);
            drawRect(x, y + 8, x + 2, y + 10, 0xFFAAAAAA);
        }
    }

    private void drawCenterPanelContent(int mouseX, int mouseY) {
        equipmentTooltip = null;
        int x = centerPanelX, y = centerPanelY;
        drawPanel(x, y, centerPanelWidth, 63);
        // Progression and donation data will replace these presentation placeholders.
        fontRenderer.drawString("#1", x + 8, y + 9, TEXT_COLOR);
        int rankSeparatorX = x + 8 + fontRenderer.getStringWidth("#1") + 7;
        drawRect(rankSeparatorX, y + 10, rankSeparatorX + 1, y + 20, MUTED_TEXT_COLOR);
        drawFittedString("Моб #1", rankSeparatorX + 8, y + 9,
                x + centerPanelWidth - 8 - rankSeparatorX - 8, TEXT_COLOR, true);
        drawFittedString("Базовая", x + 8, y + 29, 303, LINK_TEXT_COLOR, false);
        drawFittedString("—", x + 8, y + 47, 24, MUTED_TEXT_COLOR, false);
        int separatorX = x + 8 + fontRenderer.getStringWidth("—") + 7;
        drawRect(separatorX, y + 48, separatorX + 1, y + 58, MUTED_TEXT_COLOR);
        int nameX = separatorX + 8;
        drawFittedString(mc.player == null ? "Игрок" : mc.player.getName(),
                nameX, y + 47, x + centerPanelWidth - 8 - nameX, TEXT_COLOR, true);

        int top = y + 74;
        drawPanel(x, top, centerPanelWidth, centerPanelHeight - 74);
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
        drawLock(x + size / 2 - 6, y + size / 2 - 6, 0xFF585858);
        if (hover) equipmentTooltip = name + " — недоступно";
    }
    private void drawSmallLock(int x, int y) {
        drawRect(x + 2, y, x + 6, y + 1, LOCK_COLOR);
        drawRect(x + 1, y + 1, x + 3, y + 4, LOCK_COLOR);
        drawRect(x + 5, y + 1, x + 7, y + 4, LOCK_COLOR);
        drawRect(x, y + 4, x + 8, y + 9, LOCK_COLOR);
        drawRect(x + 3, y + 5, x + 5, y + 7, 0xFF111111);
    }

    private void drawLock(int x, int y, int color) {
        // Stepped arch and compact square keyhole measured from the reference.
        drawRect(x + 3, y, x + 9, y + 1, color);
        drawRect(x + 2, y + 1, x + 10, y + 3, color);
        drawRect(x + 1, y + 3, x + 4, y + 6, color);
        drawRect(x + 8, y + 3, x + 11, y + 6, color);
        drawRect(x + 4, y + 2, x + 8, y + 6, 0xFF101112);
        drawRect(x - 1, y + 6, x + 13, y + 12, color);
        drawRect(x + 4, y + 8, x + 8, y + 11, 0xFF101112);
    }
    private void drawRunePanelContent(int mouseX, int mouseY) {
        int pad = scaled(8, 4);
        int titleX = runePanelX + pad;
        int titleY = runePanelY + pad;
        drawHeadingString("РУНЫ", titleX, titleY, runePanelWidth / 2, HEADING_TEXT_COLOR, true);
        String count = activeDeck == 0
                ? inventory.ownedCatalogSlotCount() + " из " + EarthRuneCatalog.size()
                : inventory.occupiedTypeCount(CARD_NAMES.length) + " из " + CARD_NAMES.length;
        fontRenderer.drawString(count,
                runePanelX + runePanelWidth - pad - fontRenderer.getStringWidth(count), titleY, MUTED_TEXT_COLOR);
        drawSurface(titleX, runePanelY + 30, runePanelWidth - pad * 2, 19,
                BUTTON_BG_COLOR, searchField.isFocused() ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 4, 0);
        searchField.drawTextBox();
        GlStateManager.popMatrix();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            drawFittedString("Поиск...", titleX + 8, runePanelY + 34,
                    runePanelWidth - 24, MUTED_TEXT_COLOR, false);
        }
        drawPanel(runePanelX, purchasePanelY, runePanelWidth, PURCHASE_PANEL_HEIGHT);
        drawHeadingString("ПОКУПКА РУН", titleX, purchasePanelY + 8, 300, HEADING_TEXT_COLOR, false);
        drawRuneActionHints(titleX, runePanelY + runePanelHeight - 17,
                runePanelWidth - pad * 2);
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
            fontRenderer.drawString(labels[i], offset + 13, 0, 0xFF999999);
            offset += 13 + fontRenderer.getStringWidth(labels[i]) + 10;
        }
        GlStateManager.popMatrix();
    }

    private void drawMouseHintIcon(int x, int y, int button) {
        int white = 0xFFFFFFFF, dark = 0xFF161616, red = 0xFFFF4444;
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
        boolean hovered = mouseX >= x && mouseX < x + runeSlotWidth
                && mouseY >= y && mouseY < y + runeSlotHeight;
        boolean futureType = visualIndex >= CARD_NAMES.length;
        int border = futureType ? PANEL_BORDER_COLOR : (hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        drawSurface(x, y, runeSlotWidth, runeSlotHeight, hovered ? 0x5024282C : SLOT_BG_COLOR, border);
        if (futureType) {
            int markSize = Math.max(3, Math.min(scaled(8, 3), runeSlotWidth - 4));
            int markX = x + (runeSlotWidth - markSize) / 2;
            int markY = y + runeSlotHeight / 2;
            drawRect(markX, markY, markX + markSize, markY + 1, MUTED_TEXT_COLOR);
            return;
        }
        RuneInventory.Entry rune = getRuneForVisualSlot(visualIndex);
        if (rune == null) return;
        drawRankCorners(x, y, runeSlotWidth, runeSlotHeight, getRankColor(rune.layer));
        if (rune.cardIndex >= 0 && rune.cardIndex < CARD_ICONS.length) {
            String type = rune.cardIndex < RUNE_TYPES.length ? RUNE_TYPES[rune.cardIndex] : "Ресурс";
            drawRuneIcon(CARD_ICONS[rune.cardIndex], x, y, runeSlotWidth, runeSlotHeight, type);
            drawSurface(x, y, runeSlotWidth, runeSlotHeight, 0, border);
            drawRankCorners(x, y, runeSlotWidth, runeSlotHeight, getRankColor(rune.layer));
        }
    }

    private void drawEarthRuneSlot(int x, int y, int slot, int mouseX, int mouseY) {
        EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(slot);
        if (definition == null) return;
        boolean hovered = mouseX >= x && mouseX < x + runeSlotWidth
                && mouseY >= y && mouseY < y + runeSlotHeight;
        drawSurface(x, y, runeSlotWidth, runeSlotHeight, hovered ? 0x5024282C : SLOT_BG_COLOR,
                hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        RuneInventory.Entry owned = inventory.get(slot);
        if (owned == null) return;
        drawCustomRuneIcon(definition, x, y, runeSlotWidth, runeSlotHeight);
        drawSurface(x, y, runeSlotWidth, runeSlotHeight, 0,
                hovered ? SLOT_HOVER_COLOR : SLOT_BORDER_COLOR);
        drawRankCorners(x, y, runeSlotWidth, runeSlotHeight, getRankColor(owned.layer));
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
        drawRect(x - 1, y - 1, x + badgeWidth + 1, y + glyphSize + 1, 0xD012161B);
        drawTypeGlyph(hybrid ? "Клик" : type, x, y, glyphSize);
        if (hybrid) drawTypeGlyph("Яд", x + glyphSize + 1, y, glyphSize);
    }

    private void drawTypeGlyph(String type, int x, int y, int size) {
        String[] rows;
        int color;
        if ("Клик".equals(type)) {
            rows = new String[]{"0001100", "0011000", "0110000", "1111110", "0001100", "0011000", "0110000"};
            color = 0xFFFFD34E;
        } else if ("Яд".equals(type)) {
            rows = new String[]{"0001000", "0011100", "0111110", "1111111", "1111111", "0111110", "0011100"};
            color = 0xFF72E84A;
        } else if ("Усиление".equals(type)) {
            rows = new String[]{"0001000", "0011100", "0111110", "1111111", "0001000", "0001000", "0001000"};
            color = 0xFF73BEFF;
        } else if ("Вода".equals(type)) {
            rows = new String[]{"0001000", "0001000", "0011100", "0011100", "0111110", "0111110", "0011100"};
            color = 0xFF53CBED;
        } else {
            rows = new String[]{"0011100", "0111110", "1111111", "1101011", "1111111", "0111110", "0011100"};
            color = 0xFFFFB84D;
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
        int row = visualIndex / runeColumns;
        int col = visualIndex % runeColumns;
        return new int[]{
                runeGridX + col * (runeSlotWidth + runeSlotGap),
                runeGridY + row * (runeSlotHeight + runeSlotGap)
        };
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
            if (mouseX < x || mouseX >= x + runeSlotWidth
                    || mouseY < y || mouseY >= y + runeSlotHeight) continue;

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
        int w = Math.max(14, Math.min(scaled(38, 22), runeSlotWidth));
        int h = Math.max(18, Math.min(scaled(46, 28), runeSlotHeight));

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
        mouseX = (int) Math.floor((mouseX - canvasX) / canvasScale);
        mouseY = (int) Math.floor((mouseY - canvasY) / canvasScale);
        GlStateManager.pushMatrix();
        GlStateManager.translate(canvasX, canvasY, 0);
        GlStateManager.scale(canvasScale, canvasScale, 1);

        drawPanel(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);
        drawPanel(runePanelX, runePanelY, runePanelWidth, runePanelHeight);
        drawSurface(navPanelX, navPanelY, navPanelWidth, navPanelHeight, PANEL_BG_COLOR, PANEL_BORDER_COLOR);

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
                    runePanelX + runePanelWidth / 2.0D,
                    purchaseTop,
                    pos[0] + runeSlotWidth / 2.0D,
                    pos[1] + runeSlotHeight / 2.0D,
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
