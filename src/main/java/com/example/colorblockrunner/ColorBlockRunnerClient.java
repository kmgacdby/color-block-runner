package com.example.colorblockrunner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class ColorBlockRunnerClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    private static KeyBinding configKey;
    private static BlockPos target;
    private static boolean controlling;
    private static boolean savedForward, savedJump, savedSprint;

    @Override
    public void onInitializeClient() {
        Config.load();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.colorblockrunner.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.colorblockrunner"));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.colorblockrunner.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.colorblockrunner"));
        ClientTickEvents.END_CLIENT_TICK.register(ColorBlockRunnerClient::tick);
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            Config.enabled = !Config.enabled;
            Config.save();
            if (Config.enabled) start(client); else stop(client);
        }
        while (configKey.wasPressed()) client.setScreen(new ConfigScreen(client.currentScreen));

        if (!Config.enabled || client.player == null || client.world == null || client.currentScreen != null) {
            if (!Config.enabled) stop(client);
            return;
        }
        if (target == null) {
            findTarget(client);
            if (target == null) { stopMovement(client); return; }
        }
        if (isAtTarget(client)) {
            target = null;
            stopMovement(client);
            return;
        }
        moveTowardTarget(client);
    }

    private static void start(MinecraftClient client) {
        target = null;
        stopMovement(client);
        if (client.player != null && client.world != null) findTarget(client);
    }

    public static void resetTarget(MinecraftClient client) {
        target = null;
        stopMovement(client);
    }

    private static void findTarget(MinecraftClient client) {
        ItemStack held = client.player.getMainHandStack();
        DyeColor heldColor = null;
        if (held.getItem() instanceof net.minecraft.item.BlockItem blockItem)
            heldColor = ColorBlock.getDyeColor(blockItem.getBlock());
        if (heldColor == null) return;

        BlockPos under = client.player.getBlockPos().down();
        DyeColor underColor = ColorBlock.getDyeColor(client.world.getBlockState(under).getBlock());
        if (heldColor == underColor) return;

        int centerX = under.getX(), centerZ = under.getZ(), y = under.getY();
        int r = Config.clamp(Config.range, 4, 256);
        double bestDistSq = Double.MAX_VALUE;
        BlockPos best = null;
        int minX = centerX - r, maxX = centerX + r;
        int minZ = centerZ - r, maxZ = centerZ + r;
        int minChunkX = Math.floorDiv(minX, 16), maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16), maxChunkZ = Math.floorDiv(maxZ, 16);

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!client.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                int fromX = Math.max(minX, cx * 16), toX = Math.min(maxX, cx * 16 + 15);
                int fromZ = Math.max(minZ, cz * 16), toZ = Math.min(maxZ, cz * 16 + 15);
                for (int x = fromX; x <= toX; x++) {
                    for (int z = fromZ; z <= toZ; z++) {
                        int dx = x - centerX, dz = z - centerZ;
                        double distSq = (double) dx * dx + (double) dz * dz;
                        if (distSq == 0 || distSq > (double) r * r || distSq >= bestDistSq) continue;
                        BlockState state = client.world.getBlockState(new BlockPos(x, y, z));
                        if (ColorBlock.getDyeColor(state.getBlock()) == heldColor) {
                            bestDistSq = distSq;
                            best = new BlockPos(x, y, z);
                        }
                    }
                }
            }
        }
        target = best;
    }

    private static boolean isAtTarget(MinecraftClient client) {
        BlockPos under = client.player.getBlockPos().down();
        return target != null && under.getX() == target.getX() && under.getY() == target.getY() && under.getZ() == target.getZ();
    }

    private static void moveTowardTarget(MinecraftClient client) {
        double tx = target.getX() + 0.5, tz = target.getZ() + 0.5;
        double dx = tx - client.player.getX(), dz = tz - client.player.getZ();
        client.player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        if (!controlling) saveInputState(client);
        client.options.forwardKey.setPressed(true);
        client.options.jumpKey.setPressed(Config.autoJump || savedJump);
        client.options.sprintKey.setPressed(Config.autoSprint || savedSprint);
        if (Config.autoSprint) client.player.setSprinting(true);
        controlling = true;
    }

    private static void saveInputState(MinecraftClient client) {
        savedForward = client.options.forwardKey.isPressed();
        savedJump = client.options.jumpKey.isPressed();
        savedSprint = client.options.sprintKey.isPressed();
    }

    private static void stopMovement(MinecraftClient client) {
        if (!controlling) return;
        client.options.forwardKey.setPressed(savedForward);
        client.options.jumpKey.setPressed(savedJump);
        client.options.sprintKey.setPressed(savedSprint);
        if (client.player != null && Config.autoSprint) client.player.setSprinting(savedSprint);
        controlling = false;
    }

    private static void stop(MinecraftClient client) {
        target = null;
        stopMovement(client);
    }
}
