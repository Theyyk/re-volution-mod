package com.example.customguimod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MobManager {

    private static final int POISON_TICK_INTERVAL = 20;
    private static final double STATUS_EFFECT_RADIUS = 2.0D;
    private static final double STATUS_EFFECT_RADIUS_SQ = STATUS_EFFECT_RADIUS * STATUS_EFFECT_RADIUS;
    private static final double VANILLA_MAX_HEALTH = 1024.0D;
    private static final List<MobSpawn> spawns = new ArrayList<>();
    private static final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private static World serverWorld;
    private static int poisonTickCounter;

    public static class MobSpawn {
        public String name;
        public String type;
        public int hp;
        public double currentHp;
        public String resource;
        public int amount;
        public double x, y, z;
        public EntityLiving entity;
        public int respawnTimer;
        public boolean spawnFailureLogged;

        public MobSpawn(String name, String type, int hp, String resource, int amount, double x, double y, double z) {
            this.name = name;
            this.type = type;
            this.hp = hp;
            this.currentHp = Math.max(1, hp);
            this.resource = resource;
            this.amount = amount;
            this.x = x;
            this.y = y;
            this.z = z;
            this.entity = null;
            this.respawnTimer = 0;
            this.spawnFailureLogged = false;
        }
    }

    public static void loadFromDatabase(World world) {
        serverWorld = world;
        poisonDamagePlayers.clear();
        poisonTickCounter = 0;

        for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
            if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) continue;
            if (!entity.hasCustomName()) continue;

            String rawName = entity.getCustomNameTag();
            if (isCustomMobName(rawName)) {
                entity.setDead();
                CustomGuiMod.logger.info("Removed old persisted custom mob: " + rawName);
            }
        }

        spawns.clear();

        List<MongoManager.CustomMob> mobs = MongoManager.getAllMobs();
        CustomGuiMod.logger.info("Loaded mobs from DB: " + mobs.size());

        for (MongoManager.CustomMob mob : mobs) {
            if (!mob.enabled) continue;

            MobSpawn spawn = new MobSpawn(
                    mob.name,
                    mob.type,
                    mob.hp,
                    ResourceManager.getMobResource(mob.name),
                    ResourceManager.getMobAmount(mob.name),
                    mob.x,
                    mob.y,
                    mob.z
            );

            // Spawn on the first completed server tick instead of FMLServerStarting.
            // This avoids creating tracked entities before the world/entity tracker is fully ready.
            spawn.respawnTimer = 1;
            spawns.add(spawn);
        }
    }

    public static void respawnAll() {
        for (MobSpawn spawn : spawns) {
            if (spawn.entity != null && !spawn.entity.isDead) {
                spawn.entity.setDead();
            }
        }
        spawns.clear();
        if (serverWorld != null) loadFromDatabase(serverWorld);
    }

    static boolean isApplyingPoisonDamage(UUID playerUuid) {
        return playerUuid != null && poisonDamagePlayers.contains(playerUuid);
    }

    static boolean isWithinStatusRadiusSq(double distanceSq) {
        return Double.isFinite(distanceSq) && distanceSq >= 0.0D && distanceSq <= STATUS_EFFECT_RADIUS_SQ;
    }

    static double nativeHealthFor(int configuredHp) {
        return Math.max(1.0D, Math.min(VANILLA_MAX_HEALTH, configuredHp));
    }

    static double remainingHealthAfterDamage(double currentHp, double damage) {
        if (!Double.isFinite(currentHp) || currentHp <= 0.0D) return 0.0D;
        if (!Double.isFinite(damage) || damage <= 0.0D) return currentHp;
        return Math.max(0.0D, currentHp - damage);
    }

    private static void applyPoisonTicks() {
        if (serverWorld == null || serverWorld.getMinecraftServer() == null) return;
        long worldTick = serverWorld.getTotalWorldTime();

        for (EntityPlayerMP player : serverWorld.getMinecraftServer().getPlayerList().getPlayers()) {
            RuneCombatMath.Snapshot snapshot = RuneCombatService.resolve(player.getUniqueID(), worldTick);
            if (snapshot == null) continue;

            double poisonDamage = RuneCombatMath.poisonDamage(snapshot);
            if (poisonDamage <= 0.0D) continue;

            UUID playerUuid = player.getUniqueID();
            for (MobSpawn spawn : spawns) {
                EntityLiving target = spawn.entity;
                if (target == null || target.isDead || spawn.currentHp <= 0.0D) continue;
                if (!isWithinStatusRadiusSq(player.getDistanceSq(target))) continue;

                poisonDamagePlayers.add(playerUuid);
                try {
                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) poisonDamage);
                } finally {
                    poisonDamagePlayers.remove(playerUuid);
                }
            }
        }
    }

    private static void spawnMob(MobSpawn spawn) {
        if (serverWorld == null) return;

        EntityLiving entity;

        if (spawn.type.equalsIgnoreCase("skeleton")) {
            entity = new EntitySkeleton(serverWorld);
        } else {
            entity = new EntityZombie(serverWorld);
        }

        spawn.currentHp = Math.max(1.0D, spawn.hp);
        entity.setPosition(spawn.x, spawn.y, spawn.z);
        syncMobMetadata(spawn, entity);
        entity.setAlwaysRenderNameTag(true);
        entity.setFire(0);

        // Minecraft 1.12.2 clamps generic.maxHealth to 1024. Keep the vanilla
        // entity alive at a legal backing value while authoritative custom HP
        // lives in MobSpawn.currentHp and is synchronized through the name tag.
        double nativeHealth = nativeHealthFor(spawn.hp);
        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(nativeHealth);
        entity.setHealth((float) nativeHealth);
        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0D);
        entity.setNoAI(true);

        // Spawn points may live in chunks that are not currently loaded. Vanilla
        // World#spawnEntity rejects those entities unless forceSpawn is set.
        entity.forceSpawn = true;

        if (!serverWorld.spawnEntity(entity)) {
            spawn.entity = null;
            // Retry at most once per second instead of hammering spawnEntity every tick.
            spawn.respawnTimer = 20;
            if (!spawn.spawnFailureLogged) {
                spawn.spawnFailureLogged = true;
                CustomGuiMod.logger.warn("Could not spawn managed mob; retrying once per second: " + spawn.name);
            }
            return;
        }

        spawn.entity = entity;
        spawn.spawnFailureLogged = false;
        CustomGuiMod.logger.info("Spawned mob: " + spawn.name + " (" + spawn.type + ") at " + spawn.x + ", " + spawn.y + ", " + spawn.z
                + " with custom HP " + spawn.hp + " (native backing HP " + nativeHealth + ")");
    }

    private static void syncMobMetadata(MobSpawn spawn, EntityLiving entity) {
        if (spawn == null || entity == null) return;
        entity.setCustomNameTag("§c" + spawn.name
                + "|" + formatHealthMetadata(spawn.currentHp)
                + "|" + spawn.hp
                + "|" + spawn.resource
                + "|" + spawn.amount);
    }

    private static String formatHealthMetadata(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) return "0";
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        for (MobSpawn spawn : spawns) {
            if (spawn.entity != event.getEntityLiving()) continue;

            net.minecraft.util.DamageSource source = event.getSource();
            if (!(source.getTrueSource() instanceof EntityPlayerMP)) {
                event.setCanceled(true);
                return;
            }

            EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
            double damage = Math.max(0.0D, event.getAmount());
            spawn.currentHp = remainingHealthAfterDamage(spawn.currentHp, damage);
            event.setCanceled(true);

            if (spawn.currentHp <= 0.0D) {
                finishManagedDeath(spawn, player);
            } else {
                syncMobMetadata(spawn, spawn.entity);
                // Keep native backing health full because virtual HP is authoritative.
                spawn.entity.setHealth((float) nativeHealthFor(spawn.hp));
            }
            return;
        }
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        for (MobSpawn spawn : spawns) {
            if (spawn.entity != event.getEntityLiving()) continue;

            EntityPlayerMP player = event.getSource().getTrueSource() instanceof EntityPlayerMP
                    ? (EntityPlayerMP) event.getSource().getTrueSource()
                    : null;
            finishManagedDeath(spawn, player);
            break;
        }
    }

    private static void finishManagedDeath(MobSpawn spawn, EntityPlayerMP player) {
        if (spawn == null || spawn.entity == null) return;

        if (player != null) {
            double rewardBonusPercent = RuneRewardBonusService.percentFor(player.getUniqueID(), spawn.resource);
            int rewardAmount = MobRewardBonus.apply(spawn.amount, rewardBonusPercent);
            if (ResourceManager.grantPlayerResource(player.getUniqueID(), spawn.resource, rewardAmount)) {
                int newBalance = ResourceManager.getPlayerResource(player.getUniqueID(), spawn.resource);
                player.sendMessage(new TextComponentString(
                        "§6+" + rewardAmount + " " + ResourceManager.getDisplayName(spawn.resource)
                                + "! Баланс: " + newBalance));
                PlayerStatsService.sendTo(player);
            } else {
                CustomGuiMod.logger.warn("Skipped invalid mob reward resource: " + spawn.resource);
            }
        }

        spawn.currentHp = 0.0D;
        spawn.entity.setDead();
        spawn.entity = null;
        spawn.respawnTimer = 10;
        CustomGuiMod.logger.info("Mob " + spawn.name + " killed. Respawn in 0.5s.");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (MobSpawn spawn : spawns) {
            if (spawn.entity != null && !spawn.entity.isDead) {
                if (spawn.entity.isBurning()) spawn.entity.extinguish();
                continue;
            }

            if (spawn.respawnTimer > 0) {
                spawn.respawnTimer--;
            } else {
                spawnMob(spawn);
            }
        }

        poisonTickCounter++;
        if (poisonTickCounter >= POISON_TICK_INTERVAL) {
            poisonTickCounter = 0;
            applyPoisonTicks();
        }
    }

    @SubscribeEvent
    public void onMobDrops(LivingDropsEvent event) {
        if (!(event.getEntityLiving() instanceof EntityZombie)
                && !(event.getEntityLiving() instanceof EntitySkeleton)) return;

        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        if (!entity.hasCustomName()) return;
        if (!isCustomMobName(entity.getCustomNameTag())) return;

        event.getDrops().clear();
        event.setCanceled(true);
    }

    private static boolean isCustomMobName(String rawName) {
        if (rawName == null || !rawName.startsWith("§c")) return false;
        String[] parts = rawName.substring(2).split("\\|");
        return parts.length >= 5;
    }
}
