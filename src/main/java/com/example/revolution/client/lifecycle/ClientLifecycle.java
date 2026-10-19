package com.example.revolution.client.lifecycle;

import com.example.revolution.RevolutionMod;
import com.example.revolution.client.ClientBootstrap;
import com.example.revolution.client.network.ClientNetworkRegistration;
import com.example.revolution.lifecycle.ServerLifecycle;
import com.example.revolution.number.LargeNumbers;
import java.io.File;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientLifecycle {
    private ClientLifecycle() {}

    public static void preInit(File directory) {
        ServerLifecycle.configureClient(directory, RevolutionMod.logger);
        LargeNumbers.initDisplaySettings(directory);
        ClientNetworkRegistration.register();
    }

    public static void init() {
        ClientBootstrap.init();
    }
}
