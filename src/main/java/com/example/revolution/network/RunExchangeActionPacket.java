package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only; prices and rewards are never accepted from a client. */
public final class RunExchangeActionPacket implements IMessage {
    private String action = "";
    private String expectedRunId = "";
    private String offerId = "";
    private long requestId;

    public RunExchangeActionPacket() {}
    public RunExchangeActionPacket(String action, String expectedRunId, String offerId, long requestId) {
        this.action = safe(action);
        this.expectedRunId = safe(expectedRunId);
        this.offerId = safe(offerId);
        this.requestId = requestId;
    }
    public String getAction() { return action; }
    public String getExpectedRunId() { return expectedRunId; }
    public String getOfferId() { return offerId; }
    public long getRequestId() { return requestId; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        expectedRunId = ByteBufUtils.readUTF8String(buf);
        offerId = ByteBufUtils.readUTF8String(buf);
        requestId = buf.readLong();
    }
    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, expectedRunId);
        ByteBufUtils.writeUTF8String(buf, offerId);
        buf.writeLong(requestId);
    }
    private static String safe(String s) { return s == null ? "" : s; }
}
