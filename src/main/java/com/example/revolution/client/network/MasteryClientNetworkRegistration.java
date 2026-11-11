package com.example.revolution.client.network;

import com.example.revolution.network.MasteryActionPacket;
import com.example.revolution.network.MasterySnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraftforge.fml.relauncher.Side;

/** UI-03-M client extension, separate from stable wire contract IDs 0..26. */
public final class MasteryClientNetworkRegistration {
    private MasteryClientNetworkRegistration() {}
    public static void register() {
        NetworkHandler.INSTANCE.registerMessage(MasteryActionPacketClientHandler.class, MasteryActionPacket.class, 29, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MasterySnapshotPacketHandler.class, MasterySnapshotPacket.class, 30, Side.CLIENT);
    }
}
