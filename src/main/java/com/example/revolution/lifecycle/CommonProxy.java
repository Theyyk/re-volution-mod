package com.example.revolution.lifecycle;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Physical side boundary: this class never references Minecraft client classes. */
public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ServerLifecycle.preInit(event.getModConfigurationDirectory(), event.getModLog());
    }
    public void init() {}
}
