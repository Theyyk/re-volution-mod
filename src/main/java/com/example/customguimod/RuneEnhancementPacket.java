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

    public RuneEnhancementPacket(int[] levels) {
        this(-1, "", 0.0D, levels, null);
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

    @Override
    public void fromBytes(ByteBuf buf) {
        statusId = buf.readInt();
        resource = ByteBufUtils.readUTF8String(buf);
        powerStep = Math.max(0.0D, buf.readDouble());
        int count = Math.max(0, Math.min(RuneInventory.BOOST_TYPE_COUNT, buf.readInt()));
        levels = new int[RuneInventory.BOOST_TYPE_COUNT];
        costs = new int[RuneInventory.BOOST_TYPE_COUNT][BULK_AMOUNTS.length];
        for (int rune = 0; rune < count; rune++) {
            levels[rune] = Math.max(0, buf.readInt());
            for (int amount = 0; amount < BULK_AMOUNTS.length; amount++) {
                costs[rune][amount] = Math.max(0, buf.readInt());
            }
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
