package com.example.customguimod;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("customgui:main");

    public static void registerServer() {
        INSTANCE.registerMessage(PingPacket.Handler.class, PingPacket.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PongPacketServerHandler.class, PongPacket.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(CardPacketServerHandler.class, CardPacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(DeckPacketServerHandler.class, DeckPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PlayerStatsPacketServerHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        INSTANCE.registerMessage(AdminPanelRequestPacket.Handler.class, AdminPanelRequestPacket.class, 5, Side.SERVER);
        INSTANCE.registerMessage(AdminPanelStateServerHandler.class, AdminPanelStatePacket.class, 6, Side.CLIENT);
        INSTANCE.registerMessage(RuneEnhancementPacketHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
    }

    public static void registerClient() {
        INSTANCE.registerMessage(PingPacketClientHandler.class, PingPacket.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(PongPacketHandler.class, PongPacket.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(CardPacketHandler.class, CardPacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(DeckPacketHandler.class, DeckPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PlayerStatsPacketHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        INSTANCE.registerMessage(AdminPanelRequestClientHandler.class, AdminPanelRequestPacket.class, 5, Side.SERVER);
        INSTANCE.registerMessage(AdminPanelStateClientHandler.class, AdminPanelStatePacket.class, 6, Side.CLIENT);
        INSTANCE.registerMessage(RuneEnhancementPacketHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
    }
}
