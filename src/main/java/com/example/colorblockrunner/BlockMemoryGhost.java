package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BlockMemoryGhost {
    private static final Map<BlockPos, BlockState> remembered = new HashMap<>();
    private static final Set<BlockPos> dismissed = new HashSet<>();
    private static boolean middleWasDown;
    private static int scanCooldown;
    private static long nextPlaceAt;

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
        if (c.ghostAutoPlace && System.currentTimeMillis() >= nextPlaceAt) {
            if (tryAutoPlaceNearest(client)) {
                nextPlaceAt = System.currentTimeMillis() + Math.max(50, Math.min(2000, c.ghostPlaceDelay));
            } else {
                nextPlaceAt = System.currentTimeMillis() + 100;
            }
        }
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

    private static boolean tryAutoPlaceNearest(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return false;
        BlockPos best = null;
        BlockState bestState = null;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!isGhost(client, pos)) continue;
            double distance = client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
                bestState = entry.getValue();
            }
        }
        if (best == null || bestDistance > 64.0) return false;
        int slot = findHotbarBlock(client, bestState);
        if (slot < 0) return false;
        BlockHitResult hit = findPlacementHit(client, best);
        if (hit == null) return false;

        int oldSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(slot);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.getInventory().setSelectedSlot(oldSlot);
        return true;
    }

    private static int findHotbarBlock(MinecraftClient client, BlockState desired) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == desired.getBlock() && !stack.isEmpty()) return i;
        }
        return -1;
    }

    private static BlockHitResult findPlacementHit(MinecraftClient client, BlockPos ghost) {
        for (Direction face : Direction.values()) {
            BlockPos support = ghost.offset(face.getOpposite());
            if (!client.world.isChunkLoaded(support.getX() >> 4, support.getZ() >> 4)) continue;
            BlockState supportState = client.world.getBlockState(support);
            if (supportState.isAir() || supportState.getCollisionShape(client.world, support).isEmpty()) continue;
            Vec3d hitPos = Vec3d.ofCenter(support).add(Vec3d.of(face.getVector()).multiply(0.5D));
            return new BlockHitResult(hitPos, face, support, false);
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
            renderer.renderBlockAsEntity(entry.getValue(), matrices, consumers, 0xF000F0, 0);
            matrices.pop();
        }
        matrices.pop();
    }

    public static void clearAll() {
        remembered.clear();
        dismissed.clear();
        nextPlaceAt = System.currentTimeMillis() + 250;
    }
}
