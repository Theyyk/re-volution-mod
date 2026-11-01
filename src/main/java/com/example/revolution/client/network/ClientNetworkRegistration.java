package com.example.revolution.client.network;

import com.example.revolution.network.AchievementActionPacket;
import com.example.revolution.network.AchievementSnapshotPacket;
import com.example.revolution.network.AuraActionPacket;
import com.example.revolution.network.AuraSnapshotPacket;
import com.example.revolution.network.FastAutoTogglePacket;
import com.example.revolution.network.GodWorshipActionPacket;
import com.example.revolution.network.GodWorshipSnapshotPacket;
import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.network.MobZoneOverlayPacket;
import com.example.revolution.network.MobZoneOverlayRequestPacket;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PetActionPacket;
import com.example.revolution.network.PetSnapshotPacket;
import com.example.revolution.network.PingPacket;
import com.example.revolution.network.PlayerStatsPacket;
import com.example.revolution.network.PongPacket;
import com.example.revolution.network.QuestActionPacket;
import com.example.revolution.network.QuestSnapshotPacket;
import com.example.revolution.network.RuneCollectionMetadataPacket;
import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.network.StageTravelMenuPacket;
import com.example.revolution.network.StageTravelMenuRequestPacket;
import com.example.revolution.network.StageTravelTeleportRequestPacket;
import com.example.revolution.network.UniqueRuneActionPacket;
import com.example.revolution.network.UniqueRuneSnapshotPacket;
import net.minecraftforge.fml.relauncher.Side;

public final class ClientNetworkRegistration {
    private ClientNetworkRegistration() {}
    public static void register() {
        NetworkHandler.INSTANCE.registerMessage(PingPacketClientHandler.class, PingPacket.class, 0, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PongPacketHandler.class, PongPacket.class, 1, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneSlotPacketHandler.class, RuneSlotPacket.class, 2, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneCollectionsPacketHandler.class, RuneCollectionsPacket.class, 3, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PlayerStatsPacketHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneEnhancementPacketHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MobZoneOverlayRequestClientHandler.class, MobZoneOverlayRequestPacket.class, 8, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MobZoneOverlayPacketClientHandler.class, MobZoneOverlayPacket.class, 9, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(StageTravelMenuRequestClientHandler.class, StageTravelMenuRequestPacket.class, 10, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(StageTravelMenuPacketClientHandler.class, StageTravelMenuPacket.class, 11, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(StageTravelTeleportRequestClientHandler.class, StageTravelTeleportRequestPacket.class, 12, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(FastAutoTogglePacketClientHandler.class, FastAutoTogglePacket.class, 13, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneCollectionMetadataPacketHandler.class, RuneCollectionMetadataPacket.class, 14, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PetActionPacketClientHandler.class, PetActionPacket.class, 15, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PetSnapshotPacketHandler.class, PetSnapshotPacket.class, 16, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(QuestActionPacketClientHandler.class, QuestActionPacket.class, 17, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(QuestSnapshotPacketHandler.class, QuestSnapshotPacket.class, 18, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(GodWorshipActionPacketClientHandler.class, GodWorshipActionPacket.class, 19, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(GodWorshipSnapshotPacketHandler.class, GodWorshipSnapshotPacket.class, 20, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(AuraActionPacketClientHandler.class, AuraActionPacket.class, 21, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(AuraSnapshotPacketHandler.class, AuraSnapshotPacket.class, 22, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(AchievementActionPacketClientHandler.class, AchievementActionPacket.class, 23, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(AchievementSnapshotPacketHandler.class, AchievementSnapshotPacket.class, 24, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(UniqueRuneActionPacketClientHandler.class, UniqueRuneActionPacket.class, 25, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(UniqueRuneSnapshotPacketHandler.class, UniqueRuneSnapshotPacket.class, 26, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MechanicUnlockActionPacketClientHandler.class, MechanicUnlockActionPacket.class, 27, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MechanicUnlockSnapshotPacketHandler.class, MechanicUnlockSnapshotPacket.class, 28, Side.CLIENT);
    }
}
