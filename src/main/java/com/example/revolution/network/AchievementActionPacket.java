package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** Client intent only. Progress, rewards, stage state and shared payload are always server-authored. */
public final class AchievementActionPacket implements IMessage {
    private String action = "";
    private String achievementId = "";
    private int expectedStageIndex;

    public AchievementActionPacket() {}
    public AchievementActionPacket(String action, String achievementId, int expectedStageIndex) {
        this.action = safe(action);
        this.achievementId = safe(achievementId);
        this.expectedStageIndex = Math.max(0, expectedStageIndex);
    }

    public String getAction() { return action; }
    public String getAchievementId() { return achievementId; }
    public int getExpectedStageIndex() { return expectedStageIndex; }

    @Override public void fromBytes(ByteBuf buf) {
        action = ByteBufUtils.readUTF8String(buf);
        achievementId = ByteBufUtils.readUTF8String(buf);
        expectedStageIndex = Math.max(0, buf.readInt());
    }

    @Override public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(action));
        ByteBufUtils.writeUTF8String(buf, safe(achievementId));
        buf.writeInt(expectedStageIndex);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
