package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only. Unique Rune state and share payloads are always rebuilt server-side. */
public final class UniqueRuneActionPacket implements IMessage {
    private String action = "";
    private String collectionId = "";
    private String runeId = "";
    private boolean confirmed;
    private long epochDay;
    private long requestNonce;

    public UniqueRuneActionPacket() {}
    public UniqueRuneActionPacket(String action, String collectionId, String runeId, boolean confirmed, long epochDay, long requestNonce) {
        this.action = safe(action); this.collectionId = safe(collectionId); this.runeId = safe(runeId);
        this.confirmed = confirmed; this.epochDay = epochDay; this.requestNonce = requestNonce;
    }
    public String getAction() { return action; }
    public String getCollectionId() { return collectionId; }
    public String getRuneId() { return runeId; }
    public boolean isConfirmed() { return confirmed; }
    public long getEpochDay() { return epochDay; }
    public long getRequestNonce() { return requestNonce; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf); collectionId = ByteBufUtils.readUTF8String(buf); runeId = ByteBufUtils.readUTF8String(buf);
        confirmed = buf.readBoolean(); epochDay = buf.readLong(); requestNonce = buf.readLong();
    }
    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(action)); ByteBufUtils.writeUTF8String(buf, safe(collectionId)); ByteBufUtils.writeUTF8String(buf, safe(runeId));
        buf.writeBoolean(confirmed); buf.writeLong(epochDay); buf.writeLong(requestNonce);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
