package com.example.colorblockrunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UnifiedConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("utility-suite.json");
    public boolean stewEnabled=false, runnerEnabled=false, dodgeEnabled=false;
    public int stewDelay=3, runnerRange=32, dodgeRange=24;
    public boolean runnerJump=true, runnerSprint=true;
    public boolean dodgeJump=true, dodgeSneak=false, ignoreOwnArrows=true, showLanding=true;
    public String stewKey="H", runnerKey="J", dodgeKey="K", menuKey="O";
    private static UnifiedConfig instance;
    public static UnifiedConfig get(){ if(instance==null) load(); return instance; }
    public static void load(){ try{ if(Files.exists(FILE)) instance=GSON.fromJson(Files.readString(FILE),UnifiedConfig.class); }catch(Exception ignored){} if(instance==null)instance=new UnifiedConfig(); }
    public void save(){ try{Files.writeString(FILE,GSON.toJson(this));}catch(IOException ignored){}}
}
