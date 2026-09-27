package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client-side memory of nearby non-air blocks. Empty original positions are shown as ghosts. */
public final class BlockMemoryGhost {
    private static final Map<BlockPos, BlockState> remembered = new HashMap<>();
    private static final Set<BlockPos> dismissed = new HashSet<>();
    private static boolean middleWasDown;
    private static int scanCooldown;

    private BlockMemoryGhost() {}

    public static void tick(MinecraftClient client) {
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.ghostEnabled || client.player == null || client.world == null) return;
        if (--scanCooldown <= 0) {
            scanCooldown = 10;
            scan(client, Math.max(2, Math.min(32, c.ghostRange)));
        }
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            if (client.world.getBlockState(entry.getKey()).equals(entry.getValue())) dismissed.remove(entry.getKey());
        }
        boolean middle = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (middle && !middleWasDown && client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            if (isGhost(client, pos)) dismissed.add(pos.toImmutable());
        }
        middleWasDown = middle;
    }

    private static void scan(MinecraftClient client, int radius) {
        BlockPos center = client.player.getBlockPos();
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = Math.max(client.world.getBottomY(), center.getY() - radius);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + radius);
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!client.world.isChunkLoaded(x >> 4, z >> 4)) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (remembered.containsKey(pos)) continue;
                    BlockState state = client.world.getBlockState(pos);
                    if (!state.isAir()) remembered.put(pos.toImmutable(), state);
                }
            }
        }
    }

    private static boolean isGhost(MinecraftClient client, BlockPos pos) {
        return remembered.containsKey(pos) && !dismissed.contains(pos) && client.world.getBlockState(pos).isAir();
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.ghostEnabled || client.world == null || client.player == null || remembered.isEmpty()) return;
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) return;

        VertexConsumer outline = consumers.getBuffer(RenderLayer.getLines());
        var camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!isGhost(client, pos)) continue;
            float[] rgb = colorFor(entry.getValue());
            double x = pos.getX() + .03, y = pos.getY() + .03, z = pos.getZ() + .03;
            WorldRenderer.drawBox(matrices, outline, x, y, z, x + .94, y + .94, z + .94, rgb[0], rgb[1], rgb[2], .95f);
        }
        matrices.pop();
    }

    private static float[] colorFor(BlockState state) {
        String id = state.getBlock().getTranslationKey();
        if (id.contains("red")) return new float[]{1f, .28f, .32f};
        if (id.contains("blue")) return new float[]{.32f, .55f, 1f};
        if (id.contains("green")) return new float[]{.35f, .9f, .5f};
        if (id.contains("yellow")) return new float[]{1f, .85f, .25f};
        if (id.contains("purple")) return new float[]{.75f, .4f, 1f};
        if (id.contains("pink")) return new float[]{1f, .5f, .8f};
        if (id.contains("orange")) return new float[]{1f, .55f, .2f};
        return new float[]{.78f, .82f, .9f};
    }

    public static void clearAll() { remembered.clear(); dismissed.clear(); }
}
