package com.example.colorblockrunner;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArrowDodgePredictive {
    private static final double GRAVITY=0.05, DRAG=0.99, BOW_SPEED=3.0, DODGE_SPEED=0.30, MARGIN=0.08;
    private static final int MAX_TICKS=100;
    private static Vec3d direction=Vec3d.ZERO;
    private static List<Vec3d> path=List.of();
    private static int moveTicks;
    private static boolean moving, sf,sb,sl,sr;
    private ArrowDodgePredictive(){}
    private static boolean finite(Vec3d v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    public static void tick(MinecraftClient c){try{safeTick(c);}catch(Throwable ignored){path=List.of();stop(c);}}
    private static void safeTick(MinecraftClient c){
        UnifiedConfig cfg=UnifiedConfig.get();
        if(!cfg.dodgeEnabled||c.player==null||c.world==null||c.currentScreen!=null){path=List.of();stop(c);return;}
        if(moveTicks>0){move(c);if(--moveTicks<=0)stop(c);}
        ArrowEntity a=c.world.getEntitiesByClass(ArrowEntity.class,c.player.getBoundingBox().expand(Math.max(1,cfg.dodgeRange)),e->e!=null&&!e.isRemoved()&&e.isAlive()&&finite(e.getVelocity())&&(!cfg.ignoreOwnArrows||e.getOwner()!=c.player)).stream().min(Comparator.comparingDouble(e->e.squaredDistanceTo(c.player))).orElse(null);
        if(a!=null){path=simulate(c,a.getPos(),a.getVelocity());if(moveTicks<=0){int hit=hitTick(c,path);if(hit>0){Vec3d d=findDodge(c,path,hit);if(d!=null)start(c,d);}}return;}
        path=List.of();if(c.player.isUsingItem()&&c.player.getActiveItem().isOf(Items.BOW))path=bow(c);
    }
    private static List<Vec3d> bow(MinecraftClient c){ItemStack s=c.player.getActiveItem();int u=s.getMaxUseTime()-c.player.getItemUseTimeLeft();float p=Math.min(1f,u/20f);p=(p*p+p*2f)/3f;if(p<.1f)return List.of();return simulate(c,c.player.getEyePos(),c.player.getRotationVector().multiply(BOW_SPEED*p));}
    private static List<Vec3d> simulate(MinecraftClient c,Vec3d p,Vec3d v){List<Vec3d> out=new ArrayList<>();out.add(p);double r=Math.max(1,UnifiedConfig.get().dodgeRange),r2=r*r;for(int i=0;i<MAX_TICKS;i++){if(!finite(p)||!finite(v))break;Vec3d n=p.add(v);if(!finite(n))break;if(n.squaredDistanceTo(c.player.getPos())>r2&&p.squaredDistanceTo(c.player.getPos())>r2)break;BlockHitResult h=c.world.raycast(new RaycastContext(p,n,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,c.player));out.add(h.getType()==HitResult.Type.MISS?n:h.getPos());if(h.getType()!=HitResult.Type.MISS)break;p=n;v=v.multiply(DRAG).subtract(0,GRAVITY,0);if(p.y<c.world.getBottomY()-2)break;}return out;}
    private static int hitTick(MinecraftClient c,List<Vec3d> p){Box b=c.player.getBoundingBox().expand(MARGIN);for(int i=1;i<p.size();i++)if(finite(p.get(i-1))&&finite(p.get(i))&&b.raycast(p.get(i-1),p.get(i)).isPresent())return i;return -1;}
    private static Vec3d findDodge(MinecraftClient c,List<Vec3d> p,int hit){Vec3d[] ds=dirs(c);for(Vec3d d:ds){Vec3d x=distance(c,p,hit,d);if(x!=null)return x;}Vec3d[] xs={ds[0].add(ds[2]).normalize(),ds[0].add(ds[3]).normalize(),ds[1].add(ds[2]).normalize(),ds[1].add(ds[3]).normalize()};for(Vec3d d:xs){Vec3d x=distance(c,p,hit,d);if(x!=null)return x;}return null;}
    private static Vec3d distance(MinecraftClient c,List<Vec3d> p,int hit,Vec3d d){for(double dist=.25;dist<=4;dist+=.25){int ticks=Math.max(1,(int)Math.ceil(dist/DODGE_SPEED));if(ticks>hit)continue;Vec3d off=d.multiply(dist);if(safe(c,p,off,ticks))return off;}return null;}
    private static boolean safe(MinecraftClient c,List<Vec3d> p,Vec3d off,int ticks){Box base=c.player.getBoundingBox();int n=Math.min(p.size()-1,Math.max(1,ticks));for(int t=1;t<=n;t++){Box b=base.offset(off.multiply(Math.min(1,t/(double)ticks))).expand(MARGIN);if(p.get(t-1)!=null&&p.get(t)!=null&&b.raycast(p.get(t-1),p.get(t)).isPresent())return false;if(c.world.getBlockCollisions(c.player,b).iterator().hasNext())return false;}Box end=base.offset(off).expand(MARGIN);for(int t=Math.max(1,n+1);t<p.size();t++)if(end.raycast(p.get(t-1),p.get(t)).isPresent())return false;return !c.world.getBlockCollisions(c.player,end).iterator().hasNext();}
    private static Vec3d[] dirs(MinecraftClient c){double y=Math.toRadians(c.player.getYaw());Vec3d f=new Vec3d(-Math.sin(y),0,Math.cos(y)),r=new Vec3d(Math.cos(y),0,Math.sin(y));return new Vec3d[]{r.multiply(-1),r,f.multiply(-1),f};}
    private static void start(MinecraftClient c,Vec3d d){if(!moving){sf=c.options.forwardKey.isPressed();sb=c.options.backKey.isPressed();sl=c.options.leftKey.isPressed();sr=c.options.rightKey.isPressed();}direction=d.normalize();moveTicks=Math.max(1,(int)Math.ceil(d.length()/DODGE_SPEED));moving=true;}
    private static void move(MinecraftClient c){if(!moving||c.player==null)return;double y=Math.toRadians(c.player.getYaw());Vec3d f=new Vec3d(-Math.sin(y),0,Math.cos(y)),r=new Vec3d(Math.cos(y),0,Math.sin(y));double fd=direction.dotProduct(f),rd=direction.dotProduct(r);c.options.forwardKey.setPressed(fd>.25||sf);c.options.backKey.setPressed(fd<-.25||sb);c.options.leftKey.setPressed(rd<-.25||sl);c.options.rightKey.setPressed(rd>.25||sr);Vec3d v=c.player.getVelocity(),h=direction.multiply(DODGE_SPEED);c.player.setVelocity(h.x,v.y,h.z);}
    private static void stop(MinecraftClient c){if(!moving)return;c.options.forwardKey.setPressed(sf);c.options.backKey.setPressed(sb);c.options.leftKey.setPressed(sl);c.options.rightKey.setPressed(sr);moving=false;moveTicks=0;direction=Vec3d.ZERO;}
    public static void render(WorldRenderContext ctx){try{MinecraftClient c=MinecraftClient.getInstance();UnifiedConfig cfg=UnifiedConfig.get();if(!cfg.dodgeEnabled||!cfg.showLanding||path.size()<2||c.player==null)return;VertexConsumerProvider p=ctx.consumers();if(p==null||ctx.matrixStack()==null)return;VertexConsumer b=p.getBuffer(RenderLayer.getLines());Matrix4f m=ctx.matrixStack().peek().getPositionMatrix();Vec3d cam=ctx.camera().getPos();for(int i=1;i<path.size();i++){Vec3d a=path.get(i-1).subtract(cam),d=path.get(i).subtract(cam);if(!finite(a)||!finite(d))continue;b.vertex(m,(float)a.x,(float)a.y,(float)a.z).color(255,90,90,220).normal(0,1,0).next();b.vertex(m,(float)d.x,(float)d.y,(float)d.z).color(255,180,90,220).normal(0,1,0).next();}}catch(Throwable ignored){path=List.of();}}
}
