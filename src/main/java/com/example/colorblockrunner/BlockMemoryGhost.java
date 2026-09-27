package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Remembers non-air blocks and shows a ghost while their original position is empty. */
public final class BlockMemoryGhost {
    private static final Map<BlockPos, BlockState> remembered = new HashMap<>();
    private static final Set<BlockPos> dismissed = new HashSet<>();
    private static boolean middleWasDown;

    private BlockMemoryGhost() {}

    public static void tick(MinecraftClient client) {
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.ghostEnabled || client.player == null || client.world == null) return;

        int radius = Math.max(2, Math.min(64, c.ghostRange));
        BlockPos center = client.player.getBlockPos();
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = Math.max(client.world.getBottomY(), center.getY() - radius);
        int maxY = Math.min(client.world.getTopYInclusive(), center.getY() + radius);
        int minZ = center.getZ() - radius, maxZ = center.getZ() + radius;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!client.world.isChunkLoaded(new ChunkPos(x >> 4, z >> 4).toLong())) continue;
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (remembered.containsKey(pos)) continue;
                    BlockState state = client.world.getBlockState(pos);
                    if (!state.isAir()) remembered.put(pos.toImmutable(), state);
                }
            }
        }

        // Only an empty position is considered a disappearance. A different replacement block
        // is not treated as a disappearance until that replacement is removed as well.
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = client.world.getBlockState(pos);
            if (current.isAir()) {
                if (!dismissed.contains(pos)) continue;
            } else if (current.equals(entry.getValue())) {
                dismissed.remove(pos); // restored; a later second disappearance can show again
            } else {
                dismissed.remove(pos);
            }
        }

        boolean middle = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (middle && !middleWasDown && client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos pos = hit.getBlockPos();
            if (isGhost(pos)) dismissed.add(pos);
        }
        middleWasDown = middle;
    }

    private static boolean isGhost(BlockPos pos) {
        return remembered.containsKey(pos) && !dismissed.contains(pos);
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.ghostEnabled || client.world == null || client.player == null || remembered.isEmpty()) return;
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) return;

        VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
        VertexConsumer outline = consumers.getBuffer(RenderLayer.getDebugLineStrip(2.0));
        var camera = context.camera().getPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!isGhost(pos) || !client.world.getBlockState(pos).isAir()) continue;
            float[] rgb = colorFor(entry.getValue());
            VertexRendering.drawFilledBox(matrices, fill, pos.getX() + 0.02, pos.getY() + 0.02, pos.getZ() + 0.02,
                    pos.getX() + 0.98, pos.getY() + 0.98, pos.getZ() + 0.98, rgb[0], rgb[1], rgb[2], 0.28f);
            VertexRendering.drawBox(matrices, outline, pos.getX() + 0.02, pos.getY() + 0.02, pos.getZ() + 0.02,
                    pos.getX() + 0.98, pos.getY() + 0.98, pos.getZ() + 0.98, rgb[0], rgb[1], rgb[2], 0.9f);
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
