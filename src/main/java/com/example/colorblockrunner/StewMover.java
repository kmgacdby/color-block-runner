package com.example.colorblockrunner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class StewMover {
    private static long nextMove;
    private StewMover(){}
    public static void tickExternal(MinecraftClient client){
        if(!(client.currentScreen instanceof HandledScreen<?> screen) || client.player==null) return;
        if(System.currentTimeMillis()<nextMove) return;
        int hotbarEmpty=-1;
        for(int i=0;i<9;i++) if(client.player.getInventory().getStack(i).isEmpty()){hotbarEmpty=i;break;}
        if(hotbarEmpty<0) return;
        for(int i=0;i<client.player.getInventory().size();i++){
            if(i<9) continue;
            if(client.player.getInventory().getStack(i).isOf(Items.MUSHROOM_STEW)){
                int handlerSlot=screen.getScreenHandler().getSlotIndex(client.player.getInventory(),i);
                if(handlerSlot>=0){
                    client.interactionManager.clickSlot(screen.getScreenHandler().syncId,handlerSlot,0,SlotActionType.PICKUP,client.player);
                    int targetSlot=screen.getScreenHandler().getSlotIndex(client.player.getInventory(),hotbarEmpty);
                    if(targetSlot>=0) client.interactionManager.clickSlot(screen.getScreenHandler().syncId,targetSlot,0,SlotActionType.PICKUP,client.player);
                    nextMove=System.currentTimeMillis()+Math.max(0,UnifiedConfig.get().stewDelay)*50L;
                }
                return;
            }
        }
    }
}
