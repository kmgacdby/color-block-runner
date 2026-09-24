package com.example.colorblockrunner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class UtilitySuiteClient implements ClientModInitializer {
 private static boolean menuWasDown,stewWasDown,runnerWasDown,dodgeWasDown;
 @Override public void onInitializeClient(){UnifiedConfig.load();ClientTickEvents.END_CLIENT_TICK.register(UtilitySuiteClient::tick);}
 private static void tick(MinecraftClient client){long w=client.getWindow().getHandle();UnifiedConfig c=UnifiedConfig.get();boolean menu=key(w,c.menuKey);if(menu&&!menuWasDown&&client.currentScreen==null)client.setScreen(new UtilitySuiteScreen(null));menuWasDown=menu;
  if(client.currentScreen==null){boolean stew=key(w,c.stewKey),runner=key(w,c.runnerKey),dodge=key(w,c.dodgeKey);if(stew&&!stewWasDown){c.stewEnabled=!c.stewEnabled;c.save();}if(runner&&!runnerWasDown){c.runnerEnabled=!c.runnerEnabled;c.save();ColorBlockRunnerClient.resetTarget(client);}if(dodge&&!dodgeWasDown){c.dodgeEnabled=!c.dodgeEnabled;c.save();}stewWasDown=stew;runnerWasDown=runner;dodgeWasDown=dodge;if(c.runnerEnabled)ColorBlockRunnerClient.tickExternal(client);if(c.dodgeEnabled)ArrowDodge.tick(client);if(c.stewEnabled)StewMover.tickExternal(client);}
 }
 private static boolean key(long w,int k){return k>0&&GLFW.glfwGetKey(w,k)==GLFW.GLFW_PRESS;}
}
