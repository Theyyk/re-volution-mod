package com.example.revolution.unique.event;

import com.example.revolution.gameplay.GameplayDamageSource;
import com.example.revolution.mob.event.ManagedMobDamageEvent;
import com.example.revolution.mob.service.ManagedMobService;
import com.example.revolution.unique.UniqueRuneModifierService;
import java.math.BigDecimal;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** UNIQUE_RUNE damage layer. Registered after normal/prestige effects and before GOD/AURA. */
public final class UniqueRuneDamageHandler {
    private final ManagedMobService mobs;
    private final UniqueRuneModifierService modifiers;

    public UniqueRuneDamageHandler(ManagedMobService mobs, UniqueRuneModifierService modifiers) {
        if (mobs == null || modifiers == null) throw new IllegalArgumentException("Unique Rune damage dependencies are required");
        this.mobs = mobs; this.modifiers = modifiers;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onDamage(ManagedMobDamageEvent event) {
        if (event == null || event.playerUuid == null || event.source == null) return;
        BigDecimal current = event.damage;
        GameplayDamageSource source = event.source;
        BigDecimal modified = modifiers.applyDamage(event.playerUuid, source, current);
        if (modified != null && modified.signum() >= 0) event.damage = modified;
    }
}
