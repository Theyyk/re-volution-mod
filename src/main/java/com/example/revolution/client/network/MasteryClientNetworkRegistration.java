package com.example.revolution.client.network;

import com.example.revolution.network.MasteryActionPacket;
import com.example.revolution.network.CombatProjectionPacket;
import com.example.revolution.network.MasterySnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.RunExchangeActionPacket;
import com.example.revolution.network.RunExchangeSnapshotPacket;
import net.minecraftforge.fml.relauncher.Side;

/** UI-03-M client extension, separate from stable wire contract IDs 0..26. */
public final class MasteryClientNetworkRegistration {
    private MasteryClientNetworkRegistration() {}
    public static void register() {
        NetworkHandler.INSTANCE.registerMessage(MasteryActionPacketClientHandler.class, MasteryActionPacket.class, 29, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(MasterySnapshotPacketHandler.class, MasterySnapshotPacket.class, 30, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(CombatProjectionPacketHandler.class, CombatProjectionPacket.class, 31, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RunExchangeActionPacketClientHandler.class, RunExchangeActionPacket.class, 32, Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(RunExchangeSnapshotPacketHandler.class, RunExchangeSnapshotPacket.class, 33, Side.CLIENT);
    }
}
