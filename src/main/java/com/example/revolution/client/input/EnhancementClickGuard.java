package com.example.revolution.client.input;

import com.example.revolution.client.RunesGuiScreen;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Prevents accidental or automated enhancement click bursts without changing button visuals.
 */
@SideOnly(Side.CLIENT)
public final class EnhancementClickGuard {
    private static final int ENHANCE_BUTTON_ID = 2;
    private static final long COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);

    private long lastAcceptedAtNanos;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onActionPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof RunesGuiScreen)) return;

        GuiButton button = event.getButton();
        if (button == null || button.id != ENHANCE_BUTTON_ID) return;

        long now = System.nanoTime();
        if (lastAcceptedAtNanos != 0L && now - lastAcceptedAtNanos < COOLDOWN_NANOS) {
            event.setCanceled(true);
            return;
        }
        lastAcceptedAtNanos = now;
    }
}
