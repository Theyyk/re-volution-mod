package com.example.revolution.network;

import com.example.revolution.network.admin.AdminPanelRequestPacket;
import com.example.revolution.client.AdminPanelStateClientHandler;
import com.example.revolution.RevolutionMod;
import com.example.revolution.network.PingPacket;
import com.example.revolution.client.PlayerStatsPacketHandler;
import com.example.revolution.network.RuneCollectionsPacket;
import com.example.revolution.client.RuneCollectionsPacketHandler;
import com.example.revolution.network.RuneCollectionsPacketServerHandler;
import com.example.revolution.network.RuneEnhancementPacket;
import com.example.revolution.client.RuneEnhancementPacketHandler;
import com.example.revolution.network.RuneEnhancementPacketServerHandler;
import com.example.revolution.client.RuneSlotPacketHandler;
import com.example.revolution.admin.AdminPanelRequestClientHandler;
import com.example.revolution.admin.AdminPanelStatePacket;
import com.example.revolution.admin.AdminPanelStateServerHandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;

public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("revolution:main");

    public static void registerServer() {
        INSTANCE.registerMessage(PingPacket.Handler.class, PingPacket.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PongPacketServerHandler.class, PongPacket.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(RuneSlotPacketServerHandler.class, RuneSlotPacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(RuneCollectionsPacketServerHandler.class, RuneCollectionsPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PlayerStatsPacketServerHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        INSTANCE.registerMessage(AdminPanelRequestPacket.Handler.class, AdminPanelRequestPacket.class, 5, Side.SERVER);
        INSTANCE.registerMessage(AdminPanelStateServerHandler.class, AdminPanelStatePacket.class, 6, Side.CLIENT);
        INSTANCE.registerMessage(RuneEnhancementPacketServerHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
    }

    public static void registerClient() {
        INSTANCE.registerMessage(PingPacketClientHandler.class, PingPacket.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(PongPacketHandler.class, PongPacket.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(RuneSlotPacketHandler.class, RuneSlotPacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(RuneCollectionsPacketHandler.class, RuneCollectionsPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PlayerStatsPacketHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        INSTANCE.registerMessage(AdminPanelRequestClientHandler.class, AdminPanelRequestPacket.class, 5, Side.SERVER);
        INSTANCE.registerMessage(AdminPanelStateClientHandler.class, AdminPanelStatePacket.class, 6, Side.CLIENT);
        INSTANCE.registerMessage(RuneEnhancementPacketHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
    }

    /**
     * Returns true only when the connecting client advertised this mod during the Forge handshake.
     * Vanilla/no-mod clients are intentionally allowed to join, but must never receive custom packets.
     */
    public static boolean clientHasRevolutionMod(EntityPlayerMP player) {
        if (player == null || player.connection == null || player.connection.netManager == null) return false;
        try {
            NetworkDispatcher dispatcher = NetworkDispatcher.get(player.connection.netManager);
            if (dispatcher == null) return false;
            Map<String, String> modList = dispatcher.getModList();
            return modList != null && modList.containsKey(RevolutionMod.MODID);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Safely sends a custom packet only to clients that actually have RevolutionMod installed. */
    public static boolean sendToClient(IMessage message, EntityPlayerMP player) {
        if (message == null || !clientHasRevolutionMod(player)) return false;
        INSTANCE.sendTo(message, player);
        return true;
    }
}
