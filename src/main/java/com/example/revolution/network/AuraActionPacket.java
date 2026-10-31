package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only. Aura rolls, costs, rarity results and generated ids remain server-owned. */
public final class AuraActionPacket implements IMessage {
    private String action = "";
    private String value = "";
    private String ids = "";
    private long requestNonce;

    public AuraActionPacket() {}
    public AuraActionPacket(String action, String value, String ids, long requestNonce) {
        this.action = safe(action); this.value = safe(value); this.ids = safe(ids); this.requestNonce = requestNonce;
    }

    public String getAction() { return action; }
    public String getValue() { return value; }
    public String getIds() { return ids; }
    public long getRequestNonce() { return requestNonce; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        value = ByteBufUtils.readUTF8String(buf);
        ids = ByteBufUtils.readUTF8String(buf);
        requestNonce = buf.readLong();
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, value);
        ByteBufUtils.writeUTF8String(buf, ids);
        buf.writeLong(requestNonce);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
