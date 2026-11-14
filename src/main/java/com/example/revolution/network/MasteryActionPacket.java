package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** UI intents only. No gameplay quantities or permission flags are trusted from clients. */
public final class MasteryActionPacket implements IMessage {
    private String action = "";
    private String branch = "";
    private String expectedRunId = "";
    private long requestId;

    public MasteryActionPacket() {}
    public MasteryActionPacket(String action, String branch, String expectedRunId, long requestId) {
        this.action = safe(action);
        this.branch = safe(branch);
        this.expectedRunId = safe(expectedRunId);
        this.requestId = requestId;
    }
    public String getAction() { return action; }
    public String getBranch() { return branch; }
    public String getExpectedRunId() { return expectedRunId; }
    public long getRequestId() { return requestId; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        branch = ByteBufUtils.readUTF8String(buf);
        expectedRunId = ByteBufUtils.readUTF8String(buf);
        requestId = buf.readLong();
    }
    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, action);
        ByteBufUtils.writeUTF8String(buf, branch);
        ByteBufUtils.writeUTF8String(buf, expectedRunId);
        buf.writeLong(requestId);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
