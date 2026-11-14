package com.example.revolution.client;

import com.example.revolution.RevolutionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;

/**
 * The ordinary Rune GUI's world-background shader for auxiliary E-menu screens.
 * Retains ownership so closing one screen never stops a shader installed by another.
 */
public final class GuiBackgroundBlur {
    private static final ResourceLocation BLUR =
            new ResourceLocation("revolution", "shaders/post/gui_blur.json");
    private ShaderGroup owned;

    /** Called from initGui, not from drawScreen: no shader allocation per frame. */
    public void enable(Minecraft mc) {
        if (mc == null || mc.entityRenderer == null) return;
        ShaderGroup active = mc.entityRenderer.getShaderGroup();
        if (owns(active)) return;
        owned = null;
        // Vanilla/other mods may have their own shader: do not replace it.
        if (mc.entityRenderer.isShaderActive()) return;
        try {
            mc.entityRenderer.loadShader(BLUR);
            owned = mc.entityRenderer.getShaderGroup();
        } catch (RuntimeException error) {
            owned = null;
            RevolutionMod.logger.warn("Could not enable GUI blur shader", error);
        }
    }

    /** Never tears down a shader installed by a different GUI or mod. */
    public void disable(Minecraft mc) {
        if (mc != null && mc.entityRenderer != null && owns(mc.entityRenderer.getShaderGroup())) {
            mc.entityRenderer.stopUseShader();
        }
        owned = null;
    }

    private boolean owns(ShaderGroup active) {
        return owned != null && owned == active;
    }
}
