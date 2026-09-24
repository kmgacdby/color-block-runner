package com.example.colorblockrunner;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import java.util.Locale;

public final class ColorBlock {
    private static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
    private static final String[] FAMILIES={"wool","carpet","concrete","concrete_powder","terracotta","glazed_terracotta","stained_glass","stained_glass_pane","candle","bed","banner","shulker_box"};
    private ColorBlock(){}
    public static DyeColor getDyeColor(Block block){
        Identifier id=Registries.BLOCK.getId(block);String path=id.getPath().toLowerCase(Locale.ROOT);
        // Vanilla has base/uncolored variants for wool-like and glass/terracotta families; treat them as white.
        if(path.equals("wool")||path.equals("carpet")||path.equals("concrete")||path.equals("concrete_powder")||path.equals("terracotta")||path.equals("glass")||path.equals("glass_pane")||path.equals("candle"))return DyeColor.WHITE;
        for(int i=0;i<COLORS.length;i++)for(String family:FAMILIES)if(path.equals(COLORS[i]+"_"+family))return DyeColor.byId(i);
        return null;
    }
}
