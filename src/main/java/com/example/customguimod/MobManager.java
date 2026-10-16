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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MobManager {

    private static final int POISON_TICK_INTERVAL = 20;
    private static final List<MobSpawn> spawns = new ArrayList<>();
    private static final Map<UUID, UUID> selectedPoisonTargets = new HashMap<>();
    private static final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private static World serverWorld;
    private static int poisonTickCounter;

    public static class MobSpawn {
        public String name;
        public String type;
        public int hp;
        public String resource;
        public int amount;
        public double x, y, z;
        public EntityLiving entity;
        public int respawnTimer;

        public MobSpawn(String name, String type, int hp, String resource, int amount, double x, double y, double z) {
            this.name = name;
            this.type = type;
            this.hp = hp;
            this.resource = resource;
            this.amount = amount;
            this.x = x;
            this.y = y;
            this.z = z;
            this.entity = null;
            this.respawnTimer = 0;
        }
    }

    public static void loadFromDatabase(World world) {
        serverWorld = world;
        selectedPoisonTargets.clear();
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

            spawns.add(spawn);
            spawnMob(spawn);
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

    static void selectPoisonTarget(UUID playerUuid, EntityLiving target) {
        if (playerUuid == null || target == null || target.isDead || !isManagedMob(target)) return;
        selectedPoisonTargets.put(playerUuid, target.getUniqueID());
    }

    static boolean isApplyingPoisonDamage(UUID playerUuid) {
        return playerUuid != null && poisonDamagePlayers.contains(playerUuid);
    }

    private static boolean isManagedMob(EntityLiving target) {
        for (MobSpawn spawn : spawns) {
            if (spawn.entity == target && !target.isDead) return true;
        }
        return false;
    }

    private static EntityLiving poisonTarget(EntityPlayerMP player) {
        UUID selectedUuid = selectedPoisonTargets.get(player.getUniqueID());
        EntityLiving nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (MobSpawn spawn : spawns) {
            EntityLiving entity = spawn.entity;
            if (entity == null || entity.isDead || entity.getHealth() <= 0.0F) continue;
            if (selectedUuid != null && selectedUuid.equals(entity.getUniqueID())) return entity;

            double distance = player.getDistanceSq(entity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = entity;
            }
        }

        if (selectedUuid != null) selectedPoisonTargets.remove(player.getUniqueID(), selectedUuid);
        return nearest;
    }

    private static void applyPoisonTicks() {
        if (serverWorld == null || serverWorld.getMinecraftServer() == null) return;
        long worldTick = serverWorld.getTotalWorldTime();

        for (EntityPlayerMP player : serverWorld.getMinecraftServer().getPlayerList().getPlayers()) {
            RuneCombatMath.Snapshot snapshot = RuneCombatService.resolve(player.getUniqueID(), worldTick);
            if (snapshot == null) continue;

            double poisonDamage = RuneCombatMath.poisonDamage(snapshot);
            if (poisonDamage <= 0.0D) continue;

            EntityLiving target = poisonTarget(player);
            if (target == null) continue;

            UUID playerUuid = player.getUniqueID();
            poisonDamagePlayers.add(playerUuid);
            try {
                target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) poisonDamage);
            } finally {
                poisonDamagePlayers.remove(playerUuid);
            }
        }
    }

    private static void spawnMob(MobSpawn spawn) {
        EntityLiving entity;

        if (spawn.type.equalsIgnoreCase("skeleton")) {
            entity = new EntitySkeleton(serverWorld);
        } else {
            entity = new EntityZombie(serverWorld);
        }

        entity.setPosition(spawn.x, spawn.y, spawn.z);
        entity.setCustomNameTag("§c" + spawn.name + "|" + spawn.hp + "|" + spawn.hp + "|" + spawn.resource + "|" + spawn.amount);
        entity.setAlwaysRenderNameTag(true);
        entity.setFire(0);
        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(spawn.hp);
        entity.setHealth(spawn.hp);
        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0D);
        entity.setNoAI(true);

        serverWorld.spawnEntity(entity);
        spawn.entity = entity;
        CustomGuiMod.logger.info("Spawned mob: " + spawn.name + " (" + spawn.type + ") at " + spawn.x + ", " + spawn.y + ", " + spawn.z);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        for (MobSpawn spawn : spawns) {
            if (spawn.entity == event.getEntityLiving()) {
                net.minecraft.util.DamageSource source = event.getSource();
                if (source.getTrueSource() instanceof EntityPlayerMP) return;
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        for (MobSpawn spawn : spawns) {
            if (spawn.entity == event.getEntityLiving()) {
                if (event.getSource().getTrueSource() instanceof EntityPlayerMP) {
                    EntityPlayerMP player = (EntityPlayerMP) event.getSource().getTrueSource();
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

                UUID deadUuid = spawn.entity.getUniqueID();
                selectedPoisonTargets.values().removeIf(deadUuid::equals);
                spawn.entity.setDead();
                spawn.entity = null;
                spawn.respawnTimer = 10;
                CustomGuiMod.logger.info("Mob " + spawn.name + " killed. Respawn in 0.5s.");
                break;
            }
        }
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
