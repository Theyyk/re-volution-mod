package com.example.revolution.client;

import com.example.revolution.client.lifecycle.ClientLifecycle;
import com.example.revolution.lifecycle.CommonProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override public void preInit(FMLPreInitializationEvent event) {
        ClientLifecycle.preInit(event.getModConfigurationDirectory());
    }
    @Override public void init() { ClientLifecycle.init(); }
}
