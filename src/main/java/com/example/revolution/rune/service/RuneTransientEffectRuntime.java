package com.example.revolution.rune.service;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.player.repository.PlayerRepository;
import com.example.revolution.rune.definition.RuneCollectionId;
import com.example.revolution.rune.definition.RuneDamageChannel;
import com.example.revolution.rune.repository.RuneCollectionStateRepository;
import com.example.revolution.rune.repository.RuneEnhancementRepository;
import com.example.revolution.rune.repository.RuneRecord;
import com.example.revolution.rune.rules.RuneDamageMath;
import com.example.revolution.rune.state.RuneInventory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Instance-owned transient scheduler for generic rune effects that happen after a confirmed hit.
 * Persistent rune state remains in repositories; pending effects disappear with the server lifetime.
 */
public final class RuneTransientEffectRuntime {
    public static final class DelayedEffectDescriptor {
        public final RuneCollectionId collectionId;
        public final int runeIndex;
        public final RuneDamageChannel channel;
        public final double multiplier;
        public final long delayTicks;
        public final String trigger;
        public final String condition;

        private DelayedEffectDescriptor(
                RuneCollectionId collectionId,
                int runeIndex,
                RuneDamageChannel channel,
                double multiplier,
                long delayTicks,
                String trigger,
                String condition) {
            this.collectionId = collectionId;
            this.runeIndex = runeIndex;
            this.channel = channel;
            this.multiplier = multiplier;
            this.delayTicks = delayTicks;
            this.trigger = trigger;
            this.condition = condition;
        }
    }

    private static final class PendingDamage {
        final EntityPlayerMP player;
        final EntityLivingBase target;
        final RuneDamageChannel channel;
        final double multiplier;
        final long dueTick;
        final String condition;

        PendingDamage(
                EntityPlayerMP player,
                EntityLivingBase target,
                RuneDamageChannel channel,
                double multiplier,
                long dueTick,
                String condition) {
            this.player = player;
            this.target = target;
            this.channel = channel;
            this.multiplier = multiplier;
            this.dueTick = dueTick;
            this.condition = condition;
        }
    }

    private final PlayerRepository players;
    private final RuneEnhancementRepository enhancements;
    private final RuneEffectBalanceService effectBalance;
    private final RuneCombatService combat;
    private final List<PendingDamage> pending = new ArrayList<>();

    public RuneTransientEffectRuntime(
            PlayerRepository players,
            RuneEnhancementRepository enhancements,
            RuneEffectBalanceService effectBalance,
            RuneCombatService combat) {
        if (players == null || enhancements == null || effectBalance == null || combat == null) {
            throw new IllegalArgumentException("transient rune effect dependencies are required");
        }
        this.players = players;
        this.enhancements = enhancements;
        this.effectBalance = effectBalance;
        this.combat = combat;
    }

