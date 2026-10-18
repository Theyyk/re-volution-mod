package com.example.customguimod.mob;

import com.example.customguimod.CustomGuiMod;
import com.example.customguimod.PlayerStatsService;
import com.example.customguimod.rune.RuneMobEffects;
import com.example.customguimod.number.LargeNumbers;
import com.example.customguimod.persistence.MongoManager;
import com.example.customguimod.resource.ResourceManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MobManager {
    private static final int POISON_TICK_INTERVAL = 20;
    private static final int INITIAL_SPAWN_DELAY_TICKS = 20;
    private static final int LOST_ENTITY_RESPAWN_DELAY_TICKS = 1;
    private static final double STATUS_EFFECT_RADIUS = 2.0D;
    private static final double STATUS_EFFECT_RADIUS_SQ = STATUS_EFFECT_RADIUS * STATUS_EFFECT_RADIUS;
    private static final BigDecimal VANILLA_MAX_HEALTH = new BigDecimal("1024");
    private static final String SESSION_TAG = "revolutionManagedSession";
    private static final String SPAWN_KEY_TAG = "revolutionManagedSpawnKey";
    private static final List<MobSpawn> spawns = new ArrayList<>();
    private static final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private static World serverWorld;
    private static int poisonTickCounter;
    private static String managedSessionId = UUID.randomUUID().toString();

    public static class MobSpawn {
        public String name;
        public String type;
        public BigDecimal hp;
        public BigDecimal currentHp;
        public String resource;
        public BigDecimal amount;
        public double x, y, z;
        public String spawnKey;
        public EntityLiving entity;
        public int respawnTimer;
        public boolean spawnFailureLogged;
        public boolean chunkWasUnloaded;
        public boolean hasSpawnedOnce;

        public MobSpawn(String name, String type, BigDecimal hp, String resource, BigDecimal amount, double x, double y, double z) {
            this.name = name;
            this.type = type;
            this.hp = positive(hp);
            this.currentHp = this.hp;
            this.resource = resource;
            this.amount = positive(amount);
            this.x = x; this.y = y; this.z = z;
            this.spawnKey = buildSpawnKey(name, type, x, y, z);
        }
    }

    public static void loadFromDatabase(World world) {
        serverWorld = world;
        poisonDamagePlayers.clear();
        poisonTickCounter = 0;
        managedSessionId = UUID.randomUUID().toString();
        spawns.clear();

        List<MongoManager.CustomMob> mobs = MongoManager.getAllMobs();
        CustomGuiMod.logger.info("Loaded mobs from DB: " + mobs.size());
        for (MongoManager.CustomMob mob : mobs) {
            if (!mob.enabled) continue;
            MobSpawn spawn = new MobSpawn(mob.name, mob.type, mob.hp,
                    ResourceManager.getMobResource(mob.name), ResourceManager.getMobAmountValue(mob.name),
                    mob.x, mob.y, mob.z);
            spawns.add(spawn);
        }

        Set<Long> warmedChunks = new HashSet<>();
        for (MobSpawn spawn : spawns) {
            BlockPos spawnPos = spawnPosition(spawn);
            int chunkX = spawnPos.getX() >> 4;
            int chunkZ = spawnPos.getZ() >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            if (warmedChunks.add(chunkKey)) serverWorld.getChunkFromBlockCoords(spawnPos);
        }

        purgeLoadedManagedMobs(world);
        for (MobSpawn spawn : spawns) spawn.respawnTimer = INITIAL_SPAWN_DELAY_TICKS;
        CustomGuiMod.logger.info("Prepared " + spawns.size() + " managed mob(s); initial spawn delayed "
                + INITIAL_SPAWN_DELAY_TICKS + " ticks after spawn chunks were loaded.");
    }

    private static void purgeLoadedManagedMobs(World world) {
        if (world == null) return;
        int removed = 0;
        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) continue;
            if (!isManagedOrLegacyCustomMob(entity)) continue;
            entity.setDead();
            removed++;
            CustomGuiMod.logger.debug("Removed old persisted managed mob: " + entity.getUniqueID());
        }
        if (removed > 0) CustomGuiMod.logger.info("Removed " + removed + " stale managed mob(s) before initial spawn.");
    }

    public static void respawnAll() {
        for (MobSpawn spawn : spawns) if (spawn.entity != null && !spawn.entity.isDead) spawn.entity.setDead();
        spawns.clear();
        if (serverWorld != null) loadFromDatabase(serverWorld);
    }

    /** Read-only guard used by the damage listener to avoid reapplying click effects to poison ticks. */
    public static boolean isApplyingPoisonDamage(UUID playerUuid) { return playerUuid != null && poisonDamagePlayers.contains(playerUuid); }
    static boolean isWithinStatusRadiusSq(double distanceSq) { return Double.isFinite(distanceSq) && distanceSq >= 0.0D && distanceSq <= STATUS_EFFECT_RADIUS_SQ; }
    static double nativeHealthFor(BigDecimal configuredHp) { return positive(configuredHp).min(VANILLA_MAX_HEALTH).doubleValue(); }

    static BigDecimal remainingHealthAfterDamage(BigDecimal currentHp, double damage) {
        if (currentHp == null || currentHp.signum() <= 0) return BigDecimal.ZERO;
        if (!Double.isFinite(damage) || damage <= 0.0D) return currentHp;
        BigDecimal remaining = currentHp.subtract(BigDecimal.valueOf(damage));
        return remaining.signum() <= 0 ? BigDecimal.ZERO : LargeNumbers.normalize(remaining);
    }

    private static BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0) return BigDecimal.ONE;
        return LargeNumbers.normalize(value);
    }

    private static String buildSpawnKey(String name, String type, double x, double y, double z) {
        return String.valueOf(name) + "|" + String.valueOf(type).toLowerCase(java.util.Locale.ROOT) + "|"
                + Double.toString(x) + "|" + Double.toString(y) + "|" + Double.toString(z);
    }

    private static BlockPos spawnPosition(MobSpawn spawn) {
        return new BlockPos(spawn.x, spawn.y, spawn.z);
    }

    private static boolean isSpawnChunk(MobSpawn spawn, int chunkX, int chunkZ) {
        BlockPos pos = spawnPosition(spawn);
        return (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ;
    }

    private static void applyPoisonTicks() {
        if (serverWorld == null || serverWorld.getMinecraftServer() == null) return;
        long worldTick = serverWorld.getTotalWorldTime();
        for (EntityPlayerMP player : serverWorld.getMinecraftServer().getPlayerList().getPlayers()) {
            Double resolvedPoisonDamage = RuneMobEffects.poisonDamage(player.getUniqueID(), worldTick);
            if (resolvedPoisonDamage == null) continue;
            double poisonDamage = resolvedPoisonDamage;
            if (poisonDamage <= 0.0D) continue;
            UUID playerUuid = player.getUniqueID();
            for (MobSpawn spawn : spawns) {
                EntityLiving target = spawn.entity;
                if (target == null || target.isDead || spawn.currentHp.signum() <= 0) continue;
                if (!isWithinStatusRadiusSq(player.getDistanceSq(target))) continue;
                poisonDamagePlayers.add(playerUuid);
                try { target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) poisonDamage); }
                finally { poisonDamagePlayers.remove(playerUuid); }
            }
        }
    }

    private static EntityLiving findExistingCurrentSessionEntity(MobSpawn spawn) {
        if (serverWorld == null || spawn == null) return null;
        for (Entity raw : new ArrayList<>(serverWorld.loadedEntityList)) {
            if (!(raw instanceof EntityLiving) || raw.isDead) continue;
            if (!managedSessionId.equals(raw.getEntityData().getString(SESSION_TAG))) continue;
            if (!spawn.spawnKey.equals(raw.getEntityData().getString(SPAWN_KEY_TAG))) continue;
            return (EntityLiving) raw;
        }
        return null;
    }

    private static boolean isRegisteredInWorld(EntityLiving entity) {
        if (serverWorld == null || entity == null || entity.isDead || entity.world != serverWorld) return false;
        if (!serverWorld.loadedEntityList.contains(entity)) return false;
        return serverWorld.getEntityByID(entity.getEntityId()) == entity;
    }

    private static void spawnMob(MobSpawn spawn) {
        if (serverWorld == null) return;

        EntityLiving existing = findExistingCurrentSessionEntity(spawn);
        if (existing != null) {
            spawn.entity = existing;
            spawn.spawnFailureLogged = false;
            return;
        }

        BlockPos spawnPos = spawnPosition(spawn);
        serverWorld.getChunkFromBlockCoords(spawnPos);
        EntityLiving entity = spawn.type.equalsIgnoreCase("skeleton") ? new EntitySkeleton(serverWorld) : new EntityZombie(serverWorld);
        spawn.currentHp = spawn.hp;
        entity.setPosition(spawn.x, spawn.y, spawn.z);
        syncMobMetadata(spawn, entity);
        entity.getEntityData().setString(SESSION_TAG, managedSessionId);
        entity.getEntityData().setString(SPAWN_KEY_TAG, spawn.spawnKey);
        entity.setAlwaysRenderNameTag(true);
        entity.setFire(0);
        double nativeHealth = nativeHealthFor(spawn.hp);
        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(nativeHealth);
        entity.setHealth((float) nativeHealth);
        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0D);
        entity.setNoAI(true);
        entity.forceSpawn = true;
        if (!serverWorld.spawnEntity(entity)) {
            spawn.entity = null;
            spawn.respawnTimer = 20;
            if (!spawn.spawnFailureLogged) {
                spawn.spawnFailureLogged = true;
                CustomGuiMod.logger.warn("Could not spawn managed mob; retrying once per second: " + spawn.name);
            }
            return;
        }
        spawn.spawnFailureLogged = false;
        spawn.entity = entity;
        if (!spawn.hasSpawnedOnce) {
            spawn.hasSpawnedOnce = true;
            CustomGuiMod.logger.info("Spawned mob: " + spawn.name + " (" + spawn.type + ") at " + spawn.x + ", " + spawn.y + ", " + spawn.z
                    + " with custom HP " + LargeNumbers.canonical(spawn.hp) + " (native backing HP " + nativeHealth + ")");
        } else {
            CustomGuiMod.logger.debug("Respawned managed mob: " + spawn.name + " entityId=" + entity.getEntityId());
        }
    }

    private static void syncMobMetadata(MobSpawn spawn, EntityLiving entity) {
        if (spawn == null || entity == null) return;
        entity.setCustomNameTag("§c" + spawn.name + "|" + LargeNumbers.canonical(spawn.currentHp) + "|"
                + LargeNumbers.canonical(spawn.hp) + "|" + spawn.resource + "|" + LargeNumbers.canonical(spawn.amount));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return;
        if (!isManagedOrLegacyCustomMob(entity)) return;
        String entitySessionId = entity.getEntityData().getString(SESSION_TAG);
        if (managedSessionId.equals(entitySessionId)) return;
        event.setCanceled(true);
        entity.setDead();
        CustomGuiMod.logger.debug("Prevented stale persisted managed mob from rejoining world: " + entity.getUniqueID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChunkSave(ChunkDataEvent.Save event) {
        NBTTagCompound root = event.getData();
        if (root == null || !root.hasKey("Level", 10)) return;
        NBTTagCompound level = root.getCompoundTag("Level");
        if (!level.hasKey("Entities", 9)) return;
        NBTTagList entities = level.getTagList("Entities", 10);
        int removed = 0;
        for (int i = entities.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound entityTag = entities.getCompoundTagAt(i);
            if (!isManagedOrLegacyCustomMobNbt(entityTag)) continue;
            entities.removeTag(i); removed++;
        }
        if (removed > 0) {
            level.setTag("Entities", entities); root.setTag("Level", level);
            CustomGuiMod.logger.debug("Skipped persisting " + removed + " managed mob(s) in chunk " + event.getChunk().x + ", " + event.getChunk().z);
        }
    }

    @SubscribeEvent
    public void onSpawnChunkUnload(ChunkEvent.Unload event) {
        if (event.getWorld().isRemote || serverWorld == null || event.getWorld() != serverWorld) return;
        int chunkX = event.getChunk().x;
        int chunkZ = event.getChunk().z;
        for (MobSpawn spawn : spawns) {
            if (!isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = true;
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            CustomGuiMod.logger.debug("Managed mob spawn chunk unloaded: " + spawn.name);
        }
    }

    @SubscribeEvent
    public void onSpawnChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote || serverWorld == null || event.getWorld() != serverWorld) return;
        int chunkX = event.getChunk().x;
        int chunkZ = event.getChunk().z;
        for (MobSpawn spawn : spawns) {
            if (!isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = false;
            EntityLiving existing = findExistingCurrentSessionEntity(spawn);
            if (existing != null) {
                spawn.entity = existing;
                continue;
            }
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            CustomGuiMod.logger.debug("Managed mob spawn chunk loaded; scheduling clean respawn: " + spawn.name);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        for (MobSpawn spawn : spawns) {
            if (spawn.entity != event.getEntityLiving()) continue;
            net.minecraft.util.DamageSource source = event.getSource();
            if (!(source.getTrueSource() instanceof EntityPlayerMP)) { event.setCanceled(true); return; }
            EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
            spawn.currentHp = remainingHealthAfterDamage(spawn.currentHp, Math.max(0.0D, event.getAmount()));
            event.setCanceled(true);
            if (spawn.currentHp.signum() <= 0) finishManagedDeath(spawn, player);
            else { syncMobMetadata(spawn, spawn.entity); spawn.entity.setHealth((float) nativeHealthFor(spawn.hp)); }
            return;
        }
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        for (MobSpawn spawn : spawns) {
            if (spawn.entity != event.getEntityLiving()) continue;
            EntityPlayerMP player = event.getSource().getTrueSource() instanceof EntityPlayerMP ? (EntityPlayerMP) event.getSource().getTrueSource() : null;
            finishManagedDeath(spawn, player); break;
        }
    }

    private static void finishManagedDeath(MobSpawn spawn, EntityPlayerMP player) {
        if (spawn == null || spawn.entity == null) return;
        if (player != null) {
            BigDecimal rewardAmount = RuneMobEffects.rewardAmount(player.getUniqueID(), spawn.resource, spawn.amount);
            if (ResourceManager.grantPlayerResource(player.getUniqueID(), spawn.resource, rewardAmount)) {
                BigDecimal newBalance = ResourceManager.getPlayerResourceValue(player.getUniqueID(), spawn.resource);
                player.sendMessage(new TextComponentString("§6+" + LargeNumbers.format(rewardAmount) + " "
                        + ResourceManager.getDisplayName(spawn.resource) + "! Баланс: " + LargeNumbers.format(newBalance)));
                PlayerStatsService.sendTo(player);
            } else CustomGuiMod.logger.warn("Skipped invalid mob reward resource: " + spawn.resource);
        }
        spawn.currentHp = BigDecimal.ZERO;
        spawn.entity.setDead(); spawn.entity = null; spawn.respawnTimer = 10;
        CustomGuiMod.logger.info("Mob " + spawn.name + " killed. Respawn in 0.5s.");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (MobSpawn spawn : spawns) {
            boolean spawnChunkLoaded = serverWorld != null && serverWorld.isBlockLoaded(spawnPosition(spawn));
            if (!spawnChunkLoaded) {
                spawn.chunkWasUnloaded = true;
                continue;
            }

            if (spawn.chunkWasUnloaded) {
                spawn.chunkWasUnloaded = false;
                if (spawn.entity != null && !isRegisteredInWorld(spawn.entity)) {
                    if (!spawn.entity.isDead) spawn.entity.setDead();
                    spawn.entity = null;
                }
                if (spawn.entity == null) {
                    spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
                    continue;
                }
            }

            if (spawn.entity != null && !spawn.entity.isDead) {
                if (isRegisteredInWorld(spawn.entity)) {
                    if (spawn.entity.isBurning()) spawn.entity.extinguish();
                    continue;
                }
                CustomGuiMod.logger.warn("Managed mob lost from world registry while spawn chunk stayed loaded; scheduling clean respawn: " + spawn.name
                        + " entityId=" + spawn.entity.getEntityId());
                spawn.entity.setDead();
                spawn.entity = null;
                spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
                continue;
            }

            EntityLiving existing = findExistingCurrentSessionEntity(spawn);
            if (existing != null) {
                spawn.entity = existing;
                continue;
            }

            if (spawn.respawnTimer > 0) spawn.respawnTimer--; else spawnMob(spawn);
        }
        if (++poisonTickCounter >= POISON_TICK_INTERVAL) { poisonTickCounter = 0; applyPoisonTicks(); }
    }

    @SubscribeEvent public void onMobDrops(LivingDropsEvent event) {
        if (!(event.getEntityLiving() instanceof EntityZombie) && !(event.getEntityLiving() instanceof EntitySkeleton)) return;
        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        if (!isManagedOrLegacyCustomMob(entity)) return;
        event.getDrops().clear(); event.setCanceled(true);
    }

    @SubscribeEvent public void onMobExperience(LivingExperienceDropEvent event) {
        if (!(event.getEntityLiving() instanceof EntityZombie) && !(event.getEntityLiving() instanceof EntitySkeleton)) return;
        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        if (isManagedOrLegacyCustomMob(entity)) event.setDroppedExperience(0);
    }

    private static boolean isManagedOrLegacyCustomMob(Entity entity) {
        if (entity == null) return false;
        if (!entity.getEntityData().getString(SESSION_TAG).isEmpty()) return true;
        return entity.hasCustomName() && isCustomMobName(entity.getCustomNameTag());
    }

    private static boolean isManagedOrLegacyCustomMobNbt(NBTTagCompound entityTag) {
        if (entityTag == null) return false;
        if (entityTag.hasKey("ForgeData", 10) && !entityTag.getCompoundTag("ForgeData").getString(SESSION_TAG).isEmpty()) return true;
        return entityTag.hasKey("CustomName", 8) && isCustomMobName(entityTag.getString("CustomName"));
    }

    private static boolean isCustomMobName(String rawName) {
        if (rawName == null || !rawName.startsWith("§c")) return false;
        return rawName.substring(2).split("\\|").length >= 5;
    }
}
