package com.example.colorblockrunner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class UtilitySuiteClient implements ClientModInitializer {
    public static KeyBinding menuKey;
    @Override public void onInitializeClient(){
        UnifiedConfig.load();
        menuKey=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.utilitysuite.menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_O,"category.utilitysuite"));
        ClientTickEvents.END_CLIENT_TICK.register(UtilitySuiteClient::tick);
    }
    private static void tick(MinecraftClient client){
        while(menuKey.wasPressed()) client.setScreen(new UtilitySuiteScreen(null));
        UnifiedConfig c=UnifiedConfig.get();
        if(c.runnerEnabled && client.currentScreen==null) ColorBlockRunnerClient.tickExternal(client);
        if(c.dodgeEnabled && client.currentScreen==null) ArrowDodge.tick(client);
        if(c.stewEnabled && client.currentScreen==null) StewMover.tickExternal(client);
    }
}
