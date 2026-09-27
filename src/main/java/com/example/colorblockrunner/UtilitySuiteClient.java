package com.example.colorblockrunner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class UtilitySuiteClient implements ClientModInitializer {
    private static boolean menuWasDown, stewWasDown, runnerWasDown, ghostWasDown;
    @Override public void onInitializeClient() {
        UnifiedConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(UtilitySuiteClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(BlockMemoryGhost::render);
    }
    private static void toggleMessage(MinecraftClient client, String name, boolean enabled) {
        if (client.player != null) client.player.sendMessage(Text.literal(name + (enabled ? " 已开启" : " 已关闭")), true);
    }
    private static void tick(MinecraftClient client) {
        long w = client.getWindow().getHandle(); UnifiedConfig c = UnifiedConfig.get();
        boolean menu = key(w, c.menuKey);
        if (menu && !menuWasDown && client.currentScreen == null) client.setScreen(new UtilitySuiteScreen(null));
        menuWasDown = menu;
        if (client.currentScreen == null) {
            boolean stew = key(w, c.stewKey), runner = key(w, c.runnerKey), ghost = key(w, c.ghostKey);
            if (stew && !stewWasDown) { c.stewEnabled = !c.stewEnabled; c.save(); StewMover.resetTimer(); toggleMessage(client, "蘑菇煲自动搬运", c.stewEnabled); }
            if (runner && !runnerWasDown) { c.runnerEnabled = !c.runnerEnabled; c.save(); ColorBlockRunnerClient.resetTarget(client); toggleMessage(client, "同色方块跑酷", c.runnerEnabled); }
            if (ghost && !ghostWasDown) { c.ghostEnabled = !c.ghostEnabled; c.save(); toggleMessage(client, "方块记忆虚影", c.ghostEnabled); }
            stewWasDown = stew; runnerWasDown = runner; ghostWasDown = ghost;
            if (c.runnerEnabled) ColorBlockRunnerClient.tickExternal(client); else ColorBlockRunnerClient.resetTarget(client);
            if (c.stewEnabled) StewMover.tickExternal(client);
            if (c.ghostEnabled) BlockMemoryGhost.tick(client);
        }
    }
    private static boolean key(long w, int k) { return k > 0 && GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS; }
}
