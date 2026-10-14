package com.example.customguimod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RuneEnhancementPacket implements IMessage {
    public static final int[] BULK_AMOUNTS = {1, 5, 10, 100};

    private int statusId = -1;
    private String resource = "";
    private double powerStep;
    private int[] levels = new int[RuneInventory.BOOST_TYPE_COUNT];
    private int[][] costs = new int[RuneInventory.BOOST_TYPE_COUNT][BULK_AMOUNTS.length];

    public RuneEnhancementPacket() {}

    /**
     * Compatibility constructor used by the current v1.2.9 server sync path.
     * Poison is the only built-in rune status in this version, so enrich the old
     * levels-only payload on the server with authoritative enhancement metadata.
     * Future status content should use the explicit status constructor below.
     */
    public RuneEnhancementPacket(int[] levels) {
        this(
                RuneStatusRegistry.STATUS_POISON,
                RuneEconomyConfig.enhancementResourceForStatus(RuneStatusRegistry.STATUS_POISON),
                RuneEconomyConfig.enhancementPowerStep(RuneStatusRegistry.STATUS_POISON),
                levels,
                buildCosts(RuneStatusRegistry.STATUS_POISON, levels)
        );
    }

    public RuneEnhancementPacket(int statusId, String resource, double powerStep, int[] levels, int[][] costs) {
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
                    this.costs[rune][amount] = Math.max(0, costs[rune][amount]);
                }
            }
        }
    }

    private static int[][] buildCosts(int statusId, int[] levels) {
        int[][] result = new int[RuneInventory.BOOST_TYPE_COUNT][BULK_AMOUNTS.length];
        for (int rune = 0; rune < result.length; rune++) {
            int currentLevel = levels != null && rune < levels.length ? Math.max(0, levels[rune]) : 0;
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) {
                result[rune][amount] = RuneEconomyConfig.enhancementCost(
                        statusId,
                        currentLevel,
                        BULK_AMOUNTS[amount]
                );
            }
        }
        return result;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        statusId = buf.readInt();
        resource = ByteBufUtils.readUTF8String(buf);
        powerStep = Math.max(0.0D, buf.readDouble());
        int encodedCount = Math.max(0, buf.readInt());
        levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        costs = new int[RuneInventory.BOOST_TYPE_COUNT][BULK_AMOUNTS.length];
        for (int rune = 0; rune < encodedCount; rune++) {
            int level = Math.max(0, buf.readInt());
            int[] runeCosts = new int[BULK_AMOUNTS.length];
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) {
                runeCosts[amount] = Math.max(0, buf.readInt());
            }
            if (rune >= RuneInventory.BOOST_TYPE_COUNT) continue;
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
                buf.writeInt(Math.max(0, costs[rune][amount]));
            }
        }
    }

    public int getStatusId() { return statusId; }
    public String getResource() { return resource; }
    public double getPowerStep() { return powerStep; }
    public int[] getLevels() { return levels.clone(); }

    public int[][] getCosts() {
        int[][] copy = new int[costs.length][BULK_AMOUNTS.length];
        for (int rune = 0; rune < costs.length; rune++) {
            System.arraycopy(costs[rune], 0, copy[rune], 0, BULK_AMOUNTS.length);
        }
        return copy;
    }
}
