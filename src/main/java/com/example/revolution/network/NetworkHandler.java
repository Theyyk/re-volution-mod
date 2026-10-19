package com.example.revolution.network;

import com.example.revolution.RevolutionMod;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel("revolution:main");

    public static void registerServer() {
        INSTANCE.registerMessage(RateLimitedPingPacketHandler.class, PingPacket.class, 0, Side.SERVER);
        INSTANCE.registerMessage(PongPacketServerHandler.class, PongPacket.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(RuneSlotPacketServerHandler.class, RuneSlotPacket.class, 2, Side.CLIENT);
        INSTANCE.registerMessage(RuneCollectionsPacketServerHandler.class, RuneCollectionsPacket.class, 3, Side.CLIENT);
        INSTANCE.registerMessage(PlayerStatsPacketServerHandler.class, PlayerStatsPacket.class, 4, Side.CLIENT);
        // Discriminators 5 and 6 are reserved for the removed Minecraft admin-panel transport.
        INSTANCE.registerMessage(RuneEnhancementPacketServerHandler.class, RuneEnhancementPacket.class, 7, Side.CLIENT);
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
