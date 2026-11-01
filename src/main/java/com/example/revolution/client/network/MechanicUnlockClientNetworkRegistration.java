package com.example.revolution.client.network;

import com.example.revolution.network.MechanicUnlockActionPacket;
import com.example.revolution.network.MechanicUnlockSnapshotPacket;
import com.example.revolution.network.NetworkHandler;
import net.minecraftforge.fml.relauncher.Side;

/** Client-side v2.12.0 extension registration for mechanic unlock packets. */
public final class MechanicUnlockClientNetworkRegistration {
    private MechanicUnlockClientNetworkRegistration() {}

    public static void register() {
        NetworkHandler.INSTANCE.registerMessage(
                MechanicUnlockActionPacketClientHandler.class,
                MechanicUnlockActionPacket.class,
                27,
                Side.CLIENT);
        NetworkHandler.INSTANCE.registerMessage(
                MechanicUnlockSnapshotPacketHandler.class,
                MechanicUnlockSnapshotPacket.class,
                28,
                Side.CLIENT);
    }
}
