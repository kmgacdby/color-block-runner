package com.example.colorblockrunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("colorblockrunner.json");
    public static boolean enabled = false;
    public static int range = 32;
    public static boolean autoJump = true;
    public static boolean autoSprint = true;
    private Config() {}
    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Data data = GSON.fromJson(Files.readString(FILE), Data.class);
            if (data != null) {
                enabled = data.enabled;
                range = clamp(data.range, 4, 256);
                autoJump = data.autoJump;
                autoSprint = data.autoSprint;
            }
        } catch (Exception ignored) {}
    }
    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(new Data(enabled, range, autoJump, autoSprint))); }
        catch (IOException ignored) {}
    }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private record Data(boolean enabled, int range, boolean autoJump, boolean autoSprint) {}
}
