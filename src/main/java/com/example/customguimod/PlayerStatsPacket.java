package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PlayerStatsPacket implements IMessage {

    private int coins;
    private int crystals;
    private int runeDust;
    private int runeShards;
    private int runeCores;
    private int totalDamage;

    public PlayerStatsPacket() {}

    public PlayerStatsPacket(int coins, int crystals, int runeDust,
                             int runeShards, int runeCores, int totalDamage) {
        this.coins = coins;
        this.crystals = crystals;
        this.runeDust = runeDust;
        this.runeShards = runeShards;
        this.runeCores = runeCores;
        this.totalDamage = totalDamage;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        coins = buf.readInt();
        crystals = buf.readInt();
        runeDust = buf.readInt();
        runeShards = buf.readInt();
        runeCores = buf.readInt();
        totalDamage = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(coins);
        buf.writeInt(crystals);
        buf.writeInt(runeDust);
        buf.writeInt(runeShards);
        buf.writeInt(runeCores);
        buf.writeInt(totalDamage);
    }

    public int getCoins() {
        return coins;
    }

    public int getCrystals() {
        return crystals;
    }

    public int getRuneDust() {
        return runeDust;
    }

    public int getRuneShards() {
        return runeShards;
    }

    public int getRuneCores() {
        return runeCores;
    }

    public int getTotalDamage() {
        return totalDamage;
    }
}
