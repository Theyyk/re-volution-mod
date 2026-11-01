package com.example.revolution.client.input;

import com.example.revolution.client.ClientUiBridge;
import com.example.revolution.client.mechanic.ClientMechanicUnlocks;
import com.example.revolution.client.network.AuraClientState;
import com.example.revolution.client.network.GodWorshipClientState;
import com.example.revolution.mechanic.BalanceMechanicUnlockPolicy;
import com.example.revolution.network.AuraActionPacket;
import com.example.revolution.network.GodWorshipActionPacket;
import com.example.revolution.network.NetworkHandler;
import com.example.revolution.network.QuestActionPacket;
import com.example.revolution.network.StageTravelMenuRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class KeyInputHandler {
    public static KeyBinding keyOpenMenu;
    public static KeyBinding keyOpenStageTravel;
    public static KeyBinding keyOpenQuests;
    public static KeyBinding keyOpenGodWorship;
    public static KeyBinding keyOpenAura;
    private boolean menuWasPressed = false;
    private boolean stageTravelWasPressed = false;
    private boolean questsWasPressed = false;
    private boolean godWorshipWasPressed = false;
    private boolean auraWasPressed = false;
    private boolean vanillaBindingsConfigured = false;

    public static void register() {
        keyOpenMenu = new KeyBinding("key.revolution.menu", Keyboard.KEY_E, "key.categories.revolution");
        keyOpenStageTravel = new KeyBinding("key.revolution.stage_travel", Keyboard.KEY_L, "key.categories.revolution");
        keyOpenQuests = new KeyBinding("key.revolution.quests", Keyboard.KEY_Q, "key.categories.revolution");
        keyOpenGodWorship = new KeyBinding("key.revolution.god_worship", Keyboard.KEY_G, "key.categories.revolution");
        keyOpenAura = new KeyBinding("key.revolution.aura", Keyboard.KEY_U, "key.categories.revolution");
        ClientRegistry.registerKeyBinding(keyOpenMenu);
        ClientRegistry.registerKeyBinding(keyOpenStageTravel);
        ClientRegistry.registerKeyBinding(keyOpenQuests);
        ClientRegistry.registerKeyBinding(keyOpenGodWorship);
        ClientRegistry.registerKeyBinding(keyOpenAura);
    }

    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientUiBridge.tickRunePreloader();
        Minecraft minecraft = Minecraft.getMinecraft();
        configureVanillaBindings(minecraft);
        if (minecraft.gameSettings.attackIndicator != 0) minecraft.gameSettings.attackIndicator = 0;

        boolean menuPressed = keyOpenMenu != null && keyOpenMenu.isPressed();
        if (menuPressed && !menuWasPressed && minecraft.player != null && minecraft.getConnection() != null && minecraft.currentScreen == null && !ClientUiBridge.isRuneOpenPending()) ClientUiBridge.beginRuneOpen();
        menuWasPressed = menuPressed;

        boolean stageTravelPressed = keyOpenStageTravel != null && keyOpenStageTravel.isPressed();
        if (stageTravelPressed && !stageTravelWasPressed && minecraft.player != null && minecraft.getConnection() != null && minecraft.currentScreen == null) NetworkHandler.INSTANCE.sendToServer(new StageTravelMenuRequestPacket());
        stageTravelWasPressed = stageTravelPressed;

        boolean questsPressed = keyOpenQuests != null && keyOpenQuests.isPressed();
        if (questsPressed && !questsWasPressed && minecraft.player != null && minecraft.getConnection() != null && minecraft.currentScreen == null) NetworkHandler.INSTANCE.sendToServer(new QuestActionPacket("sync"));
        questsWasPressed = questsPressed;

        boolean godWorshipPressed = keyOpenGodWorship != null && keyOpenGodWorship.isPressed();
        if (godWorshipPressed && !godWorshipWasPressed && minecraft.player != null && minecraft.getConnection() != null && minecraft.currentScreen == null
                && ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.GOD_WORSHIP)) {
            GodWorshipClientState.requestOpen();
            NetworkHandler.INSTANCE.sendToServer(new GodWorshipActionPacket("sync", "", "", "", 0L));
        }
        godWorshipWasPressed = godWorshipPressed;

        boolean auraPressed = keyOpenAura != null && keyOpenAura.isPressed();
        if (auraPressed && !auraWasPressed && minecraft.player != null && minecraft.getConnection() != null && minecraft.currentScreen == null
                && ClientMechanicUnlocks.isUnlocked(BalanceMechanicUnlockPolicy.AURA_SYSTEM)) {
            AuraClientState.requestOpen();
            NetworkHandler.INSTANCE.sendToServer(new AuraActionPacket("sync", "", "", 0L));
        }
        auraWasPressed = auraPressed;
    }

    private void configureVanillaBindings(Minecraft minecraft) {
        if (vanillaBindingsConfigured || minecraft == null || minecraft.gameSettings == null) return;
        minecraft.gameSettings.keyBindInventory.setKeyCode(Keyboard.KEY_NONE);
        minecraft.gameSettings.keyBindAdvancements.setKeyCode(Keyboard.KEY_NONE);
        minecraft.gameSettings.attackIndicator = 0;
        KeyBinding.resetKeyBindingArrayAndHash();
        minecraft.gameSettings.saveOptions();
        vanillaBindingsConfigured = true;
    }
}
