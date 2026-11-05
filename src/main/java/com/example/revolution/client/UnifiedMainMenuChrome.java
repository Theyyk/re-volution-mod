package com.example.revolution.client;

import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.mechanic.MechanicUnlockGuiScreen;
import com.example.revolution.client.ui.UiAvailability;
import com.example.revolution.client.ui.UiMenuSection;
import com.example.revolution.number.LargeNumbers;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import static com.example.revolution.client.GuiTheme.*;

/**
 * Shared visual chrome for E-menu feature screens. The original first rune
 * collection is intentionally not altered. Existing server actions remain
 * inside each functional screen.
 */
public final class UnifiedMainMenuChrome {
    private static final int CLOSE_ID = 299;
    private static final UiMenuSection[] SECTIONS = UiMenuSection.values();
    private UnifiedMainMenuChrome() {}

    static int px(GuiLayout layout, int logicalX) {
        return layout.canvasX + Math.round(logicalX * layout.canvasScale);
    }

    static int py(GuiLayout layout, int logicalY) {
        return layout.canvasY + Math.round(logicalY * layout.canvasScale);
    }

    static int scaled(GuiLayout layout, int logical) {
        return Math.max(1, Math.round(logical * layout.canvasScale));
    }

    public static int contentLeft(int width, int height) {
        GuiLayout layout = geometry(width, height);
        return px(layout, layout.centerPanelX) + scaled(layout, 8);
    }

    public static int contentRight(int width, int height) {
        GuiLayout layout = geometry(width, height);
        return px(layout, layout.navPanelX) - scaled(layout, 12);
    }

    public static int contentTop(int width, int height) {
        GuiLayout layout = geometry(width, height);
        return py(layout, 65);
    }

    private static GuiLayout geometry(int width, int height) {
        GuiLayout layout = new GuiLayout(26);
        layout.update(width, height);
        return layout;
    }

    public static void installNavigation(List<GuiButton> buttons, int width, int height,
                                         UiMenuSection active) {
        GuiLayout layout = geometry(width, height);
        int x = px(layout, layout.navPanelX + 2);
        int size = scaled(layout, UnifiedMenuLayout.size(layout));
        for (int i = 0; i < SECTIONS.length; i++) {
            UiMenuSection section = SECTIONS[i];
            int logicalY = section == UiMenuSection.RUNES
                    ? layout.navPanelY + 2 : UnifiedMenuLayout.y(layout, i - 1);
            GuiControls.PanelButton b = new GuiControls.PanelButton(
                    section.buttonId(), x, py(layout, logicalY), size, size,
                    section.shortLabel());
            b.setSelected(section == active);
            applyState(b, section);
            buttons.add(b);
        }
        buttons.add(new GuiControls.PanelButton(
                CLOSE_ID, x, py(layout, layout.navPanelY + layout.navPanelHeight - 2 - UnifiedMenuLayout.size(layout)),
                size, size, "X"));
    }

    public static void refreshNavigation(List<GuiButton> buttons) {
        for (GuiButton b : buttons) {
            UiMenuSection section = UiMenuSection.fromButtonId(b.id);
            if (section != null) applyState(b, section);
        }
    }

    private static void applyState(GuiButton button, UiMenuSection section) {
        String unlockId = section.mechanicUnlockId();
        UiAvailability state = unlockId == null ? UiAvailability.AVAILABLE
                : ClientMechanicUnlocks.uiState(unlockId, UiAvailability.AVAILABLE);
        button.visible = state.isVisible();
        button.enabled = state.isInteractive();
    }

    /**
     * Returns true if the supplied button belongs to the common chrome,
     * even when disabled. Invoked from functional screens' actionPerformed.
     */
    public static boolean handleNavigation(int id, UiMenuSection active, Minecraft mc) {
        if (id == CLOSE_ID) {
            mc.displayGuiScreen(null);
            return true;
        }
        UiMenuSection target = UiMenuSection.fromButtonId(id);
        if (target == null) return false;
        if (target == active) return true;
        String unlockId = target.mechanicUnlockId();
        if (unlockId != null
                && !ClientMechanicUnlocks.uiState(unlockId, UiAvailability.AVAILABLE).isInteractive()) return true;
        switch (target) {
            case RUNES:
                ClientUiBridge.beginRuneOpen();
                mc.displayGuiScreen(null);
                break;
            case PETS: mc.displayGuiScreen(new PetsGuiScreen()); break;
            case UNIQUE_RUNES: mc.displayGuiScreen(new UniqueRunesGuiScreen()); break;
            case MECHANIC_UNLOCKS: mc.displayGuiScreen(new MechanicUnlockGuiScreen()); break;
            case ACHIEVEMENTS: mc.displayGuiScreen(new AchievementsGuiScreen()); break;
            default: break;
        }
        return true;
    }

    public static void draw(Minecraft mc, int width, int height, String sectionName) {
        GuiLayout layout = geometry(width, height);
        Gui.drawRect(0, 0, width, height, OVERLAY_COLOR);
        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1);
        GuiDrawing.drawSurface(layout.leftPanelX, layout.leftPanelY, 300, 68, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(layout.leftPanelX, 89, 300, 68, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(layout.leftPanelX, 168, 300, 84, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(layout.centerPanelX, 10,
                Math.max(200, layout.navPanelX - layout.centerPanelX - 10),
                624, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(layout.navPanelX, layout.navPanelY,
                layout.navPanelWidth, layout.navPanelHeight, PANEL_BG_COLOR, PANEL_BORDER_COLOR);

        FontRenderer font = GuiFontRenderer.get(mc);
        FontRenderer heading = GuiFontRenderer.heading(mc);
        heading.drawStringWithShadow("СТАТИСТИКА", 18, 18, HEADING_TEXT_COLOR);
        stat(font, 18, 43, 284, "Урон за клик",
                LargeNumbers.format(ClientPlayerStats.getResolvedClickDamage()));
        stat(font, 18, 56, 284, "Урон яда",
                LargeNumbers.format(ClientPlayerStats.getResolvedPoisonDamage()));

        heading.drawStringWithShadow("ВАЛЮТА", 18, 97, HEADING_TEXT_COLOR);
        stat(font, 18, 122, 284, "Монеты", LargeNumbers.format(ClientPlayerStats.getCoins()));
        stat(font, 18, 138, 284, "Кристаллы", LargeNumbers.format(ClientPlayerStats.getCrystals()));

        heading.drawStringWithShadow("РЕСУРСЫ РУН", 18, 176, HEADING_TEXT_COLOR);
        stat(font, 18, 201, 284, "Пыль рун", LargeNumbers.format(ClientPlayerStats.getRuneDust()));
        stat(font, 18, 217, 284, "Осколки рун", LargeNumbers.format(ClientPlayerStats.getRuneShards()));
        stat(font, 18, 233, 284, "Ядра рун", LargeNumbers.format(ClientPlayerStats.getRuneCores()));

        heading.drawStringWithShadow(sectionName, 335, 22, HEADING_TEXT_COLOR);
        GlStateManager.popMatrix();
    }

    private static void stat(FontRenderer font, int x, int y, int right,
                             String label, String value) {
        int valueX = right - font.getStringWidth(value);
        font.drawString(font.trimStringToWidth(label, Math.max(12, valueX - x - 6)),
                x, y, TEXT_COLOR);
        font.drawString(value, valueX, y, TEXT_COLOR);
    }
}
