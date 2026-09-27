package com.example.colorblockrunner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class StewMover {
    private static long nextMove;
    private StewMover(){}
    public static void tickExternal(MinecraftClient client){
        if(!(client.currentScreen instanceof HandledScreen<?> screen) || client.player==null || client.interactionManager==null) return;
        if(System.currentTimeMillis()<nextMove) return;
        int hotbarEmpty=-1;
        for(int i=0;i<9;i++) if(client.player.getInventory().getStack(i).isEmpty()){hotbarEmpty=i;break;}
        if(hotbarEmpty<0) return;
        for(int i=9;i<client.player.getInventory().size();i++){
            if(!client.player.getInventory().getStack(i).isOf(Items.MUSHROOM_STEW)) continue;
            var sourceSlot=screen.getScreenHandler().getSlotIndex(client.player.getInventory(),i);
            if(sourceSlot.isEmpty()) return;
            var targetSlot=screen.getScreenHandler().getSlotIndex(client.player.getInventory(),hotbarEmpty);
            if(targetSlot.isEmpty()) return;
            client.interactionManager.clickSlot(screen.getScreenHandler().syncId,sourceSlot.getAsInt(),0,SlotActionType.PICKUP,client.player);
            client.interactionManager.clickSlot(screen.getScreenHandler().syncId,targetSlot.getAsInt(),0,SlotActionType.PICKUP,client.player);
            nextMove=System.currentTimeMillis()+Math.max(0,UnifiedConfig.get().stewDelay)*50L;
            return;
        }
    }
}
