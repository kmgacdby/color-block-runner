package com.example.colorblockrunner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;

public final class ArrowDodge {
    private static Vec3d dangerPoint;
    private ArrowDodge() {}
    public static Vec3d dangerPoint(){ return dangerPoint; }

    public static void tick(MinecraftClient client){
        UnifiedConfig c=UnifiedConfig.get();
        dangerPoint=null;
        if(!c.dodgeEnabled || client.player==null || client.world==null || client.currentScreen!=null) return;
        ArrowEntity arrow=client.world.getEntitiesByClass(ArrowEntity.class, client.player.getBoundingBox().expand(c.dodgeRange), a->!c.ignoreOwnArrows || a.getOwner()!=client.player)
                .stream().filter(a->isThreat(client,a)).min(Comparator.comparingDouble(a->a.squaredDistanceTo(client.player))).orElse(null);
        if(arrow==null) return;
        Vec3d p=client.player.getPos(), v=arrow.getVelocity();
        double speed=v.length();
        if(speed<0.08) return;
        double t=0.0, best=Double.MAX_VALUE;
        for(double s=0.05;s<=1.5;s+=0.05){
            Vec3d q=arrow.getPos().add(v.multiply(s));
            double d=horizontalDistanceSq(q,p);
            if(d<best){best=d;t=s;}
            if(d < 1.2){ dangerPoint=q; break; }
        }
        if(best>=1.2) return;
        dangerPoint=arrow.getPos().add(v.multiply(t));
        Vec3d side=new Vec3d(-v.z,0,v.x).normalize();
        double left=safeScore(client,side), right=safeScore(client,side.multiply(-1));
        Vec3d dir=left>=right?side:side.multiply(-1);
        client.player.setYaw((float)Math.toDegrees(Math.atan2(-dir.x,dir.z)));
        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);
        if(c.dodgeJump) client.options.jumpKey.setPressed(true);
        else if(c.dodgeSneak) client.options.sneakKey.setPressed(true);
    }
    private static boolean isThreat(MinecraftClient c, ArrowEntity a){
        Vec3d v=a.getVelocity(); if(v.lengthSquared()<0.0064) return false;
        Vec3d rel=c.player.getPos().subtract(a.getPos());
        return v.normalize().dotProduct(rel.normalize())>0.55;
    }
    private static double horizontalDistanceSq(Vec3d a,Vec3d b){ double x=a.x-b.x,z=a.z-b.z; return x*x+z*z; }
    private static double safeScore(MinecraftClient c,Vec3d dir){
        Vec3d p=c.player.getPos().add(dir.multiply(1.8));
        Box b=c.player.getBoundingBox().offset(p.subtract(c.player.getPos()));
        return c.world.getBlockCollisions(c.player,b).iterator().hasNext()?-100:horizontalDistanceSq(p,dangerPoint==null?p:dangerPoint);
    }
}
