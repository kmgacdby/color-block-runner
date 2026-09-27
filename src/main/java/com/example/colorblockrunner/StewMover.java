package com.example.colorblockrunner;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Port of the user's verified standalone mushroom-stew mover. */
public final class StewMover {
    private static long nextMoveAt;
    private StewMover() {}

    public static void tickExternal(MinecraftClient client) {
        UnifiedConfig c = UnifiedConfig.get();
        if (!c.stewEnabled || client.player == null || client.interactionManager == null
                || !(client.currentScreen instanceof InventoryScreen inventoryScreen)) {
            resetTimer();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextMoveAt) return;
        var handler = inventoryScreen.getScreenHandler();
        int source = -1;
        for (int slotId = 9; slotId <= 35; slotId++) {
            if (handler.getSlot(slotId).getStack().isOf(Items.MUSHROOM_STEW)) { source = slotId; break; }
        }
        int target = -1;
        for (int slotId = 36; slotId <= 44; slotId++) {
            if (handler.getSlot(slotId).getStack().isEmpty()) { target = slotId; break; }
        }
        if (source < 0 || target < 0) return;
        client.interactionManager.clickSlot(handler.syncId, source, target - 36, SlotActionType.SWAP, client.player);
        nextMoveAt = now + Math.max(50, c.stewDelay);
    }

    public static void resetTimer() { nextMoveAt = 0L; }
}
