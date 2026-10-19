package com.example.revolution.network.admin;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public final class AdminPanelRequestPacket implements IMessage {
    private String action;

    public AdminPanelRequestPacket() {}
    public AdminPanelRequestPacket(String action) { this.action = action; }

    @Override public void fromBytes(ByteBuf buf) { action = ByteBufUtils.readUTF8String(buf); }
    @Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeUTF8String(buf, action == null ? "" : action); }

    public String getAction() { return action; }
}
