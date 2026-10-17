package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.math.BigDecimal;

public class PlayerStatsPacket implements IMessage {
    private String coins = "0";
    private String crystals = "0";
    private String runeDust = "0";
    private String runeShards = "0";
    private String runeCores = "0";
    private int totalDamage;
    private int activeRuneStatusId;
    private String purchaseResource;
    private int purchaseUnitCost;
    private String awakeningResource;
    private int awakeningNormalCost;
    private int awakeningImprovedCost;

    public PlayerStatsPacket() {}

    public PlayerStatsPacket(BigDecimal coins, BigDecimal crystals, BigDecimal runeDust,
                             BigDecimal runeShards, BigDecimal runeCores, int totalDamage,
                             int activeRuneStatusId, String purchaseResource, int purchaseUnitCost,
                             String awakeningResource, int awakeningNormalCost, int awakeningImprovedCost) {
        this.coins = LargeNumberValue.canonical(coins);
        this.crystals = LargeNumberValue.canonical(crystals);
        this.runeDust = LargeNumberValue.canonical(runeDust);
        this.runeShards = LargeNumberValue.canonical(runeShards);
        this.runeCores = LargeNumberValue.canonical(runeCores);
        this.totalDamage = totalDamage;
        this.activeRuneStatusId = activeRuneStatusId;
        this.purchaseResource = purchaseResource == null ? "coins" : purchaseResource;
        this.purchaseUnitCost = purchaseUnitCost;
        this.awakeningResource = awakeningResource == null ? "" : awakeningResource;
        this.awakeningNormalCost = awakeningNormalCost;
        this.awakeningImprovedCost = awakeningImprovedCost;
    }

    @Override public void fromBytes(ByteBuf buf) {
        coins = ByteBufUtils.readUTF8String(buf); crystals = ByteBufUtils.readUTF8String(buf);
        runeDust = ByteBufUtils.readUTF8String(buf); runeShards = ByteBufUtils.readUTF8String(buf); runeCores = ByteBufUtils.readUTF8String(buf);
        totalDamage = buf.readInt(); activeRuneStatusId = buf.readInt();
        purchaseResource = ByteBufUtils.readUTF8String(buf); purchaseUnitCost = buf.readInt();
        awakeningResource = ByteBufUtils.readUTF8String(buf); awakeningNormalCost = buf.readInt(); awakeningImprovedCost = buf.readInt();
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, coins); ByteBufUtils.writeUTF8String(buf, crystals);
        ByteBufUtils.writeUTF8String(buf, runeDust); ByteBufUtils.writeUTF8String(buf, runeShards); ByteBufUtils.writeUTF8String(buf, runeCores);
        buf.writeInt(totalDamage); buf.writeInt(activeRuneStatusId);
        ByteBufUtils.writeUTF8String(buf, purchaseResource == null ? "coins" : purchaseResource); buf.writeInt(purchaseUnitCost);
        ByteBufUtils.writeUTF8String(buf, awakeningResource == null ? "" : awakeningResource); buf.writeInt(awakeningNormalCost); buf.writeInt(awakeningImprovedCost);
    }

    public BigDecimal getCoins() { return LargeNumberValue.parseExact(coins); }
    public BigDecimal getCrystals() { return LargeNumberValue.parseExact(crystals); }
    public BigDecimal getRuneDust() { return LargeNumberValue.parseExact(runeDust); }
    public BigDecimal getRuneShards() { return LargeNumberValue.parseExact(runeShards); }
    public BigDecimal getRuneCores() { return LargeNumberValue.parseExact(runeCores); }
    public int getTotalDamage() { return totalDamage; }
    public int getActiveRuneStatusId() { return activeRuneStatusId; }
    public String getPurchaseResource() { return purchaseResource; }
    public int getPurchaseUnitCost() { return purchaseUnitCost; }
    public String getAwakeningResource() { return awakeningResource; }
    public int getAwakeningNormalCost() { return awakeningNormalCost; }
    public int getAwakeningImprovedCost() { return awakeningImprovedCost; }
}
