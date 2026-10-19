package com.example.revolution.mob.repository;

import com.example.revolution.mob.definition.MobDefinition;
import java.math.BigDecimal;
import java.util.List;

/** Persistence port. Implementations preserve the existing document contract. */
public interface MobRepository {
    void createMob(String name, String type, BigDecimal hp, int money);
    void setMobSpawn(String name, double x, double y, double z);
    void unspawnMob(String name);
    void deleteMob(String name);
    List<MobDefinition> getAllMobs();
    MobDefinition getMob(String name);
    void setMobReward(String mobName, String resource, BigDecimal amount);
    void setMobReward(String mobName, String resource, int amount);
    String getMobResource(String mobName);
    BigDecimal getMobAmountValue(String mobName);
    int getMobAmount(String mobName);
}
