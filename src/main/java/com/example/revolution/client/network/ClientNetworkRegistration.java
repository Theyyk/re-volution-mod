package com.example.revolution.client.network;

import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.PingPacket;
import com.example.revolution.network.PlayerStatsPacket;
import com.example.revolution.network.PongPacket;
import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.network.RuneSlotPacket;
import com.example.revolution.network.admin.AdminPanelRequestPacket;
import com.example.revolution.network.admin.AdminPanelStatePacket;
import net.minecraftforge.fml.relauncher.Side;

public final class ClientNetworkRegistration {
    private ClientNetworkRegistration() {}
    public static void register() {
        NetworkHandler.INSTANCE.registerMessage(PingPacketClientHandler.class, PingPacket.class, 0, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PongPacketHandler.class, PongPacket.class, 1, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneSlotPacketHandler.class, RuneSlotPacket.class, 2, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneCollectionsPacketHandler.class, RuneCollectionsPacket.class, 3, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(PlayerStatsPacketHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(AdminPanelRequestClientHandler.class, AdminPanelRequestPacket.class, 5, Side.SERVER);
        NetworkHandler.INSTANCE.registerMessage(AdminPanelStateClientHandler.class, AdminPanelStatePacket.class, 6, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RuneEnhancementPacketHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
    }
}
