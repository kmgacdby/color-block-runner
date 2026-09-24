package com.example.colorblockrunner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.slot.Slot;

public final class StewMover {
    private static int cooldownTicks;
    private StewMover() {}

    public static void tickExternal(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> screen) || client.player == null || client.interactionManager == null) {
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        int hotbarEmpty = -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isEmpty()) {
                hotbarEmpty = i;
                break;
            }
        }
        if (hotbarEmpty < 0) return;

        int source = -1;
        int target = -1;
        for (int s = 0; s < screen.getScreenHandler().slots.size(); s++) {
            Slot slot = screen.getScreenHandler().slots.get(s);
            if (slot.inventory != client.player.getInventory()) continue;
            if (slot.getIndex() == hotbarEmpty) target = s;
            if (slot.getIndex() >= 9 && slot.getIndex() < 36 && slot.getStack().isOf(Items.MUSHROOM_STEW)) source = s;
        }

        if (source >= 0 && target >= 0) {
            client.interactionManager.clickSlot(screen.getScreenHandler().syncId, source, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(screen.getScreenHandler().syncId, target, 0, SlotActionType.PICKUP, client.player);
            cooldownTicks = Math.max(0, UnifiedConfig.get().stewDelay);
        }
    }
}
