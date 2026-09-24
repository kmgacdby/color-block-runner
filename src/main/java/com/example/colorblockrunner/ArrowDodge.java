package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArrowDodge {
    private static final double GRAVITY = 0.05;
    private static final double DRAG = 0.99;
    private static final double ARROW_SPEED = 3.0;
    private static final int MAX_SIM_TICKS = 100;
    private static Vec3d dangerPoint;
    private static List<Vec3d> trajectory = List.of();
    private static Vec3d dodgeDirection = Vec3d.ZERO;
    private static int dodgeTicks;
    private static boolean controlling;
    private static boolean savedForward, savedBack, savedLeft, savedRight;

    private ArrowDodge() {}

    public static void tick(MinecraftClient client) {
        UnifiedConfig c = UnifiedConfig.get();
        dangerPoint = null;
        trajectory = List.of();
        if (!c.dodgeEnabled || client.player == null || client.world == null || client.currentScreen != null) {
            stopMovement(client);
            return;
        }

        ArrowEntity arrow = client.world.getEntitiesByClass(
                ArrowEntity.class,
                client.player.getBoundingBox().expand(c.dodgeRange),
                a -> !c.ignoreOwnArrows || a.getOwner() != client.player)
                .stream()
                .min(Comparator.comparingDouble(a -> a.squaredDistanceTo(client.player)))
                .orElse(null);

        if (dodgeTicks > 0) {
            applyDodgeMovement(client);
            if (--dodgeTicks <= 0) stopMovement(client);
            return;
        }

        if (arrow == null) {
            stopMovement(client);
            if (client.player.isUsingItem() && client.player.getActiveItem().isOf(Items.BOW)) trajectory = simulateBow(client);
            return;
        }

        trajectory = simulatePath(client, arrow.getPos(), arrow.getVelocity());
        if (!willHitPlayer(client, trajectory)) {
            stopMovement(client);
            return;
        }

        Vec3d dodge = findMinimumDodge(client, trajectory);
        if (dodge != null) startDodge(client, dodge);
    }

    private static List<Vec3d> simulateBow(MinecraftClient client) {
        ItemStack bow = client.player.getActiveItem();
        int useTicks = bow.getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
        float pull = Math.min(1.0f, useTicks / 20.0f);
        pull = (pull * pull + pull * 2.0f) / 3.0f;
        if (pull < 0.1f) return List.of();
        return simulatePath(client, client.player.getEyePos(), client.player.getRotationVector().multiply(ARROW_SPEED * pull));
    }

    private static List<Vec3d> simulatePath(MinecraftClient client, Vec3d start, Vec3d velocity) {
        List<Vec3d> points = new ArrayList<>();
        Vec3d p = start, v = velocity;
        points.add(p);
        double maxSq = UnifiedConfig.get().dodgeRange * (double) UnifiedConfig.get().dodgeRange;
        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            Vec3d next = p.add(v);
            if (next.squaredDistanceTo(client.player.getPos()) > maxSq && p.squaredDistanceTo(client.player.getPos()) > maxSq) break;
            BlockHitResult hit = client.world.raycast(new RaycastContext(p, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getPos());
                break;
            }
            points.add(next);
            p = next;
            v = v.multiply(DRAG).subtract(0, GRAVITY, 0);
            if (p.y < client.world.getBottomY() - 2) break;
        }
        return points;
    }

    private static boolean willHitPlayer(MinecraftClient client, List<Vec3d> path) {
        if (path.size() < 2) return false;
        Box box = client.player.getBoundingBox().expand(0.12);
        for (int i = 1; i < path.size(); i++) {
            var hit = box.raycast(path.get(i - 1), path.get(i));
            if (hit.isPresent()) {
                dangerPoint = hit.get();
                return true;
            }
        }
        return false;
    }

    private static Vec3d findMinimumDodge(MinecraftClient client, List<Vec3d> path) {
        Vec3d[] dirs = cardinalDirections(client);
        for (int index = 0; index < 4; index++) {
            for (double distance = 0.5; distance <= 4.0; distance += 0.5) {
                Vec3d displacement = dirs[index].multiply(distance);
                if (clearsPath(client, path, displacement)) return displacement;
            }
        }
        Vec3d[] diagonals = {
                dirs[0].add(dirs[2]).normalize(), dirs[0].add(dirs[3]).normalize(),
                dirs[1].add(dirs[2]).normalize(), dirs[1].add(dirs[3]).normalize()
        };
        for (Vec3d dir : diagonals) {
            for (double distance = 0.5; distance <= 4.0; distance += 0.5) {
                Vec3d displacement = dir.multiply(distance);
                if (clearsPath(client, path, displacement)) return displacement;
            }
        }
        return null;
    }

    private static boolean clearsPath(MinecraftClient client, List<Vec3d> path, Vec3d offset) {
        Box box = client.player.getBoundingBox().offset(offset).expand(0.12);
        for (int i = 1; i < path.size(); i++) if (box.raycast(path.get(i - 1), path.get(i)).isPresent()) return false;
        return !client.world.getBlockCollisions(client.player, box).iterator().hasNext();
    }

    private static Vec3d[] cardinalDirections(MinecraftClient client) {
        double yaw = Math.toRadians(client.player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();
        Vec3d right = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw)).normalize();
        return new Vec3d[]{right.multiply(-1), right, forward.multiply(-1), forward};
    }

    private static void startDodge(MinecraftClient client, Vec3d displacement) {
        if (!controlling) {
            savedForward = client.options.forwardKey.isPressed(); savedBack = client.options.backKey.isPressed();
            savedLeft = client.options.leftKey.isPressed(); savedRight = client.options.rightKey.isPressed();
        }
        dodgeDirection = displacement.normalize();
        dodgeTicks = Math.max(2, Math.min(12, (int) Math.ceil(displacement.length() / 0.22)));
        controlling = true;
        applyDodgeMovement(client);
    }

    private static void applyDodgeMovement(MinecraftClient client) {
        if (!controlling) return;
        double yaw = Math.toRadians(client.player.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3d right = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
        double f = dodgeDirection.dotProduct(forward), r = dodgeDirection.dotProduct(right);
        client.options.forwardKey.setPressed(f > 0.25 || savedForward);
        client.options.backKey.setPressed(f < -0.25 || savedBack);
        client.options.rightKey.setPressed(r > 0.25 || savedRight);
        client.options.leftKey.setPressed(r < -0.25 || savedLeft);
    }

    private static void stopMovement(MinecraftClient client) {
        if (!controlling) return;
        client.options.forwardKey.setPressed(savedForward); client.options.backKey.setPressed(savedBack);
        client.options.leftKey.setPressed(savedLeft); client.options.rightKey.setPressed(savedRight);
        controlling = false; dodgeTicks = 0; dodgeDirection = Vec3d.ZERO;
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.dodgeEnabled || !c.showLanding || trajectory.size() < 2 || client.player == null) return;
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
        Matrix3f normal = context.matrixStack().peek().getNormalMatrix();
        Vec3d camera = context.camera().getPos();
        for (int i = 1; i < trajectory.size(); i++) {
            Vec3d a = trajectory.get(i - 1).subtract(camera), b = trajectory.get(i).subtract(camera);
            buffer.vertex(matrix, (float)a.x, (float)a.y, (float)a.z).color(255, 90, 90, 220).normal(normal, 0, 1, 0).next();
            buffer.vertex(matrix, (float)b.x, (float)b.y, (float)b.z).color(255, 180, 90, 220).normal(normal, 0, 1, 0).next();
        }
    }
}
