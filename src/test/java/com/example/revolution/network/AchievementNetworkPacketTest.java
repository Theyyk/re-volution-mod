package com.example.revolution.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;

/** Wire round-trip regression for v2.10.0 Achievement C2S intent and S2C factual snapshot. */
public final class AchievementNetworkPacketTest {
    public static void main(String[] args) {
        AchievementActionPacket action = roundTripAction(new AchievementActionPacket("claim", "damage_total", 3));
        check("claim".equals(action.getAction()), "action survives wire");
        check("damage_total".equals(action.getAchievementId()), "achievement id survives wire");
        check(action.getExpectedStageIndex() == 3, "expected stage survives wire");

        AchievementSnapshotPacket.Entry entry = new AchievementSnapshotPacket.Entry();
        entry.achievementId = "damage_total";
        entry.name = "Total Damage";
        entry.category = "DAMAGE";
        entry.stageIndex = 3;
        entry.stageRoman = "III";
        entry.progress = "1500";
        entry.target = "1000";
        entry.crystals = "20";
        entry.offerings = "2";
        entry.auraKeys = "1";
        entry.achievementPoints = 25L;
        entry.highestCompletedStage = 3;
        entry.highestClaimedStage = 2;
        entry.completed = true;
        entry.claimed = false;
        entry.finalStage = false;
        entry.hidden = false;
        entry.revealed = true;

        AchievementSnapshotPacket decoded = roundTripSnapshot(new AchievementSnapshotPacket(
                Collections.singletonList(entry), 75L, 4, "3", "1000"));
        check(decoded.getEntries().size() == 1, "snapshot entry count survives wire");
        AchievementSnapshotPacket.Entry restored = decoded.getEntries().get(0);
        check("damage_total".equals(restored.achievementId) && restored.stageIndex == 3 && "III".equals(restored.stageRoman),
                "server stage identity survives wire");
        check("1500".equals(restored.progress) && "1000".equals(restored.target) && restored.completed && !restored.claimed,
                "factual stage state survives wire");
        check(decoded.getTotalAchievementPoints() == 75L && decoded.getAchievementLevel() == 4,
                "meta progression survives wire");
        check("3".equals(decoded.getEconomyBonusPercent()) && "1000".equals(decoded.getNextLevelThreshold()),
                "meta bonus/next threshold survive wire");
        System.out.println("Achievement network action/snapshot round-trip regressions passed");
    }

    private static AchievementActionPacket roundTripAction(AchievementActionPacket source) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            source.toBytes(buffer);
            AchievementActionPacket decoded = new AchievementActionPacket();
            decoded.fromBytes(buffer);
            check(!buffer.isReadable(), "action decoder consumes all bytes");
            return decoded;
        } finally { buffer.release(); }
    }

    private static AchievementSnapshotPacket roundTripSnapshot(AchievementSnapshotPacket source) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            source.toBytes(buffer);
            AchievementSnapshotPacket decoded = new AchievementSnapshotPacket();
            decoded.fromBytes(buffer);
            check(!buffer.isReadable(), "snapshot decoder consumes all bytes");
            return decoded;
        } finally { buffer.release(); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
