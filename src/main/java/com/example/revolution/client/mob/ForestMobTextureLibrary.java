package com.example.revolution.client.mob;

import com.example.revolution.mob.appearance.MobAppearanceDefinition;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Static Location 1 mob textures with the legacy deterministic generator kept as a fallback. */
@SideOnly(Side.CLIENT)
public final class ForestMobTextureLibrary {
    private static final int SIZE = 64;
    private static final String STATIC_TEXTURE_ROOT = "textures/entity/location1/";
    private final Map<String, ResourceLocation> cache = new HashMap<>();

    public ResourceLocation textureFor(MobAppearanceDefinition appearance) {
        String appearanceId = appearance.getAppearanceId();
        ResourceLocation cached = cache.get(appearanceId);
        if (cached != null) return cached;

        ResourceLocation staticTexture = new ResourceLocation(
                "revolution",
                STATIC_TEXTURE_ROOT + appearanceId + ".png");
        if (resourceExists(staticTexture)) {
            cache.put(appearanceId, staticTexture);
            return staticTexture;
        }

        ResourceLocation generated = generatedTexture(appearance);
        cache.put(appearanceId, generated);
        return generated;
    }

    private boolean resourceExists(ResourceLocation location) {
        try (IResource ignored = Minecraft.getMinecraft().getResourceManager().getResource(location)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private ResourceLocation generatedTexture(MobAppearanceDefinition appearance) {
        ForestMobPaletteCatalog.Palette palette = ForestMobPaletteCatalog.get(appearance.getAppearanceId());
        DynamicTexture texture = new DynamicTexture(SIZE, SIZE);
        int[] pixels = texture.getTextureData();
        int seed = appearance.getAppearanceId().hashCode();

        /*
         * Legacy fallback for appearances that do not have a static PNG yet. ForestMobModel currently
         * reuses simple box UVs outside the hand-authored families, so keep this atlas coherent and
         * palette-safe until those appearances are migrated as well.
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

        return Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("revolution_mob_" + appearance.getAppearanceId(), texture);
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
