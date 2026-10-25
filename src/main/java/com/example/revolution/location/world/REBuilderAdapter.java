package com.example.revolution.location.world;

import com.example.revolution.location.generation.BuildTemplateCatalog;
import com.example.revolution.location.generation.LocationGenerationPlan;
import com.example.revolution.location.generation.PhysicalStageBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

/** Optional REBuilder 1.1.2 bridge. No copied parser, placement, block rotation or build loop.
 * Reflection keeps REBuilder/WorldEdit server-only and out of the gameplay JAR/client dependency graph.
 */
public final class REBuilderAdapter implements BuildTemplateCatalog {
    private final Path directory;

    public REBuilderAdapter(Path configDirectory) {
        directory = configDirectory.resolve("rebuilder").resolve("builds");
    }

    @Override public void validate(String name) { loadTemplate(name); }

    private Object loadTemplate(String name) {
        try {
            Class<?> validatorType = Class.forName("ru.rebuilder.template.TemplateReader$BlockValidator");
            Method validate = Class.forName("ru.rebuilder.world.VanillaBlocks").getMethod("validate", String.class, int.class);
            Object validator = Proxy.newProxyInstance(validatorType.getClassLoader(), new Class<?>[] {validatorType},
                    (proxy, method, args) -> {
                        try { return validate.invoke(null, args); }
                        catch (InvocationTargetException e) { throw e.getCause(); }
                    });
            Class<?> libraryType = Class.forName("ru.rebuilder.template.TemplateLibrary");
            Object library = libraryType.getConstructor(Path.class, validatorType).newInstance(directory, validator);
            invoke(libraryType.getMethod("reload"), library);
            Object template = invoke(libraryType.getMethod("get", String.class), library, name);
            if (template == null) {
                throw new IllegalArgumentException("REBuilder template '" + name
                        + "' does not exist or is invalid. Check /rebuilder list and /rebuilder reload.");
            }
            return template;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw unavailable(e);
        }
    }

    public PhysicalStageBuilder forPlayer(EntityPlayerMP player) {
        return plan -> prepare(player, plan);
    }

    private PhysicalStageBuilder.PreparedBuild prepare(EntityPlayerMP player, LocationGenerationPlan plan) {
        try {
            Object template = loadTemplate(plan.getTemplateId());
            Class<?> serviceType = Class.forName("ru.rebuilder.world.BuildService");
            Object service = serviceType.getConstructor().newInstance();

            double originalX = player.posX;
            double originalY = player.posY;
            double originalZ = player.posZ;
            List<?> targets;
            try {
                // Remote /location create must not depend on an administrator preloading the destination.
                loadChunk(player, new BlockPos(plan.getCenterX(), plan.getCenterY(), plan.getCenterZ()));
                player.setPosition(plan.getCenterX(), plan.getCenterY(), plan.getCenterZ());
                BlockPos resolvedAnchor = player.getPosition();
                if (resolvedAnchor.getX() != plan.getCenterX()
                        || resolvedAnchor.getY() != plan.getCenterY()
                        || resolvedAnchor.getZ() != plan.getCenterZ()) {
                    throw new IllegalStateException("Could not resolve REBuilder anchor at stage center");
                }
                targets = (List<?>) invoke(serviceType.getMethod("prepare", EntityPlayerMP.class,
                        Class.forName("ru.rebuilder.template.Template"), int.class),
                        service, player, template, plan.getRotationDegrees());
            } finally {
                player.setPosition(originalX, originalY, originalZ);
            }

            Method build = serviceType.getMethod("build", EntityPlayerMP.class, List.class);
            Map<BlockPos, IBlockState> original = new LinkedHashMap<>();
            for (Object target : targets) {
                BlockPos position = (BlockPos) target.getClass().getField("position").get(target);
                // Templates may cross chunk boundaries, so preload every touched target chunk before snapshot/build.
                loadChunk(player, position);
                original.put(position, player.world.getBlockState(position));
            }
            return new PhysicalStageBuilder.PreparedBuild() {
                @Override public void apply() { invoke(build, service, player, targets); }
                @Override public void rollback() {
                    // Never call shared //undo: a no-op/failed build could undo an unrelated older edit.
                    IllegalStateException failure = null;
                    for (Map.Entry<BlockPos, IBlockState> entry : original.entrySet()) {
                        try {
                            loadChunk(player, entry.getKey());
                            if (player.world.getBlockState(entry.getKey()).equals(entry.getValue())) continue;
                            if (!player.world.setBlockState(entry.getKey(), entry.getValue(), 2)) {
                                throw new IllegalStateException("Could not restore block " + entry.getKey());
                            }
                        } catch (RuntimeException e) {
                            if (failure == null) failure = new IllegalStateException("Physical build rollback failed");
                            failure.addSuppressed(e);
                        }
                    }
                    if (failure != null) throw failure;
                }
            };
        } catch (ReflectiveOperationException | LinkageError e) {
            throw unavailable(e);
        }
    }

    private static void loadChunk(EntityPlayerMP player, BlockPos position) {
        player.world.getChunkFromChunkCoords(position.getX() >> 4, position.getZ() >> 4);
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new IllegalStateException("REBuilder: " + cause.getMessage(), cause);
        } catch (ReflectiveOperationException | LinkageError e) { throw unavailable(e); }
    }

    private static IllegalStateException unavailable(Throwable cause) {
        return new IllegalStateException("REBuilder 1.1.2 and WorldEdit 6.1.10 are required for physical Location builds; API unavailable", cause);
    }
}
