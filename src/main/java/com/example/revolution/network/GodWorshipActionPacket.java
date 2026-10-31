package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only; all God state and economy validation remains server authoritative. */
public final class GodWorshipActionPacket implements IMessage {
    private String action = "";
    private String godId = "";
    private String value = "";
    private String linkedGodId = "";
    private long requestNonce;

    public GodWorshipActionPacket() {}
    public GodWorshipActionPacket(String action, String godId, String value, String linkedGodId, long requestNonce) {
        this.action = safe(action);
        this.godId = safe(godId);
        this.value = safe(value);
        this.linkedGodId = safe(linkedGodId);
        this.requestNonce = requestNonce;
    }

    public String getAction() { return action; }
    public String getGodId() { return godId; }
    public String getValue() { return value; }
    public String getLinkedGodId() { return linkedGodId; }
    public long getRequestNonce() { return requestNonce; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        godId = ByteBufUtils.readUTF8String(buf);
        value = ByteBufUtils.readUTF8String(buf);
        linkedGodId = ByteBufUtils.readUTF8String(buf);
        requestNonce = buf.readLong();
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, godId);
        ByteBufUtils.writeUTF8String(buf, value);
        ByteBufUtils.writeUTF8String(buf, linkedGodId);
        buf.writeLong(requestNonce);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
