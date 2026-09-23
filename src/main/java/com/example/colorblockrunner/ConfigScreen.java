package com.example.colorblockrunner;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private static final int MIN_RANGE = 4, MAX_RANGE = 256, STEP = 4;
    public ConfigScreen(Screen parent) { super(Text.translatable("screen.colorblockrunner.title")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(toggleText("screen.colorblockrunner.enabled", Config.enabled), b -> {
            Config.enabled = !Config.enabled;
            Config.save();
            if (!Config.enabled && client != null) ColorBlockRunnerClient.resetTarget(client);
            clearAndInit();
        }).dimensions(cx - 100, height / 2 - 75, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.colorblockrunner.range_minus"), b -> {
            Config.range = Config.clamp(Config.range - STEP, MIN_RANGE, MAX_RANGE);
            Config.save(); clearAndInit();
        }).dimensions(cx - 100, height / 2 - 40, 40, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.colorblockrunner.range_plus"), b -> {
            Config.range = Config.clamp(Config.range + STEP, MIN_RANGE, MAX_RANGE);
            Config.save(); clearAndInit();
        }).dimensions(cx + 60, height / 2 - 40, 40, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleText("screen.colorblockrunner.auto_jump", Config.autoJump), b -> {
            Config.autoJump = !Config.autoJump; Config.save(); clearAndInit();
        }).dimensions(cx - 100, height / 2 - 5, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(toggleText("screen.colorblockrunner.auto_sprint", Config.autoSprint), b -> {
            Config.autoSprint = !Config.autoSprint; Config.save(); clearAndInit();
        }).dimensions(cx - 100, height / 2 + 30, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.colorblockrunner.close"), b -> close())
                .dimensions(cx - 100, height / 2 + 65, 200, 20).build());
    }

    private static Text toggleText(String key, boolean value) {
        return Text.translatable(key).append(value ? Text.translatable("screen.colorblockrunner.on") : Text.translatable("screen.colorblockrunner.off"));
    }

    @Override public void close() { if (client != null) client.setScreen(parent); }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int cx = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, cx, height / 2 - 112, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.colorblockrunner.range", Config.range), cx, height / 2 - 62, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.colorblockrunner.hint"), cx, height / 2 + 92, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }
}
