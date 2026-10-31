package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only. Progress, rewards, stage state and shared payload are always server-authored. */
public final class AchievementActionPacket implements IMessage {
    private String action = "";
    private String achievementId = "";
    private int expectedStageIndex;
    private long requestNonce;

    public AchievementActionPacket() {}
    public AchievementActionPacket(String action, String achievementId, int expectedStageIndex, long requestNonce) {
        this.action = safe(action);
        this.achievementId = safe(achievementId);
        this.expectedStageIndex = Math.max(0, expectedStageIndex);
        this.requestNonce = requestNonce;
    }

    public String getAction() { return action; }
    public String getAchievementId() { return achievementId; }
    public int getExpectedStageIndex() { return expectedStageIndex; }
    public long getRequestNonce() { return requestNonce; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        achievementId = ByteBufUtils.readUTF8String(buf);
        expectedStageIndex = Math.max(0, buf.readInt());
        requestNonce = buf.readLong();
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(action));
        ByteBufUtils.writeUTF8String(buf, safe(achievementId));
        buf.writeInt(expectedStageIndex);
        buf.writeLong(requestNonce);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
