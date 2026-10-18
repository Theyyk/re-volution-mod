package com.example.customguimod;

import com.example.customguimod.persistence.MongoManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = CustomGuiMod.MODID)
public class ServerInit {

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event) {
        MongoManager.connect();
        CustomGuiMod.logger.info("MongoDB подключена на сервере!");
    }
}
