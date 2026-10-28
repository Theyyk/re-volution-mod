package com.example.revolution.mob.repository;

import com.example.revolution.mob.definition.MobDefinition;
import com.example.revolution.mob.service.MobBalanceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Read-through decorator: persistence remains authoritative fallback while Balance Core may override canonical MobL_N values. */
public final class BalanceAwareMobRepository implements MobRepository {
    private final MobRepository delegate;
    private final MobBalanceService balance;

    public BalanceAwareMobRepository(MobRepository delegate, MobBalanceService balance) {
        if (delegate == null) throw new IllegalArgumentException("delegate is required");
        if (balance == null) throw new IllegalArgumentException("balance is required");
        this.delegate = delegate;
        this.balance = balance;
    }

    @Override public void createMob(String name, String type, BigDecimal hp, int money) {
        delegate.createMob(name, type, hp, money);
    }

    @Override public void setMobSpawn(String name, double x, double y, double z) {
        delegate.setMobSpawn(name, x, y, z);
    }

    @Override public void unspawnMob(String name) {
        delegate.unspawnMob(name);
    }

    @Override public void deleteMob(String name) {
        delegate.deleteMob(name);
    }

    @Override public List<MobDefinition> getAllMobs() {
        List<MobDefinition> result = new ArrayList<>();
        for (MobDefinition mob : delegate.getAllMobs()) result.add(resolve(mob));
        return result;
    }

    @Override public MobDefinition getMob(String name) {
        return resolve(delegate.getMob(name));
    }

    @Override public void setMobReward(String mobName, String resource, BigDecimal amount) {
        delegate.setMobReward(mobName, resource, amount);
    }

    @Override public void setMobReward(String mobName, String resource, int amount) {
        delegate.setMobReward(mobName, resource, amount);
    }

    @Override public String getMobResource(String mobName) {
        return balance.resource(mobName, delegate.getMobResource(mobName));
    }

    @Override public BigDecimal getMobAmountValue(String mobName) {
        return balance.reward(mobName, delegate.getMobAmountValue(mobName));
    }

    @Override public int getMobAmount(String mobName) {
        BigDecimal value = getMobAmountValue(mobName);
        if (value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(0, value.intValue());
    }

    private MobDefinition resolve(MobDefinition mob) {
        if (mob == null) return null;
        return new MobDefinition(mob.name, mob.type, balance.hp(mob.name, mob.hp), mob.money,
                mob.x, mob.y, mob.z, mob.enabled);
    }
}
