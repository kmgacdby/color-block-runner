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
    private static boolean menuWasDown, stewWasDown, runnerWasDown, ghostWasDown, middleMouseWasDown;

    @Override public void onInitializeClient() {
        UnifiedConfig.load();
        UnifiedConfig c = UnifiedConfig.get();
        syncGhostConfig(c);
        ClientTickEvents.END_CLIENT_TICK.register(UtilitySuiteClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(BlockMemoryGhost::render);
    }

    private static void syncGhostConfig(UnifiedConfig c) {
        BlockMemoryGhost.setEnabled(c.ghostEnabled);
        BlockMemoryGhost.setRange(c.ghostRange);
        BlockMemoryGhost.setAutoPlace(c.ghostAutoPlace);
        BlockMemoryGhost.setPlaceInterval(c.ghostPlaceDelay);
    }

    private static void toggleMessage(MinecraftClient client, String name, boolean enabled) {
        if (client.player != null) client.player.sendMessage(Text.literal(name + (enabled ? " 已开启" : " 已关闭")), true);
    }

    private static void tick(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        UnifiedConfig c = UnifiedConfig.get();
        boolean menu = key(window, c.menuKey);
        if (menu && !menuWasDown && client.currentScreen == null) client.setScreen(new UtilitySuiteScreen(null));
        menuWasDown = menu;
        syncGhostConfig(c);

        boolean middleDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (middleDown && !middleMouseWasDown && client.currentScreen == null && c.ghostEnabled) {
            if (BlockMemoryGhost.cancelGhostUnderCrosshair(client) && client.player != null) {
                client.player.sendMessage(Text.literal("已取消此处虚影"), true);
            }
        }
        middleMouseWasDown = middleDown;

        if (client.currentScreen == null) {
            boolean stew = key(window, c.stewKey);
            boolean runner = key(window, c.runnerKey);
            boolean ghost = key(window, c.ghostKey);
            if (stew && !stewWasDown) {
                c.stewEnabled = !c.stewEnabled; c.save(); StewMover.resetTimer();
                toggleMessage(client, "蘑菇煲自动搬运", c.stewEnabled);
            }
            if (runner && !runnerWasDown) {
                c.runnerEnabled = !c.runnerEnabled; c.save(); ColorBlockRunnerClient.resetTarget(client);
                toggleMessage(client, "同色方块跑酷", c.runnerEnabled);
            }
            if (ghost && !ghostWasDown) {
                c.ghostEnabled = !c.ghostEnabled; c.save(); syncGhostConfig(c);
                toggleMessage(client, "方块记忆虚影", c.ghostEnabled);
            }
            stewWasDown = stew; runnerWasDown = runner; ghostWasDown = ghost;
            if (c.runnerEnabled) ColorBlockRunnerClient.tickExternal(client); else ColorBlockRunnerClient.resetTarget(client);
            if (c.stewEnabled) StewMover.tickExternal(client);
            if (c.ghostEnabled) BlockMemoryGhost.tick(client);
        } else {
            // Require a fresh key press after closing a screen; don't toggle from a held key.
            stewWasDown = key(window, c.stewKey);
            runnerWasDown = key(window, c.runnerKey);
            ghostWasDown = key(window, c.ghostKey);
        }
    }

    private static boolean key(long window, int key) {
        return key > 0 && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}