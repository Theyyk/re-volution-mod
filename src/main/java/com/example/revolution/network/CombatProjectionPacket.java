package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Independent E-menu projection; does not alter legacy PlayerStatsPacket wire protocol. */
public final class CombatProjectionPacket implements IMessage {
    private double click;
    private double poison;
    private String clickDetails = "";
    private String poisonDetails = "";

    public CombatProjectionPacket() {}
    public CombatProjectionPacket(double click, double poison, String clickDetails, String poisonDetails) {
        this.click = click;
        this.poison = poison;
        this.clickDetails = clickDetails == null ? "" : clickDetails;
        this.poisonDetails = poisonDetails == null ? "" : poisonDetails;
    }
    @Override public void fromBytes(ByteBuf buf) {
        click = buf.readDouble();
        poison = buf.readDouble();
        clickDetails = ByteBufUtils.readUTF8String(buf);
        poisonDetails = ByteBufUtils.readUTF8String(buf);
    }
    @Override public void toBytes(ByteBuf buf) {
        buf.writeDouble(click);
        buf.writeDouble(poison);
        ByteBufUtils.writeUTF8String(buf, clickDetails);
        ByteBufUtils.writeUTF8String(buf, poisonDetails);
    }
    public double getClick() { return click; }
    public double getPoison() { return poison; }
    public String getClickDetails() { return clickDetails; }
    public String getPoisonDetails() { return poisonDetails; }
}
