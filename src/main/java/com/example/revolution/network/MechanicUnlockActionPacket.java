package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** v2.12.0 client intent. There is deliberately no player LOCK operation. */
public final class MechanicUnlockActionPacket implements IMessage {
    private String action = "";
    private String unlockId = "";
    private long requestNonce;

    public MechanicUnlockActionPacket() {}
    public MechanicUnlockActionPacket(String action, String unlockId, long requestNonce) {
        this.action = action == null ? "" : action;
        this.unlockId = unlockId == null ? "" : unlockId;
        this.requestNonce = requestNonce;
    }
    public String getAction() { return action; }
    public String getUnlockId() { return unlockId; }
    public long getRequestNonce() { return requestNonce; }
    @Override public void fromBytes(ByteBuf buf) { action = ByteBufUtils.readUTF8String(buf); unlockId = ByteBufUtils.readUTF8String(buf); requestNonce = buf.readLong(); }
    @Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeUTF8String(buf, action); ByteBufUtils.writeUTF8String(buf, unlockId); buf.writeLong(requestNonce); }
}
