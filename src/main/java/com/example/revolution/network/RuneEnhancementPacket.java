package com.example.revolution.network;

import com.example.revolution.rune.definition.RuneLayoutDefinition;
import io.netty.buffer.ByteBuf;
import java.math.BigDecimal;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RuneEnhancementPacket implements IMessage {
    public static final int[] BULK_AMOUNTS = {1, 5, 10, 100};

    private int statusId = -1;
    private String resource = "";
    private double powerStep;
    private int[] levels = new int[RuneLayoutDefinition.BOOST_TYPE_COUNT];
    private BigDecimal[][] costs = emptyCosts();

    public RuneEnhancementPacket() {}

    public RuneEnhancementPacket(int statusId, String resource, double powerStep, int[] levels, BigDecimal[][] costs) {
        this.statusId = statusId;
        this.resource = resource == null ? "" : resource;
        this.powerStep = Math.max(0.0D, powerStep);
        if (levels != null) {
            int limit = Math.min(this.levels.length, levels.length);
            for (int i = 0; i < limit; i++) this.levels[i] = Math.max(0, levels[i]);
        }
        if (costs != null) {
            int runeLimit = Math.min(this.costs.length, costs.length);
            for (int rune = 0; rune < runeLimit; rune++) {
                if (costs[rune] == null) continue;
                int amountLimit = Math.min(this.costs[rune].length, costs[rune].length);
                for (int amount = 0; amount < amountLimit; amount++) {
                    BigDecimal value = costs[rune][amount];
                    this.costs[rune][amount] = value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
                }
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        statusId = buf.readInt();
        resource = ByteBufUtils.readUTF8String(buf);
        powerStep = Math.max(0.0D, buf.readDouble());
        int encodedCount = Math.max(0, buf.readInt());
        levels = new int[RuneLayoutDefinition.BOOST_TYPE_COUNT];
        costs = emptyCosts();
        for (int rune = 0; rune < encodedCount; rune++) {
            int level = Math.max(0, buf.readInt());
            BigDecimal[] runeCosts = new BigDecimal[BULK_AMOUNTS.length];
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) {
                runeCosts[amount] = parseCost(ByteBufUtils.readUTF8String(buf));
            }
            if (rune >= RuneLayoutDefinition.BOOST_TYPE_COUNT) continue;
            levels[rune] = level;
            System.arraycopy(runeCosts, 0, costs[rune], 0, BULK_AMOUNTS.length);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(statusId);
        ByteBufUtils.writeUTF8String(buf, resource == null ? "" : resource);
        buf.writeDouble(Math.max(0.0D, powerStep));
        buf.writeInt(levels.length);
        for (int rune = 0; rune < levels.length; rune++) {
            buf.writeInt(Math.max(0, levels[rune]));
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) {
                BigDecimal value = costs[rune][amount];
                ByteBufUtils.writeUTF8String(buf, value == null || value.signum() < 0 ? "0" : value.toPlainString());
            }
        }
    }

    public int getStatusId() { return statusId; }
    public String getResource() { return resource; }
    public double getPowerStep() { return powerStep; }
    public int[] getLevels() { return levels.clone(); }

    public BigDecimal[][] getCosts() {
        BigDecimal[][] copy = emptyCosts();
        for (int rune = 0; rune < costs.length; rune++) System.arraycopy(costs[rune], 0, copy[rune], 0, BULK_AMOUNTS.length);
        return copy;
    }

    private static BigDecimal[][] emptyCosts() {
        BigDecimal[][] result = new BigDecimal[RuneLayoutDefinition.BOOST_TYPE_COUNT][BULK_AMOUNTS.length];
        for (int rune = 0; rune < result.length; rune++) {
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) result[rune][amount] = BigDecimal.ZERO;
        }
        return result;
    }

    private static BigDecimal parseCost(String raw) {
        try {
            BigDecimal value = new BigDecimal(raw == null || raw.trim().isEmpty() ? "0" : raw.trim());
            return value.signum() < 0 ? BigDecimal.ZERO : value;
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }
}
