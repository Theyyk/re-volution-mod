package com.example.customguimod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
final class RuneTexturePreloader implements IResourceManagerReloadListener {
    private static final ResourceLocation GUNPOWDER_TEXTURE =
            new ResourceLocation("minecraft", "textures/items/gunpowder.png");
    private static final int RUNE_TEXTURE_SIZE = 128;

    static void register() {
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).registerReloadListener(new RuneTexturePreloader());
        } else {
            new RuneTexturePreloader().onResourceManagerReload(resourceManager);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        Minecraft mc = Minecraft.getMinecraft();
        Set<String> loaded = new HashSet<>();
        BufferedImage fallback = null;
        int fallbackCount = 0;

        for (int i = 0; i < EarthRuneCatalog.size(); i++) {
            EarthRuneCatalog.Definition definition = EarthRuneCatalog.at(i);
            if (definition == null || !loaded.add(definition.iconTexture)) continue;

            ResourceLocation runeTexture = new ResourceLocation(definition.iconTexture);

            // A DynamicTexture fallback can remain cached under the same resource
            // location after a resource-pack reload. Drop the cached texture first
            // so an enabled pack can restore its real rune PNG immediately.
            mc.getTextureManager().deleteTexture(runeTexture);

            if (resourceExists(resourceManager, runeTexture)) {
                mc.getTextureManager().bindTexture(runeTexture);
                continue;
            }

            if (fallback == null) fallback = loadGunpowderFallback(resourceManager);
            if (fallback != null) {
                mc.getTextureManager().loadTexture(runeTexture, new DynamicTexture(fallback));
                fallbackCount++;
            }
        }

        CustomGuiMod.logger.info("Preloaded " + loaded.size() + " rune textures"
                + (fallbackCount > 0 ? "; vanilla gunpowder fallbacks=" + fallbackCount : ""));
    }

    private boolean resourceExists(IResourceManager resourceManager, ResourceLocation location) {
        try (IResource ignored = resourceManager.getResource(location)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private BufferedImage loadGunpowderFallback(IResourceManager resourceManager) {
        try (IResource resource = resourceManager.getResource(GUNPOWDER_TEXTURE)) {
            BufferedImage source = ImageIO.read(resource.getInputStream());
            if (source == null) return null;

            BufferedImage scaled = new BufferedImage(
                    RUNE_TEXTURE_SIZE,
                    RUNE_TEXTURE_SIZE,
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                );
                graphics.drawImage(source, 0, 0, RUNE_TEXTURE_SIZE, RUNE_TEXTURE_SIZE, null);
            } finally {
                graphics.dispose();
            }
            return scaled;
        } catch (IOException e) {
            CustomGuiMod.logger.warn("Could not load vanilla gunpowder fallback texture", e);
            return null;
        }
    }
}
