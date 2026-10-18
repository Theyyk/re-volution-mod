package com.example.revolution;

import com.example.revolution.persistence.MongoManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = RevolutionMod.MODID)
public class ServerInit {

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event) {
        MongoManager.connect();
        RevolutionMod.logger.info("MongoDB подключена на сервере!");
    }
}
