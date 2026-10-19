package com.example.revolution.mob.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.mob.definition.ManagedMobDefinition;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.rules.MobHealthRules;
import com.example.revolution.mob.state.MobRuntimeState;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.rune.service.RuneMobEffectService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ManagedMobService {
    private final MobRepository mobRepository;
    private final RuneMobEffectService runeEffects;
    private final MobRewardService rewards;

    public ManagedMobService(MobRepository mobRepository, RuneMobEffectService runeEffects, MobRewardService rewards) {
        this.mobRepository = mobRepository;
        this.runeEffects = runeEffects;
        this.rewards = rewards;
    }

    private static final int POISON_TICK_INTERVAL = 20;
    private static final int INITIAL_SPAWN_DELAY_TICKS = 20;
    private static final int LOST_ENTITY_RESPAWN_DELAY_TICKS = 1;
    private static final String SESSION_TAG = "revolutionManagedSession";
    private static final String SPAWN_KEY_TAG = "revolutionManagedSpawnKey";
    private final List<MobRuntimeState> spawns = new ArrayList<>();
    private final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private World serverWorld;
    private int poisonTickCounter;
    private String managedSessionId = UUID.randomUUID().toString();

    public void loadFromDatabase(World world) {
        serverWorld = world;
        poisonDamagePlayers.clear();
        poisonTickCounter = 0;
        managedSessionId = UUID.randomUUID().toString();
        spawns.clear();

        List<MobDefinition> mobs = mobRepository.getAllMobs();
        RevolutionMod.logger.info("Loaded mobs from DB: " + mobs.size());
        for (MobDefinition mob : mobs) {
            if (!mob.enabled) continue;
            MobRuntimeState spawn = new MobRuntimeState(new ManagedMobDefinition(mob.name, mob.type, mob.hp,
                    mobRepository.getMobResource(mob.name), mobRepository.getMobAmountValue(mob.name),
                    mob.x, mob.y, mob.z));
            spawns.add(spawn);
        }

        Set<Long> warmedChunks = new HashSet<>();
        for (MobRuntimeState spawn : spawns) {
            BlockPos spawnPos = spawnPosition(spawn);
            int chunkX = spawnPos.getX() >> 4;
            int chunkZ = spawnPos.getZ() >> 4;
            long chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
            if (warmedChunks.add(chunkKey)) serverWorld.getChunkFromBlockCoords(spawnPos);
        }

        purgeLoadedManagedMobs(world);
        for (MobRuntimeState spawn : spawns) spawn.respawnTimer = INITIAL_SPAWN_DELAY_TICKS;
        RevolutionMod.logger.info("Prepared " + spawns.size() + " managed mob(s); initial spawn delayed "
                + INITIAL_SPAWN_DELAY_TICKS + " ticks after spawn chunks were loaded.");
    }

    private void purgeLoadedManagedMobs(World world) {
        if (world == null) return;
        int removed = 0;
        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) continue;
            if (!isManagedOrLegacyCustomMob(entity)) continue;
            entity.setDead();
            removed++;
            RevolutionMod.logger.debug("Removed old persisted managed mob: " + entity.getUniqueID());
        }
        if (removed > 0) RevolutionMod.logger.info("Removed " + removed + " stale managed mob(s) before initial spawn.");
    }

    public void respawnAll() {
        for (MobRuntimeState spawn : spawns) if (spawn.entity != null && !spawn.entity.isDead) spawn.entity.setDead();
        spawns.clear();
        if (serverWorld != null) loadFromDatabase(serverWorld);
    }

    /** Read-only guard used by the damage listener to avoid reapplying click effects to poison ticks. */
    public boolean isApplyingPoisonDamage(UUID playerUuid) { return playerUuid != null && poisonDamagePlayers.contains(playerUuid); }

    private BlockPos spawnPosition(MobRuntimeState spawn) {
        return new BlockPos(spawn.definition.x, spawn.definition.y, spawn.definition.z);
    }

    private boolean isSpawnChunk(MobRuntimeState spawn, int chunkX, int chunkZ) {
        BlockPos pos = spawnPosition(spawn);
        return (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ;
    }

    private void applyPoisonTicks() {
        if (serverWorld == null || serverWorld.getMinecraftServer() == null) return;
        long worldTick = serverWorld.getTotalWorldTime();
        for (EntityPlayerMP player : serverWorld.getMinecraftServer().getPlayerList().getPlayers()) {
            Double resolvedPoisonDamage = runeEffects.poisonDamage(player.getUniqueID(), worldTick);
            if (resolvedPoisonDamage == null) continue;
            double poisonDamage = resolvedPoisonDamage;
            if (poisonDamage <= 0.0D) continue;
            UUID playerUuid = player.getUniqueID();
            for (MobRuntimeState spawn : spawns) {
                EntityLiving target = spawn.entity;
                if (target == null || target.isDead || spawn.currentHp.signum() <= 0) continue;
                if (!MobHealthRules.isWithinStatusRadiusSq(player.getDistanceSq(target))) continue;
                poisonDamagePlayers.add(playerUuid);
                try { target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) poisonDamage); }
                finally { poisonDamagePlayers.remove(playerUuid); }
            }
        }
    }

    private EntityLiving findExistingCurrentSessionEntity(MobRuntimeState spawn) {
        if (serverWorld == null || spawn == null) return null;
        for (Entity raw : new ArrayList<>(serverWorld.loadedEntityList)) {
            if (!(raw instanceof EntityLiving) || raw.isDead) continue;
            if (!managedSessionId.equals(raw.getEntityData().getString(SESSION_TAG))) continue;
            if (!spawn.definition.spawnKey.equals(raw.getEntityData().getString(SPAWN_KEY_TAG))) continue;
            return (EntityLiving) raw;
        }
        return null;
    }

    private boolean isRegisteredInWorld(EntityLiving entity) {
        if (serverWorld == null || entity == null || entity.isDead || entity.world != serverWorld) return false;
        if (!serverWorld.loadedEntityList.contains(entity)) return false;
        return serverWorld.getEntityByID(entity.getEntityId()) == entity;
    }

    private void spawnMob(MobRuntimeState spawn) {
        if (serverWorld == null) return;

        EntityLiving existing = findExistingCurrentSessionEntity(spawn);
        if (existing != null) {
            spawn.entity = existing;
            spawn.spawnFailureLogged = false;
            return;
        }

        BlockPos spawnPos = spawnPosition(spawn);
        serverWorld.getChunkFromBlockCoords(spawnPos);
        EntityLiving entity = spawn.definition.type.equalsIgnoreCase("skeleton") ? new EntitySkeleton(serverWorld) : new EntityZombie(serverWorld);
        spawn.currentHp = spawn.definition.hp;
        entity.setPosition(spawn.definition.x, spawn.definition.y, spawn.definition.z);
        syncMobMetadata(spawn, entity);
        entity.getEntityData().setString(SESSION_TAG, managedSessionId);
        entity.getEntityData().setString(SPAWN_KEY_TAG, spawn.definition.spawnKey);
        entity.setAlwaysRenderNameTag(true);
        entity.setFire(0);
        double nativeHealth = MobHealthRules.nativeHealthFor(spawn.definition.hp);
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
                RevolutionMod.logger.warn("Could not spawn managed mob; retrying once per second: " + spawn.definition.name);
            }
            return;
        }
        spawn.spawnFailureLogged = false;
        spawn.entity = entity;
        if (!spawn.hasSpawnedOnce) {
            spawn.hasSpawnedOnce = true;
            RevolutionMod.logger.info("Spawned mob: " + spawn.definition.name + " (" + spawn.definition.type + ") at " + spawn.definition.x + ", " + spawn.definition.y + ", " + spawn.definition.z
                    + " with custom HP " + LargeNumbers.canonical(spawn.definition.hp) + " (native backing HP " + nativeHealth + ")");
        } else {
            RevolutionMod.logger.debug("Respawned managed mob: " + spawn.definition.name + " entityId=" + entity.getEntityId());
        }
    }

    private void syncMobMetadata(MobRuntimeState spawn, EntityLiving entity) {
        if (spawn == null || entity == null) return;
        entity.setCustomNameTag("§c" + spawn.definition.name + "|" + LargeNumbers.canonical(spawn.currentHp) + "|"
                + LargeNumbers.canonical(spawn.definition.hp) + "|" + spawn.definition.resource + "|" + LargeNumbers.canonical(spawn.definition.amount));
    }

    public boolean isStaleEntity(Entity entity, World world) {
        if (world.isRemote) return false;
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return false;
        if (!isManagedOrLegacyCustomMob(entity)) return false;
        String entitySessionId = entity.getEntityData().getString(SESSION_TAG);
        if (managedSessionId.equals(entitySessionId)) return false;
        return true;
    }

    public void removePersistedMobs(NBTTagCompound root, int chunkX, int chunkZ) {
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
            RevolutionMod.logger.debug("Skipped persisting " + removed + " managed mob(s) in chunk " + chunkX + ", " + chunkZ);
        }
    }

    public void chunkUnloaded(World world, int chunkX, int chunkZ) {
        if (world.isRemote || serverWorld == null || world != serverWorld) return;
        for (MobRuntimeState spawn : spawns) {
            if (!isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = true;
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk unloaded: " + spawn.definition.name);
        }
    }

    public void chunkLoaded(World world, int chunkX, int chunkZ) {
        if (world.isRemote || serverWorld == null || world != serverWorld) return;
        for (MobRuntimeState spawn : spawns) {
            if (!isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = false;
            EntityLiving existing = findExistingCurrentSessionEntity(spawn);
            if (existing != null) {
                spawn.entity = existing;
                continue;
            }
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk loaded; scheduling clean respawn: " + spawn.definition.name);
        }
    }

    public boolean applyDamage(EntityLivingBase entity, DamageSource source, float damage) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            if (!(source.getTrueSource() instanceof EntityPlayerMP)) { return true; }
            EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
            spawn.currentHp = MobHealthRules.remainingHealthAfterDamage(spawn.currentHp, Math.max(0.0D, damage));
            if (spawn.currentHp.signum() <= 0) finishManagedDeath(spawn, player);
            else { syncMobMetadata(spawn, spawn.entity); spawn.entity.setHealth((float) MobHealthRules.nativeHealthFor(spawn.definition.hp)); }
            return true;
        }
        return false;
    }

    public void died(EntityLivingBase entity, DamageSource source) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            EntityPlayerMP player = source.getTrueSource() instanceof EntityPlayerMP ? (EntityPlayerMP) source.getTrueSource() : null;
            finishManagedDeath(spawn, player); break;
        }
    }

    private void finishManagedDeath(MobRuntimeState spawn, EntityPlayerMP player) {
        if (spawn == null || spawn.entity == null) return;
        if (player != null) rewards.grant(player, spawn.definition);
        spawn.currentHp = BigDecimal.ZERO;
        spawn.entity.setDead(); spawn.entity = null; spawn.respawnTimer = 10;
        RevolutionMod.logger.info("Mob " + spawn.definition.name + " killed. Respawn in 0.5s.");
    }

    public void tick() {
        for (MobRuntimeState spawn : spawns) {
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
                RevolutionMod.logger.warn("Managed mob lost from world registry while spawn chunk stayed loaded; scheduling clean respawn: " + spawn.definition.name
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

    private boolean isManagedOrLegacyCustomMob(Entity entity) {
        if (entity == null) return false;
        if (!entity.getEntityData().getString(SESSION_TAG).isEmpty()) return true;
        return entity.hasCustomName() && isCustomMobName(entity.getCustomNameTag());
    }

    private boolean isManagedOrLegacyCustomMobNbt(NBTTagCompound entityTag) {
        if (entityTag == null) return false;
        if (entityTag.hasKey("ForgeData", 10) && !entityTag.getCompoundTag("ForgeData").getString(SESSION_TAG).isEmpty()) return true;
        return entityTag.hasKey("CustomName", 8) && isCustomMobName(entityTag.getString("CustomName"));
    }

    private boolean isCustomMobName(String rawName) {
        if (rawName == null || !rawName.startsWith("§c")) return false;
        return rawName.substring(2).split("\\|").length >= 5;
    }

    public void clear() {
        spawns.clear();
        poisonDamagePlayers.clear();
        serverWorld = null;
        poisonTickCounter = 0;
    }

    public boolean suppressVanillaDrops(EntityLivingBase entity) {
        return (entity instanceof EntityZombie || entity instanceof EntitySkeleton) && isManagedOrLegacyCustomMob(entity);
    }

    public void removeStaleEntity(Entity entity) {
        entity.setDead();
        RevolutionMod.logger.debug("Prevented stale persisted managed mob from rejoining world: " + entity.getUniqueID());
    }

    public boolean manages(EntityLivingBase entity) {
        for (MobRuntimeState spawn : spawns) if (spawn.entity == entity) return true;
        return false;
    }
}
