package com.example.colorblockrunner;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockMemoryGhost {
    private static final Map<BlockPos, BlockState> remembered = new LinkedHashMap<>();
    private static final Map<BlockPos, Long> clearedUntil = new LinkedHashMap<>();
    private static int range = 16;
    private static boolean enabled = false;
    private static boolean autoPlace = false;
    private static long placeIntervalMs = 500L;
    private static long lastPlaceTime = 0L;

    private BlockMemoryGhost() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static void setRange(int value) { range = Math.max(1, Math.min(64, value)); }
    public static int getRange() { return range; }
    public static void setAutoPlace(boolean value) { autoPlace = value; }
    public static boolean isAutoPlace() { return autoPlace; }
    public static void setPlaceInterval(long value) { placeIntervalMs = Math.max(50L, Math.min(2000L, value)); }
    public static long getPlaceInterval() { return placeIntervalMs; }

    public static void clearAll() {
        remembered.clear();
        clearedUntil.clear();
    }

    public static void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return;
        BlockPos center = client.player.getBlockPos();
        int r = range;
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int y = center.getY() - r; y <= center.getY() + r; y++) {
                for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (center.getSquaredDistance(pos) > (double) r * r) continue;
                    BlockState current = client.world.getBlockState(pos);
                    remembered.putIfAbsent(pos.toImmutable(), current);
                }
            }
        }
        if (autoPlace) tryAutoPlace(client);
    }

    public static boolean isGhost(MinecraftClient client, BlockPos pos) {
        BlockState original = remembered.get(pos);
        if (original == null || clearedUntil.containsKey(pos)) return false;
        BlockState current = client.world.getBlockState(pos);
        return current.isAir() && !original.isAir();
    }

    public static boolean cancelAt(MinecraftClient client, BlockPos pos) {
        if (!isGhost(client, pos)) return false;
        clearedUntil.put(pos.toImmutable(), System.currentTimeMillis());
        return true;
    }

    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null || context.camera() == null) return;
        double camX = context.camera().getPos().x;
        double camY = context.camera().getPos().y;
        double camZ = context.camera().getPos().z;
        BlockRenderManager renderer = client.getBlockRenderManager();
        for (Map.Entry<BlockPos, BlockState> entry : remembered.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!isGhost(client, pos)) continue;
            matrices.push();
            matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
            renderer.renderBlockAsEntity(entry.getValue(), matrices, consumers, 15728880, 0);
            matrices.pop();
        }
    }

    private static void tryAutoPlace(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < placeIntervalMs || client.interactionManager == null) return;
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
        if (best == null || bestDistance > 64.0) return;
        int slot = findHotbarBlock(client, bestState);
        if (slot < 0) return;
        BlockHitResult hit = findPlacementHit(client, best);
        if (hit == null) return;
        int oldSlot = client.player.getInventory().selectedSlot;
        client.player.getInventory().selectedSlot = slot;
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
        client.player.getInventory().selectedSlot = oldSlot;
        lastPlaceTime = now;
    }

    private static int findHotbarBlock(MinecraftClient client, BlockState desired) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == desired.getBlock()) return i;
        }
        return -1;
    }

    private static BlockHitResult findPlacementHit(MinecraftClient client, BlockPos ghost) {
        for (Direction face : Direction.values()) {
            BlockPos support = ghost.offset(face.getOpposite());
            if (!client.world.isChunkLoaded(support.getX() >> 4, support.getZ() >> 4)) continue;
            if (client.world.getBlockState(support).isAir()) continue;
            return new BlockHitResult(support.toCenterPos(), face, support, false);
        }
        return null;
    }
}
