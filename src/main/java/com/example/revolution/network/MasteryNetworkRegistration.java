package com.example.revolution.network;

import net.minecraftforge.fml.relauncher.Side;

/** UI-03-M extension IDs after Mechanic Unlocks 27/28; legacy IDs remain unchanged. */
public final class MasteryNetworkRegistration {
    private MasteryNetworkRegistration() {}
    public static void registerServer() {
        NetworkHandler.INSTANCE.registerMessage(MasteryActionPacketHandler.class, MasteryActionPacket.class, 29, Side.SERVER);
        NetworkHandler.INSTANCE.registerMessage(MasterySnapshotPacketServerHandler.class, MasterySnapshotPacket.class, 30, Side.CLIENT);
    }
}