    /** Pure repository/balance resolution used by scheduling and regression tests. */
    public List<DelayedEffectDescriptor> resolveDelayedEffects(UUID uuid) {
        if (uuid == null) return Collections.emptyList();
        List<DelayedEffectDescriptor> result = new ArrayList<>();
        for (RuneCollectionId collectionId : availableCollections(uuid)) {
            if (!isCollectionUnlocked(uuid, collectionId)) continue;
            RuneInventory inventory = inventory(uuid, collectionId);
            int[] levels = enhancements.getLevels(uuid, collectionId.legacyIndex());
            for (int rune = 0; rune < RuneInventory.BOOST_TYPE_COUNT; rune++) {
                RuneInventory.Entry owned = inventory.getBoost(rune);
                if (owned == null) continue;
                int enhancementLevel = enhancementLevel(levels, rune);
                for (RuneEffectBalanceService.ResolvedEffect effect : effectBalance.effects(
                        collectionId, rune, owned.layer, enhancementLevel)) {
                    if (effect.type != RuneEffectBalanceService.EffectType.DELAYED_DAMAGE_BONUS) continue;
                    if (effect.trigger != null && !"HIT_MOB".equalsIgnoreCase(effect.trigger)) continue;
                    RuneDamageChannel channel = channel(effect.targetSelector);
                    if (channel == null || channel == RuneDamageChannel.CLICK) continue;
                    if (!supportedCondition(effect.condition)) continue;
                    long delayTicks = Math.max(1L, Math.round(effect.delaySeconds * 20.0D));
                    double multiplier = Math.max(0.0D, effect.primaryValue);
                    if (multiplier <= 0.0D) continue;
                    result.add(new DelayedEffectDescriptor(
                            collectionId, rune, channel, multiplier, delayTicks,
                            effect.trigger, effect.condition));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Schedules delayed effects only from confirmed direct CLICK damage, never recursively from statuses. */
    public synchronized void confirmedDamage(
            EntityPlayerMP player,
            EntityLivingBase target,
            GameplayDamageSource source,
            BigDecimal actualDamage) {
        if (player == null || target == null || source != GameplayDamageSource.CLICK) return;
        if (actualDamage == null || actualDamage.signum() <= 0) return;
        if (player.world == null || target.world == null || player.world != target.world || player.world.isRemote) return;

        long now = player.world.getTotalWorldTime();
        for (DelayedEffectDescriptor effect : resolveDelayedEffects(player.getUniqueID())) {
            pending.add(new PendingDamage(
                    player,
                    target,
                    effect.channel,
                    effect.multiplier,
                    now + effect.delayTicks,
                    effect.condition));
        }
    }

    /** Executes due effects through the same status damage boundary used by periodic status ticks. */
    public synchronized void tick() {
        Iterator<PendingDamage> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingDamage effect = iterator.next();
            EntityPlayerMP player = effect.player;
            EntityLivingBase target = effect.target;
            if (player == null || player.world == null) {
                iterator.remove();
                continue;
            }
            long now = player.world.getTotalWorldTime();
            if (now < effect.dueTick) continue;
            iterator.remove();

            if (target == null || target.world == null || target.world != player.world || target.world.isRemote) continue;
            if ("TARGET_ALIVE".equalsIgnoreCase(effect.condition) && target.isDead) continue;
            if (target.isDead) continue;

            RuneDamageMath.Snapshot snapshot = combat.resolveDamage(player.getUniqueID(), now);
            double baseStatusDamage = RuneDamageMath.statusDamage(effect.channel, snapshot);
            double amount = RuneDamageMath.sanitize(baseStatusDamage * effect.multiplier);
            if (amount <= 0.0D) continue;
            RuneStatusDamageRuntime.apply(player, target, effect.channel, amount);
        }
    }

    public synchronized int pendingCount() {
        return pending.size();
    }

    public synchronized void clear() {
        pending.clear();
    }

    private RuneInventory inventory(UUID uuid, RuneCollectionId collectionId) {
        RuneInventory inventory = new RuneInventory();
        List<RuneRecord> records = players.getRunesInCollection(uuid, collectionId.legacyIndex());
        if (records == null) return inventory;
        for (RuneRecord rune : records) {
            if (rune != null) inventory.set(rune.slot, rune.runeIndex, rune.layer);
        }
        return inventory;
    }

    private RuneCollectionId[] availableCollections(UUID uuid) {
        if (players instanceof RuneCollectionStateRepository) return RuneCollectionId.values();
        int legacyCount = Math.min(players.getCollectionNames(uuid).size(), RuneCollectionId.values().length);
        RuneCollectionId[] ids = new RuneCollectionId[legacyCount];
        for (int i = 0; i < legacyCount; i++) ids[i] = RuneCollectionId.fromLegacyIndex(i);
        return ids;
    }

    private boolean isCollectionUnlocked(UUID uuid, RuneCollectionId id) {
        if (id == null) return false;
        if (!(players instanceof RuneCollectionStateRepository)) return true;
        Set<RuneCollectionId> unlocked = ((RuneCollectionStateRepository) players).getUnlockedCollections(uuid);
        return unlocked != null && unlocked.contains(id);
    }

    private int enhancementLevel(int[] levels, int rune) {
        return levels != null && rune >= 0 && rune < levels.length ? Math.max(0, levels[rune]) : 0;
    }

    private RuneDamageChannel channel(String targetSelector) {
        if (targetSelector == null) return null;
        try {
            return RuneDamageChannel.valueOf(targetSelector.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean supportedCondition(String condition) {
        return condition == null || condition.trim().isEmpty() || "TARGET_ALIVE".equalsIgnoreCase(condition);
    }
}
