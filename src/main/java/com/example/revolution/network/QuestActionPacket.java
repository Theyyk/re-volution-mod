package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client Quest intent only. Progress, rewards and lifecycle remain server authoritative. */
public final class QuestActionPacket implements IMessage {
    private String action = "";
    private String entryId = "";

    public QuestActionPacket() {}
    public QuestActionPacket(String action) { this(action, ""); }
    public QuestActionPacket(String action, String entryId) {
        this.action = action == null ? "" : action;
        this.entryId = entryId == null ? "" : entryId;
    }

    public String getAction() { return action; }
    public String getEntryId() { return entryId; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        entryId = ByteBufUtils.readUTF8String(buf);
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, entryId);
    }
}
