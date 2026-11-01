package com.example.revolution.network;

import com.example.revolution.effect.EffectResolver;
import com.example.revolution.god.GodWorshipModel.ActivationMode;
import com.example.revolution.god.GodWorshipModel.FeatureGate;
import com.example.revolution.god.GodWorshipModel.GodId;
import com.example.revolution.god.GodWorshipModel.PersistentState;
import com.example.revolution.god.GodWorshipModel.Preference;
import com.example.revolution.god.GodWorshipModel.RunState;
import com.example.revolution.god.GodWorshipModel.State;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.lifecycle.ServerServices;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server adapter for God Worship UI intents. */
public final class GodWorshipActionPacketHandler implements IMessageHandler<GodWorshipActionPacket, IMessage> {
    @Override public IMessage onMessage(GodWorshipActionPacket message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || message == null) return null;
        player.getServerWorld().addScheduledTask(() -> handle(player, message));
        return null;
    }

    private static void handle(EntityPlayerMP player, GodWorshipActionPacket request) {
        ServerServices services = ServerLifecycle.services();
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_WORSHIP)) return;
        String action = request.getAction();
        if ("sync".equals(action)) {
            long now = System.currentTimeMillis();
            services.godAutomation.setMenuOpen(player.getUniqueID(), true, now);
            sendSnapshot(player, services, now);
            return;
        }
        if ("menu_closed".equals(action)) {
            services.godAutomation.setMenuOpen(player.getUniqueID(), false, System.currentTimeMillis());
            return;
        }
        if (!GodWorshipRequestLimiter.acceptMutation(player.getUniqueID(), request.getRequestNonce())) return;

        GodId god = parseGod(request.getGodId());
        if ("duration_upgrade".equals(action)) {
            if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_PERMANENT_DURATION)) return;
            services.godAutomation.purchasePermanentDuration(player.getUniqueID());
        } else if (god == null) {
            return;
        } else if ("unlock".equals(action)) {
            services.gods.unlock(player.getUniqueID(), god);
        } else if ("upgrade".equals(action)) {
            services.gods.upgrade(player.getUniqueID(), god);
        } else if ("click".equals(action)) {
            long now = System.currentTimeMillis();
            if (!GodWorshipRequestLimiter.acceptClick(player.getUniqueID(), now, services.godAutomation.clickRateLimitPerSecond(god))) return;
            boolean duration = services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_PERMANENT_DURATION);
            services.gods.click(player.getUniqueID(), god, now, duration);
        } else if ("auto_worship".equals(action)) {
            if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_AUTO_WORSHIP)) return;
            services.godAutomation.setAutoWorship(player.getUniqueID(), god, Boolean.parseBoolean(request.getValue()));
        } else if ("activation_mode".equals(action)) {
            if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_AUTO_WORSHIP)) return;
            ActivationMode mode = parseMode(request.getValue());
            if (mode == null) return;
            GodId linked = parseGod(request.getLinkedGodId());
            services.godAutomation.setActivationMode(player.getUniqueID(), god, mode, linked);
        } else return;
        sendSnapshot(player, services, System.currentTimeMillis());
    }

    public static void sendSnapshot(EntityPlayerMP player, ServerServices services) { sendSnapshot(player, services, System.currentTimeMillis()); }

    private static void sendSnapshot(EntityPlayerMP player, ServerServices services, long now) {
        if (!services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_WORSHIP)) return;
        boolean autoUnlocked = services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_AUTO_WORSHIP);
        boolean durationUnlocked = services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_PERMANENT_DURATION);
        services.godAutomation.tick(player.getUniqueID(), now, autoUnlocked, durationUnlocked);
        RunState run = services.gods.runState(player.getUniqueID());
        PersistentState persistent = services.gods.persistentState(player.getUniqueID());
        List<GodWorshipSnapshotPacket.Entry> entries = new ArrayList<>();
        for (GodId god : GodId.values()) {
            State state = run.gods.get(god); Preference preference = persistent.preferences.get(god);
            BigDecimal unlockCost = services.godPolicy.unlockCost(god);
            BigDecimal upgradeCost = state != null && state.unlocked ? services.godPolicy.upgradeCost(god, state.level) : BigDecimal.ZERO;
            BigDecimal currentMultiplier = BigDecimal.ONE.add(state != null && state.unlocked ? services.godPolicy.buffAdditiveMultiplier(god, state.level) : BigDecimal.ZERO);
            EffectResolver.Stat primary = services.godPolicy.primaryChannel(god); EffectResolver.Stat secondary = services.godPolicy.secondaryChannel(god);
            entries.add(new GodWorshipSnapshotPacket.Entry(god.name(), state != null && state.unlocked, state == null ? 0L : state.level,
                    state == null || state.phase == null ? "LOCKED" : state.phase.name(), state == null ? 0L : state.buffActiveUntilMillis,
                    state == null ? 0L : state.cooldownUntilMillis, state == null ? 0L : state.chargeProgress, services.godPolicy.chargeRequired(god),
                    exact(unlockCost), exact(upgradeCost), services.godPolicy.unlockResource(god), primary == null ? "" : primary.name(),
                    secondary == null ? "" : secondary.name(), exact(currentMultiplier), autoUnlocked && preference != null && preference.autoWorshipEnabled,
                    autoUnlocked && preference != null && preference.activationMode != null ? preference.activationMode.name() : "MANUAL",
                    autoUnlocked && preference != null && preference.linkedGodId != null ? preference.linkedGodId.name() : ""));
        }
        long durationBonusMillis = durationUnlocked ? services.godPolicy.permanentDurationMillis(persistent.permanentDurationLevel) : 0L;
        long durationBonusSeconds = durationBonusMillis == Long.MAX_VALUE ? Long.MAX_VALUE : durationBonusMillis / 1000L;
        NetworkHandler.sendToClient(new GodWorshipSnapshotPacket(entries, exact(persistent.offerings), persistent.permanentDurationLevel,
                durationBonusSeconds, exact(services.godPolicy.durationUpgradeCost(persistent.permanentDurationLevel)),
                services.godPolicy.featureEnabled(FeatureGate.GOD_AUTO_ACTIVATION) && autoUnlocked,
                services.godPolicy.featureEnabled(FeatureGate.GOD_PERMANENT_BUFF_DURATION) && durationUnlocked,
                services.godPolicy.featureEnabled(FeatureGate.GOD_SECONDARY_BUFFS)
                        && services.mechanicUnlocks.isUnlocked(player.getUniqueID(), BalanceMechanicUnlockPolicy.GOD_SECONDARY_BUFFS)), player);
    }

    private static GodId parseGod(String value) { if (value == null || value.isEmpty()) return null; try { return GodId.valueOf(value); } catch (IllegalArgumentException ignored) { return null; } }
    private static ActivationMode parseMode(String value) { if (value == null || value.isEmpty()) return null; try { return ActivationMode.valueOf(value); } catch (IllegalArgumentException ignored) { return null; } }
    private static String exact(BigDecimal value) { return value == null || value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString(); }
}
