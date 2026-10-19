package com.example.revolution.client.settings;

import com.example.revolution.number.LargeNumberDisplayMode;
import com.example.revolution.number.LargeNumbers;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SettingsScreen extends GuiScreen {
    private static final int BUTTON_W = 180;
    private static final int BUTTON_H = 20;

    @Override
    public void initGui() {
        buttonList.clear();
        int x = width / 2 - BUTTON_W / 2;
        int y = height / 2 - 12;
        LargeNumberDisplayMode[] modes = LargeNumberDisplayMode.values();
        for (int i = 0; i < modes.length; i++) {
            buttonList.add(new GuiButton(i, x, y + i * 24, BUTTON_W, BUTTON_H, label(modes[i])));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id < 0 || button.id >= LargeNumberDisplayMode.values().length) return;
        LargeNumbers.setDisplayMode(LargeNumberDisplayMode.values()[button.id]);
        initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "RE:volution - Настройки", width / 2, height / 2 - 78, 0xFFFFFF);
        drawCenteredString(fontRenderer, "Формат больших чисел", width / 2, height / 2 - 58, 0xBFBFBF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static String label(LargeNumberDisplayMode mode) {
        boolean selected = LargeNumbers.displayMode() == mode;
        return (selected ? "§a> " : "") + mode.displayName() + (selected ? " §a<" : "");
    }
}
