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

    @Override public void onInitializeClient() {
        Config.load();
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.colorblockrunner.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.colorblockrunner"));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.colorblockrunner.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.colorblockrunner"));
        ClientTickEvents.END_CLIENT_TICK.register(ColorBlockRunnerClient::tick);
    }

    public static void tickExternal(MinecraftClient client) {
        UnifiedConfig u=UnifiedConfig.get();
        Config.enabled=u.runnerEnabled; Config.range=u.runnerRange; Config.autoJump=u.runnerJump; Config.autoSprint=u.runnerSprint;
        if(!u.runnerEnabled) { stop(client); return; }
        if(client.player==null || client.world==null || client.currentScreen!=null) return;
        if(target==null) findTarget(client);
        if(target==null){stopMovement(client);return;}
        if(isAtTarget(client)){target=null;stopMovement(client);return;}
        moveTowardTarget(client);
    }

    private static void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) { Config.enabled=!Config.enabled; Config.save(); if(Config.enabled) start(client); else stop(client); }
        while (configKey.wasPressed()) client.setScreen(new ConfigScreen(client.currentScreen));
        if(!Config.enabled || client.player==null || client.world==null || client.currentScreen!=null){if(!Config.enabled)stop(client);return;}
        if(target==null){findTarget(client);if(target==null){stopMovement(client);return;}}
        if(isAtTarget(client)){target=null;stopMovement(client);return;}
        moveTowardTarget(client);
    }
    private static void start(MinecraftClient client){target=null;stopMovement(client);if(client.player!=null&&client.world!=null)findTarget(client);}
    public static void resetTarget(MinecraftClient client){target=null;stopMovement(client);}
    private static void findTarget(MinecraftClient client){
        ItemStack held=client.player.getMainHandStack(); DyeColor heldColor=null;
        if(held.getItem() instanceof net.minecraft.item.BlockItem b) heldColor=ColorBlock.getDyeColor(b.getBlock());
        if(heldColor==null)return;
        BlockPos under=client.player.getBlockPos().down();
        if(ColorBlock.getDyeColor(client.world.getBlockState(under).getBlock())==heldColor)return;
        int cx0=under.getX(),cz0=under.getZ(),y=under.getY(),r=Config.clamp(Config.range,4,256); double best=Double.MAX_VALUE; BlockPos found=null;
        for(int x=cx0-r;x<=cx0+r;x++) for(int z=cz0-r;z<=cz0+r;z++){
            int dx=x-cx0,dz=z-cz0; double d=(double)dx*dx+(double)dz*dz; if(d==0||d>r*(double)r||d>=best)continue;
            int ccx=Math.floorDiv(x,16),ccz=Math.floorDiv(z,16); if(!client.world.getChunkManager().isChunkLoaded(ccx,ccz))continue;
            if(ColorBlock.getDyeColor(client.world.getBlockState(new BlockPos(x,y,z)).getBlock())==heldColor){best=d;found=new BlockPos(x,y,z);}
        }
        target=found;
    }
    private static boolean isAtTarget(MinecraftClient client){BlockPos p=client.player.getBlockPos().down();return target!=null&&p.equals(target);}
    private static void moveTowardTarget(MinecraftClient client){double tx=target.getX()+.5,tz=target.getZ()+.5;double dx=tx-client.player.getX(),dz=tz-client.player.getZ();client.player.setYaw((float)(Math.toDegrees(Math.atan2(dz,dx))-90));if(!controlling)saveInputState(client);client.options.forwardKey.setPressed(true);client.options.jumpKey.setPressed(Config.autoJump||savedJump);client.options.sprintKey.setPressed(Config.autoSprint||savedSprint);if(Config.autoSprint)client.player.setSprinting(true);controlling=true;}
    private static void saveInputState(MinecraftClient client){savedForward=client.options.forwardKey.isPressed();savedJump=client.options.jumpKey.isPressed();savedSprint=client.options.sprintKey.isPressed();}
    private static void stopMovement(MinecraftClient client){if(!controlling)return;client.options.forwardKey.setPressed(savedForward);client.options.jumpKey.setPressed(savedJump);client.options.sprintKey.setPressed(savedSprint);if(client.player!=null&&Config.autoSprint)client.player.setSprinting(savedSprint);controlling=false;}
    private static void stop(MinecraftClient client){target=null;stopMovement(client);}
}
