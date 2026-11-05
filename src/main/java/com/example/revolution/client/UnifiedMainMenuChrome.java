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
    private static final UiMenuSection[] SECTIONS = UiMenuSection.mainTabs();
    // Renderer-only instance. Never display this screen or initialize its GUI.
    private static final RuneResourcesGuiScreen AUXILIARY_SIDEBAR = new RuneResourcesGuiScreen();
    private static RuneResourcesGuiScreen lastRuneScreen;
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
        return UnifiedMainMenuGeometry.contentLeft(width, height);
    }

    public static int contentRight(int width, int height) {
        return UnifiedMainMenuGeometry.contentRight(width, height);
    }

    public static int contentTop(int width, int height) {
        return UnifiedMainMenuGeometry.contentTop(width, height);
    }

    private static GuiLayout geometry(int width, int height) {
        GuiLayout layout = new GuiLayout(26);
        layout.update(width, height);
        return layout;
    }

    /** Retain the original Rune GUI across E-tab changes, without re-running the preloader. */
    public static void rememberRunes(RunesGuiScreen screen) {
        if (screen instanceof RuneResourcesGuiScreen)
            lastRuneScreen = (RuneResourcesGuiScreen) screen;
    }

    public static void forgetRunes() {
        lastRuneScreen = null;
    }

    public static void openRunes(Minecraft minecraft) {
        RuneResourcesGuiScreen screen = lastRuneScreen;
        if (screen == null) {
            screen = new RuneResourcesGuiScreen();
            lastRuneScreen = screen;
        }
        // Never show a null screen between two tabs; the original screen will
        // re-sync its authoritative rune snapshot on init.
        minecraft.displayGuiScreen(screen);
    }

    /** Public factory for screens outside the client root package. */
    public static GuiButton panelButton(int id, int x, int y, int width, int height, String text) {
        return new GuiControls.PanelButton(id, x, y, width, height, text);
    }

    /** Theme-consistent surface for feature screens in subpackages. */
    public static void panel(int x, int y, int width, int height) {
        GuiDrawing.drawSurface(x, y, width, height, PANEL_BG_SOFT, PANEL_BORDER_COLOR);
    }

    public static void installNavigation(List<GuiButton> buttons, int width, int height,
                                         UiMenuSection active) {
        GuiLayout layout = geometry(width, height);
        int x = px(layout, layout.navPanelX + 2);
        int size = scaled(layout, UnifiedMenuLayout.size(layout));
        for (int i = 0; i < SECTIONS.length; i++) {
            UiMenuSection section = SECTIONS[i];
            int logicalY = i == 0 ? layout.navPanelY + 2 : UnifiedMenuLayout.y(layout, i - 1);
            GuiControls.PanelButton b = new GuiControls.PanelButton(
                    section.buttonId(), x, py(layout, logicalY), size, size,
                    section.shortLabel());
            b.setSelected(section == active || (section == UiMenuSection.RUNES && active == UiMenuSection.UNIQUE_RUNES));
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
                openRunes(mc);
                break;
            case PETS: mc.displayGuiScreen(new PetsGuiScreen()); break;
            case UNIQUE_RUNES: mc.displayGuiScreen(new UniqueRunesGuiScreen()); break;
            case MECHANIC_UNLOCKS: mc.displayGuiScreen(new MechanicUnlockGuiScreen()); break;
            case ACHIEVEMENTS: mc.displayGuiScreen(new AchievementsGuiScreen()); break;
            default: break;
        }
        return true;
    }

    public static void draw(Minecraft mc, int width, int height,
                            int mouseX, int mouseY, String sectionName) {
        GuiLayout layout = geometry(width, height);
        Gui.drawRect(0, 0, width, height, OVERLAY_COLOR);
        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.canvasX, layout.canvasY, 0);
        GlStateManager.scale(layout.canvasScale, layout.canvasScale, 1);

        // Exactly the same existing renderer as the original rune screen:
        // character preview, equipment slots, statistics and all currencies.
        AUXILIARY_SIDEBAR.drawAuxiliarySidebar(mc, width, height,
                layout.canvasMouseX(mouseX), layout.canvasMouseY(mouseY));

        // Only the right-hand feature content changes between main tabs.
        GuiDrawing.drawSurface(layout.runePanelX, layout.runePanelY,
                Math.max(80, layout.navPanelX - layout.runePanelX - 10),
                624, PANEL_BG_COLOR, PANEL_BORDER_COLOR);
        GuiDrawing.drawSurface(layout.navPanelX, layout.navPanelY,
                layout.navPanelWidth, layout.navPanelHeight,
                PANEL_BG_COLOR, PANEL_BORDER_COLOR);

        GuiFontRenderer.heading(mc).drawStringWithShadow(sectionName,
                layout.runePanelX + 10, 22, HEADING_TEXT_COLOR);
        GlStateManager.popMatrix();
    }
}
