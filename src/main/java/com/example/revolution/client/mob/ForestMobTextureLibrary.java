package com.example.revolution.client.mob;

import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Deterministic generated 64x64 textures for the complete first-location roster. */
@SideOnly(Side.CLIENT)
public final class ForestMobTextureLibrary {
    private static final int SIZE = 64;
    private final Map<String, ResourceLocation> cache = new HashMap<>();

    public ResourceLocation textureFor(MobAppearanceDefinition appearance) {
        ResourceLocation cached = cache.get(appearance.getAppearanceId());
        if (cached != null) return cached;

        ForestMobPaletteCatalog.Palette palette = ForestMobPaletteCatalog.get(appearance.getAppearanceId());
        DynamicTexture texture = new DynamicTexture(SIZE, SIZE);
        int[] pixels = texture.getTextureData();
        int seed = appearance.getAppearanceId().hashCode();

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int hash = mix(seed, x, y);
                int band = ((x / 8) + (y / 8)) & 3;
                int color;
                if (band == 0) color = palette.base;
                else if (band == 1) color = palette.shadow;
                else if (band == 2) color = palette.base;
                else color = palette.detail;

                if ((hash & 31) == 0 || ((x + y + seed) & 31) == 0) color = palette.accent;
                else if ((hash & 7) == 0) color = blend(color, palette.shadow, 0.35F);
                else if ((hash & 15) == 1) color = blend(color, palette.detail, 0.25F);
                pixels[y * SIZE + x] = color;
            }
        }

        // Distinct high-contrast eye/accent islands used by most family UV layouts.
        fill(pixels, 4, 4, 8, 8, palette.accent);
        fill(pixels, 18, 4, 22, 8, palette.accent);
        fill(pixels, 34, 4, 46, 10, palette.detail);
        fill(pixels, 50, 4, 60, 10, palette.shadow);
        texture.updateDynamicTexture();

        ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("revolution_mob_" + appearance.getAppearanceId(), texture);
        cache.put(appearance.getAppearanceId(), location);
        return location;
    }

    private static int mix(int seed, int x, int y) {
        int value = seed;
        value ^= x * 0x45d9f3b;
        value = Integer.rotateLeft(value, 13);
        value ^= y * 0x119de1f3;
        value *= 0x27d4eb2d;
        return value ^ (value >>> 15);
    }

    private static int blend(int a, int b, float amount) {
        int ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * amount);
        int g = (int) (ag + (bg - ag) * amount);
        int blue = (int) (ab + (bb - ab) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | blue;
    }

    private static void fill(int[] pixels, int left, int top, int right, int bottom, int color) {
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) pixels[y * SIZE + x] = color;
        }
    }
}
