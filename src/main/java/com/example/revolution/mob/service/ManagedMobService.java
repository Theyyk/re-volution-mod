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
import com.example.revolution.mob.rules.MobContributionRules;
import com.example.revolution.mob.rules.MobHealthRules;
import com.example.revolution.mob.state.MobRuntimeState;
import com.example.revolution.number.LargeNumbers;
import com.example.revolution.realm.definition.RealmCatalog;
import com.example.revolution.realm.definition.RealmDefinition;
import com.example.revolution.rune.rules.RunePoisonAoeRules;
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
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public final class ManagedMobService {
    private Runnable configurationGuard = () -> { };
    public void setConfigurationGuard(Runnable guard) { configurationGuard = guard; }
    public void requireConfigurationReady() { configurationGuard.run(); }
    private static final int POISON_TICK_INTERVAL = RunePoisonAoeRules.MC_TICK_INTERVAL;
    private static final int INITIAL_SPAWN_DELAY_TICKS = 20;
    private static final int LOST_ENTITY_RESPAWN_DELAY_TICKS = 1;
    private static final int RANDOM_SURFACE_ATTEMPTS = 64;
    private static final int MAX_SPAWN_RISE = 3;
    private static final int COMBAT_PAUSE_TICKS = 20;
    private static final int ROAM_TARGET_ATTEMPTS = 16;
    private static final int SEPARATION_CHECK_INTERVAL = 10;
    private static final long LOCKED_NOTICE_COOLDOWN_TICKS = 40L;
    private static final double HARD_BOUNDARY_MARGIN = 2.0D;
    private static final double SOFT_BOUNDARY_MARGIN = 1.0D;
    private static final double ROAM_SPEED = 1.0D;
    private static final double SEPARATION_RADIUS = 3.5D;
    private static final double HARD_PERSONAL_SPACE = 1.5D;
    private static final double PREFERRED_TARGET_SPACING = 4.0D;
    private static final double SEPARATION_STEP = 2.5D;
    private static final String SESSION_TAG = "revolutionManagedSession";
    private static final String SPAWN_KEY_TAG = "revolutionManagedSpawnKey";
    private static final String LOCATION_ID_TAG = "revolutionLocationId";
    private static final String SPAWN_ID_TAG = "revolutionSpawnId";
    private static final String REALM_ID_TAG = "revolutionRealmId";

    private final MobRepository mobRepository;
    private final LocationRepository locationRepository;
    private final RuneMobEffectService runeEffects;
    private final MobRewardService rewards;
    private final MobProgressionService progression;
    private final Random random = new Random();
    private final List<MobRuntimeState> spawns = new ArrayList<>();
    private final Set<UUID> poisonDamagePlayers = new HashSet<>();
    private final Map<UUID, Long> progressionNoticeTicks = new HashMap<>();
    private MinecraftServer server;
    private LocationService locationService;
    private int poisonTickCounter;
    private String managedSessionId = UUID.randomUUID().toString();

    public ManagedMobService(
            MobRepository mobRepository,
            LocationRepository locationRepository,
            RuneMobEffectService runeEffects,
            MobRewardService rewards,
            MobProgressionService progression) {
        this.mobRepository = mobRepository;
        this.locationRepository = locationRepository;
        this.runeEffects = runeEffects;
        this.rewards = rewards;
        this.progression = progression;
    }

    public void loadFromDatabase(MinecraftServer server) {
        configurationGuard.run();
        this.server = server;
        poisonDamagePlayers.clear();
        progressionNoticeTicks.clear();
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
        progression.reloadConfiguration(locations, acceptedStoredSpawns);

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
        if (server == null || spawn == null) return null;
        return DimensionManager.getWorld(spawn.dimensionId);
    }

    private boolean resolveInitialPosition(MobRuntimeState spawn, World world) {
        if (spawn.initialResolved) return true;
        if (spawn.isLegacy()) {
            spawn.initialX = spawn.definition.x;
            spawn.initialY = spawn.definition.y;
            spawn.initialZ = spawn.definition.z;
            spawn.initialResolved = true;
            return true;
        }
        if (spawn.spawnDefinition.getSpawnMode() == SpawnMode.FIXED_POINT) {
            Double fixedY = findSafeSurfaceY(world, spawn.definition.x, spawn.definition.z, spawn.definition.y);
            if (fixedY == null) {
                if (!spawn.spawnFailureLogged) {
                    spawn.spawnFailureLogged = true;
                    RevolutionMod.logger.warn("No safe surface found for fixed Mob Spawn " + spawn.spawnDefinition.getSpawnId()
                            + " in Location " + spawn.location.getLocationId() + "; retrying later.");
                }
                return false;
            }
            spawn.initialX = spawn.definition.x;
            spawn.initialY = fixedY;
            spawn.initialZ = spawn.definition.z;
            spawn.initialResolved = true;
            spawn.spawnFailureLogged = false;
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
            spawn.spawnFailureLogged = false;
            return true;
        }
        Double fallbackY = findSafeSurfaceY(world, spawn.definition.x, spawn.definition.z, spawn.definition.y);
        if (fallbackY != null) {
            spawn.initialX = spawn.definition.x;
            spawn.initialY = fallbackY;
            spawn.initialZ = spawn.definition.z;
            spawn.initialResolved = true;
            spawn.spawnFailureLogged = false;
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
        for (int offset = 0; offset <= MAX_SPAWN_RISE; offset++) {
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
        for (int y = centerY - MAX_SPAWN_RISE - 1; y >= 2; y--) {
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
        if (state.getBlock() instanceof BlockLeaves || state.getBlock() instanceof BlockLog) return null;
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
        configurationGuard.run();
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
            Double poisonDps = runeEffects.poisonDamage(player.getUniqueID(), player.world.getTotalWorldTime());
            if (poisonDps == null || poisonDps <= 0.0D) continue;

            List<EntityLiving> targets = new ArrayList<>();
            for (MobRuntimeState spawn : spawns) {
                EntityLiving target = spawn.entity;
                if (target == null || target.isDead || target.world != player.world || spawn.currentHp.signum() <= 0) continue;
                if (!MobHealthRules.isWithinStatusRadiusSq(player.getDistanceSq(target))) continue;
                targets.add(target);
            }
            double perTargetTick = RunePoisonAoeRules.perTargetTick(poisonDps, targets.size());
            if (perTargetTick <= 0.0D) continue;

            UUID playerUuid = player.getUniqueID();
            poisonDamagePlayers.add(playerUuid);
            try {
                for (EntityLiving target : targets) {
                    if (target.isDead) continue;
                    target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float) perTargetTick);
                }
            } finally {
                poisonDamagePlayers.remove(playerUuid);
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

    private void resetCombatAttempt(MobRuntimeState spawn) {
        if (spawn == null) return;
        spawn.currentHp = spawn.definition.hp;
        spawn.contributions.clear();
        spawn.combatPauseTicks = 0;
    }

    private void syncFullHealth(MobRuntimeState spawn, EntityLiving entity) {
        if (spawn == null || entity == null || entity.isDead) return;
        syncMobMetadata(spawn, entity);
        entity.setHealth((float) MobHealthRules.nativeHealthFor(spawn.definition.hp));
    }

    private boolean isPlayerInsideContributionZone(MobRuntimeState spawn, EntityPlayerMP player) {
        if (spawn == null || player == null || spawn.isLegacy()) return false;
        World world = worldFor(spawn);
        return world != null && player.world == world && spawn.location.contains(player.posX, player.posZ);
    }

    private void pruneContributions(MobRuntimeState spawn) {
        if (spawn == null || spawn.isLegacy() || spawn.contributions.isEmpty()) return;
        for (UUID playerUuid : new ArrayList<>(spawn.contributions.participants())) {
            EntityPlayerMP player = server == null ? null : server.getPlayerList().getPlayerByUUID(playerUuid);
            if (isPlayerInsideContributionZone(spawn, player)) continue;
            BigDecimal returned = spawn.contributions.remove(playerUuid);
            if (returned.signum() <= 0) continue;
            spawn.currentHp = MobContributionRules.restoreHealth(spawn.currentHp, spawn.definition.hp, returned);
            if (spawn.entity != null && !spawn.entity.isDead) syncFullHealth(spawn, spawn.entity);
            RevolutionMod.logger.debug("Rolled back managed mob contribution after player left zone: spawnKey="
                    + spawn.definition.spawnKey + " player=" + playerUuid + " returnedHp=" + LargeNumbers.canonical(returned)
                    + " currentHp=" + LargeNumbers.canonical(spawn.currentHp));
        }
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
        resetCombatAttempt(spawn);
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
        spawn.separationCooldown = random.nextInt(SEPARATION_CHECK_INTERVAL + 1);
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

        if (spawn.combatPauseTicks > 0) {
            spawn.combatPauseTicks--;
            entity.getNavigator().clearPath();
            entity.motionX = 0.0D;
            entity.motionZ = 0.0D;
            return;
        }

        if (distance >= Math.max(0.0D, spawn.location.getRadius() - SOFT_BOUNDARY_MARGIN)) {
            if (spawn.roamCooldown <= 0 || entity.getNavigator().noPath()) {
                moveToward(entity, spawn.location.getCenterX(), spawn.location.getCenterZ());
            }
            spawn.roamCooldown = 20;
            return;
        }

        if (spawn.separationCooldown > 0) spawn.separationCooldown--;
        else if (trySeparate(spawn, entity)) {
            spawn.separationCooldown = SEPARATION_CHECK_INTERVAL;
            spawn.roamCooldown = Math.max(spawn.roamCooldown, SEPARATION_CHECK_INTERVAL);
            return;
        } else {
            spawn.separationCooldown = SEPARATION_CHECK_INTERVAL;
        }

        if (spawn.roamCooldown > 0) {
            spawn.roamCooldown--;
            return;
        }

        boolean pathStarted = moveTowardSpacedTarget(spawn, entity);
        if (!pathStarted) {
            moveToward(entity, spawn.location.getCenterX(), spawn.location.getCenterZ());
            spawn.roamCooldown = 20;
        } else {
            spawn.roamCooldown = 40 + random.nextInt(61);
        }
    }

    private boolean trySeparate(MobRuntimeState spawn, EntityLiving entity) {
        double pushX = 0.0D;
        double pushZ = 0.0D;
        double nearestSq = Double.POSITIVE_INFINITY;
        boolean found = false;
        double radiusSq = SEPARATION_RADIUS * SEPARATION_RADIUS;
        for (MobRuntimeState other : spawns) {
            if (other == spawn || other.realmId != spawn.realmId || other.location == null || other.entity == null || other.entity.isDead) continue;
            if (other.location.getLocationId() != spawn.location.getLocationId() || other.entity.world != entity.world) continue;
            double dx = entity.posX - other.entity.posX;
            double dz = entity.posZ - other.entity.posZ;
            double distSq = dx * dx + dz * dz;
            if (distSq > radiusSq) continue;
            found = true;
            nearestSq = Math.min(nearestSq, distSq);
            if (distSq < 0.0001D) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                pushX += Math.cos(angle);
                pushZ += Math.sin(angle);
                continue;
            }
            double dist = Math.sqrt(distSq);
            double weight = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
            if (dist < HARD_PERSONAL_SPACE) weight += 1.0D;
            pushX += (dx / dist) * weight;
            pushZ += (dz / dist) * weight;
        }
        if (!found) return false;
        double magnitude = Math.sqrt(pushX * pushX + pushZ * pushZ);
        if (magnitude < 0.0001D) return false;
        double step = nearestSq < HARD_PERSONAL_SPACE * HARD_PERSONAL_SPACE ? SEPARATION_STEP + 1.0D : SEPARATION_STEP;
        double targetX = entity.posX + (pushX / magnitude) * step;
        double targetZ = entity.posZ + (pushZ / magnitude) * step;
        double[] clamped = clampToLocation(spawn.location, targetX, targetZ);
        return moveToward(entity, clamped[0], clamped[1]);
    }

    private boolean moveTowardSpacedTarget(MobRuntimeState spawn, EntityLiving entity) {
        double bestSpacingSq = -1.0D;
        double bestX = Double.NaN;
        double bestY = Double.NaN;
        double bestZ = Double.NaN;
        for (int attempt = 0; attempt < ROAM_TARGET_ATTEMPTS; attempt++) {
            LocationService.HorizontalPoint target = locationService.randomPoint(spawn.location.getLocationId(), random);
            Double y = findSafeSurfaceY(entity.world, target.getX(), target.getZ(), entity.posY);
            if (y == null) continue;
            double spacingSq = minimumNeighborDistanceSq(spawn, entity, target.getX(), target.getZ());
            if (spacingSq > bestSpacingSq) {
                bestSpacingSq = spacingSq;
                bestX = target.getX();
                bestY = y;
                bestZ = target.getZ();
            }
            if (spacingSq >= PREFERRED_TARGET_SPACING * PREFERRED_TARGET_SPACING && attempt >= 3) break;
        }
        if (Double.isNaN(bestX)) return false;
        return entity.getNavigator().tryMoveToXYZ(bestX, bestY, bestZ, ROAM_SPEED);
    }

    private double minimumNeighborDistanceSq(MobRuntimeState spawn, EntityLiving entity, double x, double z) {
        double minimum = Double.POSITIVE_INFINITY;
        for (MobRuntimeState other : spawns) {
            if (other == spawn || other.realmId != spawn.realmId || other.location == null || other.entity == null || other.entity.isDead) continue;
            if (other.location.getLocationId() != spawn.location.getLocationId() || other.entity.world != entity.world) continue;
            double dx = x - other.entity.posX;
            double dz = z - other.entity.posZ;
            minimum = Math.min(minimum, dx * dx + dz * dz);
        }
        return minimum;
    }

    private double[] clampToLocation(LocationDefinition location, double x, double z) {
        double dx = x - location.getCenterX();
        double dz = z - location.getCenterZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double allowed = Math.max(0.5D, location.getRadius() - 0.5D);
        if (distance <= allowed || distance < 0.0001D) return new double[] {x, z};
        double scale = allowed / distance;
        return new double[] {location.getCenterX() + dx * scale, location.getCenterZ() + dz * scale};
    }

    private boolean moveToward(EntityLiving entity, double x, double z) {
        Double y = findSafeSurfaceY(entity.world, x, z, entity.posY);
        if (y == null) return false;
        return entity.getNavigator().tryMoveToXYZ(x, y, z, ROAM_SPEED);
    }

    private void syncMobMetadata(MobRuntimeState spawn, EntityLiving entity) {
        if (spawn == null || entity == null) return;
        entity.setSilent(true);
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
            resetCombatAttempt(spawn);
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk unloaded; combat attempt reset: " + spawn.definition.spawnKey);
        }
    }

    public void chunkLoaded(World world, int chunkX, int chunkZ) {
        if (world.isRemote || server == null) return;
        for (MobRuntimeState spawn : spawns) {
            if (worldFor(spawn) != world || !isSpawnChunk(spawn, chunkX, chunkZ)) continue;
            spawn.chunkWasUnloaded = false;
            EntityLiving existing = findExistingCurrentSessionEntity(spawn);
            if (existing != null) {
                resetCombatAttempt(spawn);
                spawn.entity = existing;
                syncFullHealth(spawn, existing);
                continue;
            }
            resetCombatAttempt(spawn);
            spawn.entity = null;
            spawn.respawnTimer = LOST_ENTITY_RESPAWN_DELAY_TICKS;
            RevolutionMod.logger.debug("Managed mob spawn chunk loaded; scheduling clean full-HP respawn: " + spawn.definition.spawnKey);
        }
    }

    public boolean applyDamage(EntityLivingBase entity, DamageSource source, float damage) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            if (!(source.getTrueSource() instanceof EntityPlayerMP)) return true;
            EntityPlayerMP player = (EntityPlayerMP) source.getTrueSource();

            if (!spawn.isLegacy()) {
                pruneContributions(spawn);
                if (!isPlayerInsideContributionZone(spawn, player)) return true;
                if (!progression.isUnlocked(player.getUniqueID(), spawn.location.getLocationId())) {
                    notifyProgressionLocked(spawn, player);
                    return true;
                }
            }

            if (!isApplyingPoisonDamage(player.getUniqueID())) {
                spawn.combatPauseTicks = COMBAT_PAUSE_TICKS;
                spawn.entity.getNavigator().clearPath();
                spawn.entity.motionX = 0.0D;
                spawn.entity.motionZ = 0.0D;
            }

            if (spawn.isLegacy()) {
                spawn.currentHp = MobHealthRules.remainingHealthAfterDamage(spawn.currentHp, Math.max(0.0D, damage));
            } else {
                BigDecimal effectiveDamage = MobContributionRules.effectiveDamage(spawn.currentHp, Math.max(0.0D, damage));
                if (effectiveDamage.signum() <= 0) return true;
                spawn.contributions.add(player.getUniqueID(), effectiveDamage);
                BigDecimal remaining = spawn.currentHp.subtract(effectiveDamage);
                spawn.currentHp = remaining.signum() <= 0 ? BigDecimal.ZERO : LargeNumbers.normalize(remaining);
            }

            if (spawn.currentHp.signum() <= 0) finishManagedDeath(spawn, player);
            else {
                syncMobMetadata(spawn, spawn.entity);
                spawn.entity.setHealth((float) MobHealthRules.nativeHealthFor(spawn.definition.hp));
            }
            return true;
        }
        return false;
    }

    private void notifyProgressionLocked(MobRuntimeState spawn, EntityPlayerMP player) {
        long now = player.world.getTotalWorldTime();
        Long previousNotice = progressionNoticeTicks.get(player.getUniqueID());
        if (previousNotice != null && now - previousNotice < LOCKED_NOTICE_COOLDOWN_TICKS) return;
        progressionNoticeTicks.put(player.getUniqueID(), now);
        Integer required = progression.previousRequiredSlot(spawn.location.getLocationId());
        String suffix = required == null ? "" : " (предыдущий mobSlot " + required + ")";
        player.sendMessage(new TextComponentString("§cЭтот моб пока закрыт. Сначала победи предыдущего моба в этой Location" + suffix + "."));
    }

    public void died(EntityLivingBase entity, DamageSource source) {
        for (MobRuntimeState spawn : spawns) {
            if (spawn.entity != entity) continue;
            EntityPlayerMP player = source.getTrueSource() instanceof EntityPlayerMP ? (EntityPlayerMP) source.getTrueSource() : null;
            finishManagedDeath(spawn, player);
            break;
        }
    }

    private void grantContributionRewards(MobRuntimeState spawn) {
        if (spawn == null || spawn.isLegacy() || spawn.contributions.isEmpty() || server == null) return;
        Map<UUID, BigDecimal> contributionSnapshot = spawn.contributions.snapshot();
        BigDecimal totalContribution = spawn.contributions.total();
        UUID progressOwner = spawn.contributions.leadingContributor();
        for (Map.Entry<UUID, BigDecimal> item : contributionSnapshot.entrySet()) {
            EntityPlayerMP participant = server.getPlayerList().getPlayerByUUID(item.getKey());
            if (!isPlayerInsideContributionZone(spawn, participant)) continue;
            rewards.grantProportional(participant, spawn.definition, item.getValue(), totalContribution);
        }
        if (progressOwner != null) {
            RevolutionMod.logger.info("Managed mob contribution winner: mob=" + spawn.definition.name
                    + " spawnKey=" + spawn.definition.spawnKey + " player=" + progressOwner
                    + " contribution=" + LargeNumbers.canonical(spawn.contributions.amount(progressOwner))
                    + "/" + LargeNumbers.canonical(totalContribution));
            EntityPlayerMP winner = server.getPlayerList().getPlayerByUUID(progressOwner);
            if (isPlayerInsideContributionZone(spawn, winner)) {
                MobProgressionService.UnlockResult unlock = progression.unlockNext(progressOwner, spawn.location.getLocationId());
                if (unlock != null) {
                    String mobLabel = unlock.mobName == null || unlock.mobName.isEmpty() ? "mobSlot " + unlock.mobSlotId : unlock.mobName;
                    winner.sendMessage(new TextComponentString("§aОткрыт следующий моб: " + mobLabel
                            + " (Location " + unlock.worldLocationId + ", mobSlot " + unlock.mobSlotId + ")."));
                    RevolutionMod.logger.info("Mob progression unlocked: player=" + progressOwner
                            + " location=" + unlock.worldLocationId + " mobSlot=" + unlock.mobSlotId
                            + " mob=" + unlock.mobName);
                }
            }
        }
    }

    private void finishManagedDeath(MobRuntimeState spawn, EntityPlayerMP player) {
        if (spawn == null || spawn.entity == null) return;
        if (spawn.isLegacy()) {
            if (player != null) rewards.grant(player, spawn.definition);
        } else {
            grantContributionRewards(spawn);
        }
        spawn.currentHp = BigDecimal.ZERO;
        spawn.contributions.clear();
        spawn.combatPauseTicks = 0;
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
                if (!spawn.chunkWasUnloaded) resetCombatAttempt(spawn);
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
                resetCombatAttempt(spawn);
                syncFullHealth(spawn, spawn.entity);
            }

            if (spawn.entity != null && !spawn.entity.isDead) {
                if (isRegisteredInWorld(spawn, spawn.entity)) {
                    pruneContributions(spawn);
                    if (spawn.entity.isBurning()) spawn.entity.extinguish();
                    maintainMovement(spawn, spawn.entity);
                    continue;
                }
                RevolutionMod.logger.warn("Managed mob lost from world registry while spawn chunk stayed loaded; scheduling clean respawn: "
                        + spawn.definition.spawnKey + " entityId=" + spawn.entity.getEntityId());
                spawn.entity.setDead();
                spawn.entity = null;
                resetCombatAttempt(spawn);
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
        progressionNoticeTicks.clear();
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
