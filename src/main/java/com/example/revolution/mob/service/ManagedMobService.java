package com.example.revolution.mob.service;

import com.example.revolution.RevolutionMod;
import com.example.revolution.location.definition.LocationCatalog;
import com.example.revolution.location.definition.LocationDefinition;
import com.example.revolution.location.definition.MobSpawnDefinition;
import com.example.revolution.location.definition.MovementMode;
import com.example.revolution.location.definition.SpawnMode;
import com.example.revolution.location.definition.SpawnRealmScope;
import com.example.revolution.location.definition.StoredMobSpawn;
import com.example.revolution.location.repository.LocationRepository;
import com.example.revolution.location.service.LocationService;
import com.example.revolution.mob.definition.ManagedMobDefinition;
import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.repository.MobRepository;
import com.example.revolution.mob.rules.MobHealthRules;
import com.example.revolution.mob.state.MobRuntimeState;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import com.example.revolution.rune.service.RuneMobEffectService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public final class ManagedMobService {
    private static final int POISON_TICK_INTERVAL = 20;
    private static final int INITIAL_SPAWN_DELAY_TICKS = 20;
    private static final int LOST_ENTITY_RESPAWN_DELAY_TICKS = 1;
    private static final int RANDOM_SURFACE_ATTEMPTS = 64;
    private static final double HARD_BOUNDARY_MARGIN = 2.0D;
    private static final double SOFT_BOUNDARY_MARGIN = 1.0D;
    private static final double ROAM_SPEED = 1.0D;
    private static final String SESSION_TAG = "revolutionManagedSession";
    private static final String SPAWN_KEY_TAG = "revolutionManagedSpawnKey";
    private static final String LOCATION_ID_TAG = "revolutionLocationId";
    private static final String SPAWN_ID_TAG = "revolutionSpawnId";
    private static final String REALM_ID_TAG = "revolutionRealmId";

    private final MobRepository mobRepository;
    private final LocationRepository locationRepository;
    private final RuneMobEffectService runeEffects;
    private final MobRewardService rewards;
    private final Random random = new Random();
    private final List<MobRuntimeState> spawns = new ArrayList<>();
    private final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private MinecraftServer server;
    private LocationService locationService;
    private int poisonTickCounter;
    private String managedSessionId = UUID.randomUUID().toString();

    public ManagedMobService(
            MobRepository mobRepository,
            LocationRepository locationRepository,
            RuneMobEffectService runeEffects,
            MobRewardService rewards) {
        this.mobRepository = mobRepository;
        this.locationRepository = locationRepository;
        this.runeEffects = runeEffects;
        this.rewards = rewards;
    }

    public void loadFromDatabase(MinecraftServer server) {
        this.server = server;
        poisonDamagePlayers.clear();
        poisonTickCounter = 0;
        managedSessionId = UUID.randomUUID().toString();
        spawns.clear();

        List<MobDefinition> mobs = mobRepository.getAllMobs();
        List<LocationDefinition> locations = locationRepository.getLocations();
        List<StoredMobSpawn> storedSpawns = locationRepository.getMobSpawns();
        RevolutionMod.logger.info("Loaded mobs from DB: " + mobs.size() + ", locations: " + locations.size()
                + ", mob spawn slots: " + storedSpawns.size());

        Map<String, MobDefinition> mobsByName = new HashMap<>();
        for (MobDefinition mob : mobs) if (mob != null && mob.name != null) mobsByName.put(mob.name, mob);
        Map<String, Integer> mobIds = assignRuntimeMobIds(mobsByName.keySet());
        Map<Integer, LocationDefinition> locationsById = new HashMap<>();
        for (LocationDefinition location : locations) locationsById.put(location.getLocationId(), location);

        List<MobSpawnDefinition> foundationSpawns = new ArrayList<>();
        List<StoredMobSpawn> acceptedStoredSpawns = new ArrayList<>();
        Set<String> mobsWithCustomSpawns = new HashSet<>();
        for (StoredMobSpawn stored : storedSpawns) {
            if (stored == null || !stored.isEnabled()) continue;
            MobDefinition mob = mobsByName.get(stored.getMobName());
            Integer mobId = mobIds.get(stored.getMobName());
            LocationDefinition location = locationsById.get(stored.getLocationId());
            if (mob == null || mobId == null) {
                RevolutionMod.logger.warn("Ignoring Mob Spawn " + stored.getSpawnId() + " in Location "
                        + stored.getLocationId() + ": unknown mob " + stored.getMobName());
                continue;
            }
            if (location == null) {
                RevolutionMod.logger.warn("Ignoring Mob Spawn " + stored.getSpawnId() + ": unknown Location "
                        + stored.getLocationId());
                continue;
            }
            if (!location.contains(stored.getSpawnX(), stored.getSpawnZ())) {
                RevolutionMod.logger.warn("Ignoring Mob Spawn " + stored.getSpawnId() + " in Location "
                        + stored.getLocationId() + ": fallback point is outside Location Area");
                continue;
            }
            foundationSpawns.add(stored.toDefinition(mobId));
            acceptedStoredSpawns.add(stored);
            mobsWithCustomSpawns.add(stored.getMobName());
        }

        try {
            locationService = new LocationService(new LocationCatalog(locations, foundationSpawns));
        } catch (RuntimeException e) {
            locationService = null;
            RevolutionMod.logger.error("Location/Mob Spawn catalog is invalid; only legacy Realm 0 mob spawns will run.", e);
            acceptedStoredSpawns.clear();
            mobsWithCustomSpawns.clear();
        }

        if (locationService != null) {
            for (StoredMobSpawn stored : acceptedStoredSpawns) {
                MobDefinition mob = mobsByName.get(stored.getMobName());
                Integer mobId = mobIds.get(stored.getMobName());
                MobSpawnDefinition spawnDefinition = stored.toDefinition(mobId);
                LocationDefinition location = locationsById.get(stored.getLocationId());
                if (spawnDefinition.getRealmScope() == SpawnRealmScope.REALM_0) {
                    addRuntimeSpawn(mob, location, spawnDefinition, 0);
                } else {
                    addRuntimeSpawn(mob, location, spawnDefinition, 1);
                    addRuntimeSpawn(mob, location, spawnDefinition, 2);
                    addRuntimeSpawn(mob, location, spawnDefinition, 3);
                }
            }
        }

        for (MobDefinition mob : mobs) {
            if (!mob.enabled || mobsWithCustomSpawns.contains(mob.name)) continue;
            ManagedMobDefinition definition = managedDefinition(mob, mob.x, mob.y, mob.z,
                    "legacy|realm0|" + mob.name);
            spawns.add(new MobRuntimeState(definition));
        }

        Set<String> warmedChunks = new HashSet<>();
        Set<World> worldsToPurge = new HashSet<>();
        for (MobRuntimeState spawn : spawns) {
            World world = worldFor(spawn);
            if (world == null) continue;
            worldsToPurge.add(world);
            if (!resolveInitialPosition(spawn, world)) continue;
            BlockPos spawnPos = spawnPosition(spawn);
            String chunkKey = spawn.dimensionId + ":" + (spawnPos.getX() >> 4) + ":" + (spawnPos.getZ() >> 4);
            if (warmedChunks.add(chunkKey)) world.getChunkFromBlockCoords(spawnPos);
        }
        for (World world : worldsToPurge) purgeLoadedManagedMobs(world);
        for (MobRuntimeState spawn : spawns) spawn.respawnTimer = INITIAL_SPAWN_DELAY_TICKS;
        RevolutionMod.logger.info("Prepared " + spawns.size() + " managed physical mob slot(s); initial spawn delayed "
                + INITIAL_SPAWN_DELAY_TICKS + " ticks.");
    }

    private Map<String, Integer> assignRuntimeMobIds(Set<String> names) {
        List<String> sorted = new ArrayList<>(names);
        Collections.sort(sorted);
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) result.put(sorted.get(i), i + 1);
        return result;
    }

    private void addRuntimeSpawn(MobDefinition mob, LocationDefinition location, MobSpawnDefinition spawnDefinition, int realmId) {
        RealmDefinition realm = RealmCatalog.byRealmId(realmId);
        if (realm == null) return;
        if (server == null || server.getWorld(realm.getDimensionId()) == null) {
            RevolutionMod.logger.warn("Skipping Mob Spawn " + spawnDefinition.getSpawnId() + " for Realm " + realmId
                    + ": dimension " + realm.getDimensionId() + " is unavailable");
            return;
        }
        String key = "location|" + location.getLocationId() + "|spawn|" + spawnDefinition.getSpawnId()
                + "|realm|" + realmId + "|mob|" + mob.name;
        ManagedMobDefinition definition = managedDefinition(mob,
                spawnDefinition.getSpawnX(), spawnDefinition.getSpawnY(), spawnDefinition.getSpawnZ(), key);
        spawns.add(new MobRuntimeState(definition, realmId, realm.getDimensionId(), location, spawnDefinition));
    }

    private ManagedMobDefinition managedDefinition(MobDefinition mob, double x, double y, double z, String spawnKey) {
        return new ManagedMobDefinition(mob.name, mob.type, mob.hp,
                mobRepository.getMobResource(mob.name), mobRepository.getMobAmountValue(mob.name),
                x, y, z, spawnKey);
    }

    private World worldFor(MobRuntimeState spawn) {
        return server == null || spawn == null ? null : server.getWorld(spawn.dimensionId);
    }

    private boolean resolveInitialPosition(MobRuntimeState spawn, World world) {
        if (spawn.initialResolved) return true;
        if (spawn.isLegacy() || spawn.spawnDefinition.getSpawnMode() == SpawnMode.FIXED_POINT) {
            spawn.initialX = spawn.definition.x;
            spawn.initialY = spawn.definition.y;
            spawn.initialZ = spawn.definition.z;
            spawn.initialResolved = true;
            return true;
        }
        for (int attempt = 0; attempt < RANDOM_SURFACE_ATTEMPTS; attempt++) {
            LocationService.HorizontalPoint point = locationService.randomPoint(spawn.location.getLocationId(), random);
            Double y = findSafeSurfaceY(world, point.getX(), point.getZ(), spawn.definition.y);
            if (y == null) continue;
            spawn.initialX = point.getX();
            spawn.initialY = y;
            spawn.initialZ = point.getZ();
            spawn.initialResolved = true;
            return true;
        }
        Double fallbackY = safeStandingY(world, spawn.definition.x, spawn.definition.y, spawn.definition.z);
        if (fallbackY != null) {
            spawn.initialX = spawn.definition.x;
            spawn.initialY = fallbackY;
            spawn.initialZ = spawn.definition.z;
            spawn.initialResolved = true;
            RevolutionMod.logger.warn("Random-area surface lookup failed for Mob Spawn "
                    + spawn.spawnDefinition.getSpawnId() + "; using configured fallback point.");
            return true;
        }
        if (!spawn.spawnFailureLogged) {
            spawn.spawnFailureLogged = true;
            RevolutionMod.logger.warn("No safe surface found for Mob Spawn " + spawn.spawnDefinition.getSpawnId()
                    + " in Location " + spawn.location.getLocationId() + "; retrying later.");
        }
        return false;
    }

    private Double findSafeSurfaceY(World world, double x, double z, double preferredY) {
        int centerY = MathHelper.clamp(MathHelper.floor(preferredY), 2, Math.max(2, world.getActualHeight() - 2));
        for (int offset = 0; offset <= 24; offset++) {
            int up = centerY + offset;
            if (up < world.getActualHeight() - 1) {
                Double y = safeStandingYAtBlock(world, x, up, z);
                if (y != null) return y;
            }
            if (offset == 0) continue;
            int down = centerY - offset;
            if (down >= 2) {
                Double y = safeStandingYAtBlock(world, x, down, z);
                if (y != null) return y;
            }
        }
        return findSafeSurfaceY(world, x, z);
    }

    private Double findSafeSurfaceY(World world, double x, double z) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        int top = Math.min(world.getActualHeight() - 2, Math.max(2, world.getHeight(new BlockPos(blockX, 0, blockZ)).getY() + 1));
        for (int y = top; y >= 2; y--) {
            Double standing = safeStandingYAtBlock(world, x, y, z);
            if (standing != null) return standing;
        }
        return null;
    }

    private Double safeStandingYAtBlock(World world, double x, int y, double z) {
        int blockX = MathHelper.floor(x);
        int blockZ = MathHelper.floor(z);
        BlockPos ground = new BlockPos(blockX, y - 1, blockZ);
        BlockPos feet = new BlockPos(blockX, y, blockZ);
        BlockPos head = new BlockPos(blockX, y + 1, blockZ);
        IBlockState state = world.getBlockState(ground);
        if (state.getMaterial().blocksMovement() && !state.getMaterial().isLiquid()
                && world.isAirBlock(feet) && world.isAirBlock(head)) return (double) y;
        return null;
    }

    private Double safeStandingY(World world, double x, double y, double z) {
        return safeStandingYAtBlock(world, x, MathHelper.floor(y), z);
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
        if (removed > 0) RevolutionMod.logger.info("Removed " + removed + " stale managed mob(s) before initial spawn in dimension "
                + world.provider.getDimension() + ".");
    }

    public void respawnAll() {
        for (MobRuntimeState spawn : spawns) if (spawn.entity != null && !spawn.entity.isDead) spawn.entity.setDead();
        spawns.clear();
        if (server != null) loadFromDatabase(server);
    }

    public boolean isApplyingPoisonDamage(UUID playerUuid) {
        return playerUuid != null && poisonDamagePlayers.contains(playerUuid);
    }

    private BlockPos spawnPosition(MobRuntimeState spawn) {
        double x = spawn.initialResolved ? spawn.initialX : spawn.definition.x;
        double y = spawn.initialResolved ? spawn.initialY : spawn.definition.y;
        double z = spawn.initialResolved ? spawn.initialZ : spawn.definition.z;
        return new BlockPos(x, y, z);
    }

    private boolean isSpawnChunk(MobRuntimeState spawn, int chunkX, int chunkZ) {
        BlockPos pos = spawnPosition(spawn);
        return (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ;
    }

    private void applyPoisonTicks() {
        if (server == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            Double resolvedPoisonDamage = runeEffects.poisonDamage(player.getUniqueID(), player.world.getTotalWorldTime());
            if (resolvedPoisonDamage == null || resolvedPoisonDamage <= 0.0D) continue;
            UUID playerUuid = player.getUniqueID();
            for (MobRuntimeState spawn : spawns) {
                EntityLiving target = spawn.entity;
                if (target == null || target.isDead || target.world != player.world || spawn.currentHp.signum() <= 0) continue;
                if (!MobHealthRules.isWithinStatusRadiusSq(player.getDistanceSq(target))) continue;
                poisonDamagePlayers.add(playerUuid);
                try { target.attackEntityFrom(DamageSource.causePlayerDamage(player), resolvedPoisonDamage.floatValue()); }
                finally { poisonDamagePlayers.remove(playerUuid); }
            }
        }
    }

    private EntityLiving findExistingCurrentSessionEntity(MobRuntimeState spawn) {
        World world = worldFor(spawn);
        if (world == null || spawn == null) return null;
        for (Entity raw : new ArrayList<>(world.loadedEntityList)) {
            if (!(raw instanceof EntityLiving) || raw.isDead) continue;
            if (!managedSessionId.equals(raw.getEntityData().getString(SESSION_TAG))) continue;
            if (!spawn.definition.spawnKey.equals(raw.getEntityData().getString(SPAWN_KEY_TAG))) continue;
            return (EntityLiving) raw;
        }
        return null;
    }

    private boolean isRegisteredInWorld(MobRuntimeState spawn, EntityLiving entity) {
        World world = worldFor(spawn);
        if (world == null || entity == null || entity.isDead || entity.world != world) return false;
        if (!world.loadedEntityList.contains(entity)) return false;
        return world.getEntityByID(entity.getEntityId()) == entity;
    }

    private void spawnMob(MobRuntimeState spawn) {
        World world = worldFor(spawn);
        if (world == null) return;
        if (!resolveInitialPosition(spawn, world)) {
            spawn.respawnTimer = 20;
            return;
        }

        EntityLiving existing = findExistingCurrentSessionEntity(spawn);
        if (existing != null) {
            spawn.entity = existing;
            spawn.spawnFailureLogged = false;
            return;
        }

        BlockPos spawnPos = spawnPosition(spawn);
        world.getChunkFromBlockCoords(spawnPos);
        EntityLiving entity = spawn.definition.type.equalsIgnoreCase("skeleton") ? new EntitySkeleton(world) : new EntityZombie(world);
        spawn.currentHp = spawn.definition.hp;
        float yaw = spawn.spawnDefinition == null ? 0.0F : spawn.spawnDefinition.getYaw();
        float pitch = spawn.spawnDefinition == null ? 0.0F : spawn.spawnDefinition.getPitch();
        entity.setLocationAndAngles(spawn.initialX, spawn.initialY, spawn.initialZ, yaw, pitch);
        syncMobMetadata(spawn, entity);
        entity.getEntityData().setString(SESSION_TAG, managedSessionId);
        entity.getEntityData().setString(SPAWN_KEY_TAG, spawn.definition.spawnKey);
        entity.getEntityData().setInteger(REALM_ID_TAG, spawn.realmId);
        if (spawn.spawnDefinition != null) {
            entity.getEntityData().setInteger(LOCATION_ID_TAG, spawn.spawnDefinition.getLocationId());
            entity.getEntityData().setInteger(SPAWN_ID_TAG, spawn.spawnDefinition.getSpawnId());
        }
        entity.setAlwaysRenderNameTag(true);
        entity.setFire(0);
        double nativeHealth = MobHealthRules.nativeHealthFor(spawn.definition.hp);
        entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(nativeHealth);
        entity.setHealth((float) nativeHealth);
        entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        entity.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(0.0D);
        boolean roaming = spawn.spawnDefinition != null && spawn.spawnDefinition.getMovementMode() == MovementMode.ROAMING;
        entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(roaming ? 0.23D : 0.0D);
        if (roaming) clearVanillaTasks(entity);
        entity.setNoAI(!roaming);
        entity.forceSpawn = true;
        if (!world.spawnEntity(entity)) {
            spawn.entity = null;
            spawn.respawnTimer = 20;
            if (!spawn.spawnFailureLogged) {
                spawn.spawnFailureLogged = true;
                RevolutionMod.logger.warn("Could not spawn managed mob; retrying once per second: " + spawn.definition.name
                        + " spawnKey=" + spawn.definition.spawnKey);
            }
            return;
        }
        spawn.spawnFailureLogged = false;
        spawn.entity = entity;
        spawn.roamCooldown = 1;
        if (!spawn.hasSpawnedOnce) {
            spawn.hasSpawnedOnce = true;
            RevolutionMod.logger.info("Spawned mob: " + spawn.definition.name + " (" + spawn.definition.type + ") at "
                    + spawn.initialX + ", " + spawn.initialY + ", " + spawn.initialZ + " realm=" + spawn.realmId
                    + (spawn.spawnDefinition == null ? " legacy" : " location=" + spawn.spawnDefinition.getLocationId()
                    + " spawn=" + spawn.spawnDefinition.getSpawnId() + " " + spawn.spawnDefinition.getSpawnMode()
                    + "/" + spawn.spawnDefinition.getMovementMode())
                    + " with custom HP " + LargeNumbers.canonical(spawn.definition.hp) + " (native backing HP " + nativeHealth + ")");
        } else {
            RevolutionMod.logger.debug("Respawned managed mob: " + spawn.definition.name + " entityId=" + entity.getEntityId());
        }
    }

    private void clearVanillaTasks(EntityLiving entity) {
        for (EntityAITasks.EntityAITaskEntry entry : new ArrayList<>(entity.tasks.taskEntries)) {
            entity.tasks.removeTask(entry.action);
        }
        for (EntityAITasks.EntityAITaskEntry entry : new ArrayList<>(entity.targetTasks.taskEntries)) {
            entity.targetTasks.removeTask(entry.action);
        }
        entity.setAttackTarget(null);
        entity.getNavigator().clearPath();
    }

    private void maintainMovement(MobRuntimeState spawn, EntityLiving entity) {
        if (spawn.spawnDefinition == null || spawn.location == null
                || spawn.spawnDefinition.getMovementMode() == MovementMode.STATIONARY) {
            if (entity.getDistanceSq(spawn.initialX, spawn.initialY, spawn.initialZ) > 0.25D) {
                entity.setLocationAndAngles(spawn.initialX, spawn.initialY, spawn.initialZ, entity.rotationYaw, entity.rotationPitch);
            }
            return;
        }

        entity.setAttackTarget(null);
        double dx = entity.posX - spawn.location.getCenterX();
        double dz = entity.posZ - spawn.location.getCenterZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > spawn.location.getRadius() + HARD_BOUNDARY_MARGIN) {
            entity.getNavigator().clearPath();
            entity.setLocationAndAngles(spawn.initialX, spawn.initialY, spawn.initialZ, entity.rotationYaw, entity.rotationPitch);
            spawn.roamCooldown = 20;
            RevolutionMod.logger.debug("Returned roaming mob to initial point after leaving Location boundary: "
                    + spawn.definition.spawnKey);
            return;
        }

        if (distance >= Math.max(0.0D, spawn.location.getRadius() - SOFT_BOUNDARY_MARGIN)) {
            if (spawn.roamCooldown <= 0 || entity.getNavigator().noPath()) {
                moveToward(entity, spawn.location.getCenterX(), spawn.location.getCenterZ());
            }
            spawn.roamCooldown = 20;
            return;
        }

        if (spawn.roamCooldown > 0) {
            spawn.roamCooldown--;
            return;
        }

        boolean pathStarted = false;
        for (int attempt = 0; attempt < 8 && !pathStarted; attempt++) {
            LocationService.HorizontalPoint target = locationService.randomPoint(spawn.location.getLocationId(), random);
            pathStarted = moveToward(entity, target.getX(), target.getZ());
        }
        if (!pathStarted) {
            moveToward(entity, spawn.location.getCenterX(), spawn.location.getCenterZ());
            spawn.roamCooldown = 20;
        } else {
            spawn.roamCooldown = 40 + random.nextInt(61);
        }
    }

    private boolean moveToward(EntityLiving entity, double x, double z) {
        Double y = findSafeSurfaceY(entity.world, x, z, entity.posY);
        if (y == null) return false;
        return entity.getNavigator().tryMoveToXYZ(x, y, z, ROAM_SPEED);
    }

    private void syncMobMetadata(MobRuntimeState spawn, EntityLiving entity) {
        if (spawn == null || entity == null) return;
        entity.setCustomNameTag("§c" + spawn.definition.name + "|" + LargeNumbers.canonical(spawn.currentHp) + "|"
                + LargeNumbers.canonical(spawn.definition.hp) + "|" + spawn.definition.resource + "|"
                + LargeNumbers.canonical(spawn.definition.amount));
    }

    public boolean isStaleEntity(Entity entity, World world) {
        if (world.isRemote) return false;
        if (!(entity instanceof EntityZombie) && !(entity instanceof EntitySkeleton)) return false;
        if (!isManagedOrLegacyCustomMob(entity)) return false;
        String entitySessionId = entity.getEntityData().getString(SESSION_TAG);
        return !managedSessionId.equals(entitySessionId);
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
            entities.removeTag(i);
            removed++;
        }
        if (removed > 0) {
            level.setTag("Entities", entities);
            root.setTag("Level", level);
            RevolutionMod.logger.debug("Skipped persisting " + removed + " managed mob(s) in chunk " + chunkX + ", " + chunkZ);
        }
    }

    public void chunkUnloaded(World world, int chunkX, int chunkZ) {
        if (world.isRemote || server == null) return;
        for (MobRuntimeState spawn : spawns) {
            if (worldFor(spawn) != world || !isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = true;
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk unloaded: " + spawn.definition.spawnKey);
        }
    }

    public void chunkLoaded(World world, int chunkX, int chunkZ) {
        if (world.isRemote || server == null) return;
        for (MobRuntimeState spawn : spawns) {
            if (worldFor(spawn) != world || !isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = false;
            EntityLiving existing = findExistingCurrentSessionEntity(spawn);
            if (existing != null) {
                spawn.entity = existing;
                continue;
            }
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk loaded; scheduling clean respawn: " + spawn.definition.spawnKey);
        }
    }

    public boolean applyDamage(EntityLivingBase entity, DamageSource source, float damage) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            if (!(source.getTrueSource() instanceof EntityPlayerMP)) return true;
            EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();
            spawn.currentHp = MobHealthRules.remainingHealthAfterDamage(spawn.currentHp, Math.max(0.0D, damage));
            if (spawn.currentHp.signum() <= 0) finishManagedDeath(spawn, player);
            else {
                syncMobMetadata(spawn, spawn.entity);
                spawn.entity.setHealth((float) MobHealthRules.nativeHealthFor(spawn.definition.hp));
            }
            return true;
        }
        return false;
    }

    public void died(EntityLivingBase entity, DamageSource source) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            EntityPlayerMP player = source.getTrueSource() instanceof EntityPlayerMP ? (EntityPlayerMP) source.getTrueSource() : null;
            finishManagedDeath(spawn, player);
            break;
        }
    }

    private void finishManagedDeath(MobRuntimeState spawn, EntityPlayerMP player) {
        if (spawn == null || spawn.entity == null) return;
        if (player != null) rewards.grant(player, spawn.definition);
        spawn.currentHp = BigDecimal.ZERO;
        spawn.entity.setDead();
        spawn.entity = null;

        boolean randomAreaRespawn = spawn.spawnDefinition != null
                && spawn.spawnDefinition.getSpawnMode() == SpawnMode.RANDOM_AREA;
        if (randomAreaRespawn) {
            spawn.initialResolved = false;
            World world = worldFor(spawn);
            if (world != null && resolveInitialPosition(spawn, world)) {
                world.getChunkFromBlockCoords(spawnPosition(spawn));
            }
        }

        spawn.respawnTimer = 10;
        RevolutionMod.logger.info("Mob " + spawn.definition.name + " killed. Respawn in 0.5s "
                + (randomAreaRespawn ? "at a freshly rolled random Location point." : "at its fixed slot point."));
    }

    public void tick() {
        for (MobRuntimeState spawn : spawns) {
            World world = worldFor(spawn);
            if (world == null) continue;
            if (!resolveInitialPosition(spawn, world)) {
                if (spawn.respawnTimer > 0) spawn.respawnTimer--;
                continue;
            }
            boolean spawnChunkLoaded = world.isBlockLoaded(spawnPosition(spawn));
            if (!spawnChunkLoaded) {
                spawn.chunkWasUnloaded = true;
                continue;
            }

            if (spawn.chunkWasUnloaded) {
                spawn.chunkWasUnloaded = false;
                if (spawn.entity != null && !isRegisteredInWorld(spawn, spawn.entity)) {
                    if (!spawn.entity.isDead) spawn.entity.setDead();
                    spawn.entity = null;
                }
                if (spawn.entity == null) {
                    spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
                    continue;
                }
            }

            if (spawn.entity != null && !spawn.entity.isDead) {
                if (isRegisteredInWorld(spawn, spawn.entity)) {
                    if (spawn.entity.isBurning()) spawn.entity.extinguish();
                    maintainMovement(spawn, spawn.entity);
                    continue;
                }
                RevolutionMod.logger.warn("Managed mob lost from world registry while spawn chunk stayed loaded; scheduling clean respawn: "
                        + spawn.definition.spawnKey + " entityId=" + spawn.entity.getEntityId());
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

            if (spawn.respawnTimer > 0) spawn.respawnTimer--;
            else spawnMob(spawn);
        }
        if (++poisonTickCounter >= POISON_TICK_INTERVAL) {
            poisonTickCounter = 0;
            applyPoisonTicks();
        }
    }

    private boolean isManagedOrLegacyCustomMob(Entity entity) {
        if (entity == null) return false;
        if (!entity.getEntityData().getString(SESSION_TAG).isEmpty()) return true;
        return entity.hasCustomName() && isCustomMobName(entity.getCustomNameTag());
    }

    private boolean isManagedOrLegacyCustomMobNbt(NBTTagCompound entityTag) {
        if (entityTag == null) return false;
        if (entityTag.hasKey("ForgeData", 10)
                && !entityTag.getCompoundTag("ForgeData").getString(SESSION_TAG).isEmpty()) return true;
        return entityTag.hasKey("CustomName", 8) && isCustomMobName(entityTag.getString("CustomName"));
    }

    private boolean isCustomMobName(String rawName) {
        if (rawName == null || !rawName.startsWith("§c")) return false;
        return rawName.substring(2).split("\\|").length >= 5;
    }

    public void clear() {
        spawns.clear();
        poisonDamagePlayers.clear();
        server = null;
        locationService = null;
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
