package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class PlayerStatsPacket implements IMessage {

    private long coins;
    private long crystals;
    private long runeDust;
    private long runeShards;
    private long runeCores;
    private int totalDamage;
    private int activeRuneStatusId;
    private String purchaseResource;
    private int purchaseUnitCost;
    private String awakeningResource;
    private int awakeningNormalCost;
    private int awakeningImprovedCost;

    public PlayerStatsPacket() {}

    public PlayerStatsPacket(long coins, long crystals, long runeDust,
                             long runeShards, long runeCores, int totalDamage,
                             int activeRuneStatusId,
                             String purchaseResource, int purchaseUnitCost,
                             String awakeningResource, int awakeningNormalCost,
                             int awakeningImprovedCost) {
        this.coins = coins;
        this.crystals = crystals;
        this.runeDust = runeDust;
        this.runeShards = runeShards;
        this.runeCores = runeCores;
        this.totalDamage = totalDamage;
        this.activeRuneStatusId = activeRuneStatusId;
        this.purchaseResource = purchaseResource == null ? "coins" : purchaseResource;
        this.purchaseUnitCost = purchaseUnitCost;
        this.awakeningResource = awakeningResource == null ? "" : awakeningResource;
        this.awakeningNormalCost = awakeningNormalCost;
        this.awakeningImprovedCost = awakeningImprovedCost;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        coins = buf.readLong();
        crystals = buf.readLong();
        runeDust = buf.readLong();
        runeShards = buf.readLong();
        runeCores = buf.readLong();
        totalDamage = buf.readInt();
        activeRuneStatusId = buf.readInt();
        purchaseResource = ByteBufUtils.readUTF8String(buf);
        purchaseUnitCost = buf.readInt();
        awakeningResource = ByteBufUtils.readUTF8String(buf);
        awakeningNormalCost = buf.readInt();
        awakeningImprovedCost = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(coins);
        buf.writeLong(crystals);
        buf.writeLong(runeDust);
        buf.writeLong(runeShards);
        buf.writeLong(runeCores);
        buf.writeInt(totalDamage);
        buf.writeInt(activeRuneStatusId);
        ByteBufUtils.writeUTF8String(buf, purchaseResource == null ? "coins" : purchaseResource);
        buf.writeInt(purchaseUnitCost);
        ByteBufUtils.writeUTF8String(buf, awakeningResource == null ? "" : awakeningResource);
        buf.writeInt(awakeningNormalCost);
        buf.writeInt(awakeningImprovedCost);
    }

    public long getCoins() { return coins; }
    public long getCrystals() { return crystals; }
    public long getRuneDust() { return runeDust; }
    public long getRuneShards() { return runeShards; }
    public long getRuneCores() { return runeCores; }
    public int getTotalDamage() { return totalDamage; }
    public int getActiveRuneStatusId() { return activeRuneStatusId; }
    public String getPurchaseResource() { return purchaseResource; }
    public int getPurchaseUnitCost() { return purchaseUnitCost; }
    public String getAwakeningResource() { return awakeningResource; }
    public int getAwakeningNormalCost() { return awakeningNormalCost; }
    public int getAwakeningImprovedCost() { return awakeningImprovedCost; }
}
