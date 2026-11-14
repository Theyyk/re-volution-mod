package com.example.revolution.network;

import net.minecraftforge.fml.relauncher.Side;

/** UI-03-M extension IDs after Mechanic Unlocks 27/28; legacy IDs remain unchanged. */
public final class MasteryNetworkRegistration {
    private MasteryNetworkRegistration() {}
    public static void registerServer() {
        NetworkHandler.INSTANCE.registerMessage(MasteryActionPacketHandler.class, MasteryActionPacket.class, 29, Side.SERVER);
        NetworkHandler.INSTANCE.registerMessage(MasterySnapshotPacketServerHandler.class, MasterySnapshotPacket.class, 30, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(CombatProjectionPacketServerHandler.class, CombatProjectionPacket.class, 31, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RunExchangeActionPacketHandler.class, RunExchangeActionPacket.class, 32, Side.SERVER);
        NetworkHandler.INSTANCE.registerMessage(RunExchangeSnapshotPacketServerHandler.class, RunExchangeSnapshotPacket.class, 33, Side.CLIENT);
    }
}
