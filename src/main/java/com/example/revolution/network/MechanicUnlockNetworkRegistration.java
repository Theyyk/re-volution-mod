package com.example.revolution.network;

import net.minecraftforge.fml.relauncher.Side;

/** v2.12.0 extension registration kept separate from the stable legacy discriminator contract. */
public final class MechanicUnlockNetworkRegistration {
    private MechanicUnlockNetworkRegistration() {}

    public static void registerServer() {
        NetworkHandler.INSTANCE.registerMessage(
                MechanicUnlockActionPacketHandler.class,
                MechanicUnlockActionPacket.class,
                27,
                Side.SERVER);
        NetworkHandler.INSTANCE.registerMessage(
                MechanicUnlockSnapshotPacketServerHandler.class,
                MechanicUnlockSnapshotPacket.class,
                28,
                Side.CLIENT);
    }
}
