package com.example.colorblockrunner;

import net.minecraft.block.Block;
import net.minecraft.util.DyeColor;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.Locale;

public final class ColorBlock {
    private static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };
    private static final String[] FAMILIES = {
            "wool", "carpet", "concrete", "concrete_powder", "terracotta", "glazed_terracotta",
            "stained_glass", "stained_glass_pane", "candle", "bed", "banner", "shulker_box"
    };
    private ColorBlock() {}
    public static DyeColor getDyeColor(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (int i = 0; i < COLORS.length; i++) {
            for (String family : FAMILIES) {
                if (path.equals(COLORS[i] + "_" + family)) return DyeColor.byId(i);
            }
        }
        return null;
    }
}
