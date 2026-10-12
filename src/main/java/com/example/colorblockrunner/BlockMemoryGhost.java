package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BlockMemoryGhost {
    private static final Map<BlockPos, BlockState> remembered = new LinkedHashMap<>();
    private static final Set<BlockPos> ghosts = new LinkedHashSet<>();
    // A manually dismissed ghost stays hidden only until its original block is restored.
    private static final Set<BlockPos> cleared = new LinkedHashSet<>();
    private static int range = 16;
    private static boolean enabled;
    private static boolean autoPlace;
    private static long placeIntervalMs = 250L;
    private static long lastPlaceNanos;
    private static BlockPos lastAttemptedPos;

    private BlockMemoryGhost() {}

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static void setRange(int value) { range = Math.max(1, Math.min(64, value)); }
    public static int getRange() { return range; }
    public static void setAutoPlace(boolean value) { autoPlace = value; }
    public static boolean isAutoPlace() { return autoPlace; }
    public static void setPlaceInterval(long value) { placeIntervalMs = Math.max(1L, Math.min(2000L, value)); }
    public static long getPlaceInterval() { return placeIntervalMs; }

    public static void clearAll() {
        remembered.clear();
        ghosts.clear();
        cleared.clear();
        lastAttemptedPos = null;
    }

    public static void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return;

        BlockPos center = client.player.getBlockPos();
        int r = range;
        // Scan only loaded, nearby coordinates. Remembered positions survive after leaving this radius.
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int y = center.getY() - r; y <= center.getY() + r; y++) {
                for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (center.getSquaredDistance(pos) > (double) r * r) continue;
                    if (!client.world.isChunkLoaded(x >> 4, z >> 4)) continue;

                    BlockPos key = pos.toImmutable();
                    BlockState current = client.world.getBlockState(key);
                    BlockState original = remembered.get(key);

                    if (original == null) {
                        if (!current.isAir()) remembered.put(key, current);
                        continue;
                    }

                    if (current.isAir()) {
                        if (!cleared.contains(key)) ghosts.add(key);
                    } else if (current.equals(original)) {
                        // Restoring the exact original state re-arms this position for future breaks.
                        ghosts.remove(key);
                        cleared.remove(key);
                    } else {
                        // A different block at this position does not erase the original memory.
                        ghosts.remove(key);
                    }
                }
            }
        }

        if (autoPlace) tryAutoPlace(client);
    }

    public static boolean isGhost(MinecraftClient client, BlockPos pos) {
        if (client.world == null || pos == null || !ghosts.contains(pos) || !remembered.containsKey(pos)) return false;
        return client.world.getBlockState(pos).isAir();
    }

    public static boolean cancelAt(MinecraftClient client, BlockPos pos) {
        if (!isGhost(client, pos)) return false;
        BlockPos key = pos.toImmutable();
        ghosts.remove(key);
        cleared.add(key);
        if (key.equals(lastAttemptedPos)) lastAttemptedPos = null;
        return true;
    }

    /**
     * Ray-tests ghost block cubes because the vanilla crosshair raycast cannot hit client-only geometry.
     * Returns true only when the middle-click cancelled a ghost actually under the crosshair.
     */
    public static boolean cancelGhostUnderCrosshair(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null) return false;
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d end = start.add(client.player.getRotationVec(1.0F).multiply(6.0));
        BlockPos hitPos = null;
        double nearest = Double.MAX_VALUE;

        for (BlockPos pos : ghosts) {
            if (!isGhost(client, pos)) continue;
            Optional<Vec3d> hit = new Box(pos).expand(0.001).raycast(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.squaredDistanceTo(hit.get());
            if (distance < nearest) {
                nearest = distance;
                hitPos = pos;
            }
        }
        return hitPos != null && cancelAt(client, hitPos);
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.world == null) return;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null || context.camera() == null) return;

        double camX = context.camera().getPos().x;
        double camY = context.camera().getPos().y;
        double camZ = context.camera().getPos().z;
        BlockRenderManager renderer = client.getBlockRenderManager();

        for (BlockPos pos : ghosts) {
            BlockState state = remembered.get(pos);
            if (state == null || !isGhost(client, pos)) continue;
            matrices.push();
            matrices.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
            renderer.renderBlockAsEntity(state, matrices, consumers, 15728880, 0);
            matrices.pop();
        }
    }

    private static void tryAutoPlace(MinecraftClient client) {
        if (client.interactionManager == null || client.player.isUsingItem()) return;
        long now = System.nanoTime();
        long intervalNanos = placeIntervalMs * 1_000_000L;
        if (now - lastPlaceNanos < intervalNanos) return;

        BlockPos target = null;
        BlockState targetState = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : ghosts) {
            BlockState state = remembered.get(pos);
            if (state == null || !isGhost(client, pos)) continue;
            double distance = client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = pos;
                targetState = state;
            }
        }
        if (target == null || bestDistance > 64.0) return;

        int slot = findHotbarBlock(client, targetState);
        if (slot < 0) return;
        BlockHitResult hit = findPlacementHit(client, target);
        if (hit == null) return;

        // Avoid repeatedly hammering the same impossible placement every client tick.
        if (target.equals(lastAttemptedPos) && now - lastPlaceNanos < Math.max(intervalNanos, 100_000_000L)) return;

        int oldSlot = client.player.getInventory().selectedSlot;
        try {
            client.player.getInventory().selectedSlot = slot;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
            client.player.swingHand(Hand.MAIN_HAND);
            lastAttemptedPos = target.toImmutable();
            lastPlaceNanos = now;
        } finally {
            client.player.getInventory().selectedSlot = oldSlot;
        }
    }

    private static int findHotbarBlock(MinecraftClient client, BlockState desired) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() == desired.getBlock()) return i;
        }
        return -1;
    }

    private static BlockHitResult findPlacementHit(MinecraftClient client, BlockPos ghost) {
        for (Direction face : Direction.values()) {
            BlockPos support = ghost.offset(face.getOpposite());
            if (!client.world.isChunkLoaded(support.getX() >> 4, support.getZ() >> 4)) continue;
            if (client.world.getBlockState(support).isAir()) continue;

            // Hit the centre of the support face that points into the missing block.
            Vec3d hitPoint = support.toCenterPos().add(
                    face.getOffsetX() * 0.5,
                    face.getOffsetY() * 0.5,
                    face.getOffsetZ() * 0.5);
            return new BlockHitResult(hitPoint, face, support, false);
        }
        return null;
    }
}