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

        /*
         * ForestMobModel deliberately reuses simple box UVs. Keep the complete atlas coherent so any
         * cuboid can sample any region without picking up an unrelated eye/island patch. Variation is
         * low-frequency and palette-safe; silhouette details carry most of the per-appearance identity.
         */
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int hash = mix(seed, x >> 1, y >> 1);
                int color = palette.base;
                int value = hash & 63;
                if (value < 10) color = blend(palette.base, palette.shadow, 0.28F);
                else if (value > 55) color = blend(palette.base, palette.detail, 0.24F);
                if (((x + seed) & 15) == 0 && ((y + (seed >>> 4)) & 15) < 3) {
                    color = blend(color, palette.accent, 0.18F);
                }
                pixels[y * SIZE + x] = color;
            }
        }
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
}