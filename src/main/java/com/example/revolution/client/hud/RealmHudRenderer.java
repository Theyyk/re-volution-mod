package com.example.revolution.client.hud;

import com.example.revolution.client.input.KeyInputHandler;
import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.client.settings.SettingsKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Lower-right Realm indicator with the compact RE:volution control legend above it. */
@SideOnly(Side.CLIENT)
public final class RealmHudRenderer {
    private static final int MARGIN = 6;
    private static final int LINE_GAP = 2;
    private static final int HINT_COLOR = 0xAFAFAF;
    private static final int REALM_COLOR = 0xE6E6E6;
    private static final int BASE_LINE_COUNT = 5;

    @SubscribeEvent
    public void onRenderText(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.world == null || minecraft.gameSettings.hideGUI) return;

        FontRenderer font = minecraft.fontRenderer;
        ScaledResolution resolution = event.getResolution();
        int lineStep = font.FONT_HEIGHT + LINE_GAP;
        int right = resolution.getScaledWidth() - MARGIN;
        int y = resolution.getScaledHeight() - font.FONT_HEIGHT - MARGIN;

        String realm = labelForDimension(minecraft.player.dimension);
        if (realm != null) drawRightAligned(font, realm, right, y, REALM_COLOR);

        y -= lineStep;
        drawHint(font, keyName(KeyInputHandler.keyOpenStageTravel), "Локации", right, y);
        y -= lineStep;
        drawHint(font, keyName(SettingsKeyHandler.keyOpenSettings), "Формат чисел", right, y);
        y -= lineStep;
        drawHint(font, keyName(KeyInputHandler.keyOpenMenu), "Меню", right, y);

        // UI-03-M R1: Mastery belongs to the same lower-right control legend as the other hints.
        // Use the live remappable key label; do not render a separate lower-left overlay.
        y -= lineStep;
        drawHint(font, keyName(KeyInputHandler.keyOpenMastery), "Мастерство", right, y);
        if (auraHintVisible()) {
            y -= lineStep;
            drawHint(font, keyName(KeyInputHandler.keyOpenAura), "Ауры", right, y);
        }
    }

    /** Match the same server-snapshot mechanic gate used by the actual Aura key handler. */
    static boolean auraHintVisible() {
        return ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.AURA_SYSTEM);
    }

    static int reservedBottomPixels(FontRenderer font) {
        if (font == null) return auraHintVisible() ? 69 : 58;
        int lines = BASE_LINE_COUNT + (auraHintVisible() ? 1 : 0);
        return MARGIN + lines * font.FONT_HEIGHT + (lines - 1) * LINE_GAP + 8;
    }

    private static void drawHint(FontRenderer font, String key, String action, int right, int y) {
        drawRightAligned(font, "§f" + key + " §7- " + action, right, y, HINT_COLOR);
    }

    private static void drawRightAligned(FontRenderer font, String text, int right, int y, int color) {
        font.drawStringWithShadow(text, right - font.getStringWidth(text), y, color);
    }

    private static String keyName(KeyBinding binding) {
        if (binding == null) return "?";
        return GameSettings.getKeyDisplayString(binding.getKeyCode());
    }

    static String labelForDimension(int dimensionId) {
        switch (dimensionId) {
            case 0: return "Realm 0";
            case 201: return "Realm 1";
            case 202: return "Realm 2";
            case 203: return "Realm 3";
            default: return null;
        }
    }
}
