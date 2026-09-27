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

    /** Returns the dye color, with undyed/base blocks represented as WHITE for legacy callers. */
    public static DyeColor getDyeColor(Block block){
        Match match = match(block);
        return match == null ? null : match.color;
    }

    /**
     * Returns a key that distinguishes an undyed/base block from an actually white-dyed block.
     * This fixes, for example, plain terracotta being treated as white terracotta.
     */
    public static String getMatchKey(Block block){
        Match match = match(block);
        return match == null ? null : (match.base ? "base" : "dyed") + ":" + match.color.getName();
    }

    private static Match match(Block block){
        Identifier id=Registries.BLOCK.getId(block);
        String path=id.getPath().toLowerCase(Locale.ROOT);
        if(path.equals("wool")||path.equals("carpet")||path.equals("concrete")||path.equals("concrete_powder")||path.equals("terracotta")||path.equals("glass")||path.equals("glass_pane")||path.equals("candle"))
            return new Match(DyeColor.WHITE,true);
        for(int i=0;i<COLORS.length;i++) for(String family:FAMILIES)
            if(path.equals(COLORS[i]+"_"+family)) return new Match(DyeColor.byId(i),false);
        return null;
    }

    private record Match(DyeColor color, boolean base) {}
}
