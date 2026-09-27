package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Remembers nearby block states and renders the original block model when the position becomes air. */
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
        if (middle && !middleWasDown) {
            BlockPos ghost = findGhostUnderCrosshair(client, Math.max(6, Math.min(64, c.ghostRange * 2)));
            if (ghost != null) dismissed.add(ghost.toImmutable());
        }
        middleWasDown = middle;
    }

    private static void scan(MinecraftClient client, int radius) {
        BlockPos center = client.player.getBlockPos();
        int minX = center.getX() - radius, maxX = center.getX() + radius;
        int minY = Math.max(client.world.getBottomY(), center.getY() - radius);
        int maxY = Math.min(client.world.getTopY(), center.getY() + radius);
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

    private static BlockPos findGhostUnderCrosshair(MinecraftClient client, double maxDistance) {
        Vec3d start = client.gameRenderer.getCamera().getPos();
        Vec3d direction = client.player.getRotationVec(1.0F).normalize();
        double step = 0.05D;
        BlockPos last = null;
        for (double distance = 0.0D; distance <= maxDistance; distance += step) {
            Vec3d p = start.add(direction.multiply(distance));
            BlockPos pos = BlockPos.ofFloored(p);
            if (pos.equals(last)) continue;
            last = pos;
            if (isGhost(client, pos)) return pos;
        }
        return null;
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.ghostEnabled || client.world == null || client.player == null || remembered.isEmpty()) return;
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrices = context.matrixStack();
        if (consumers == null || matrices == null) return;

        Vec3d camera = context.camera().getPos();
        BlockRenderManager renderer = client.getBlockRenderManager();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!isGhost(client, pos)) continue;
            matrices.push();
            matrices.translate(pos.getX(), pos.getY(), pos.getZ());
            renderer.renderBlockAsEntity(entry.getValue(), 0.0D, 0.0D, 0.0D, client.world, matrices, consumers, 0xF000F0, 0);
            matrices.pop();
        }
        matrices.pop();
    }

    public static void clearAll() {
        remembered.clear();
        dismissed.clear();
    }
}
