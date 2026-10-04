package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Меню, которое открывается на Right Shift. */
public class VisualsScreen extends Screen {
    private static final String[] PARTICLE_NAMES = HitVisualsClient.PARTICLE_NAMES;

    public VisualsScreen() {
        super(Text.literal("Hit Visuals"));
    }

    @Override
    protected void init() {
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 82;
        int step = 24;

        addDrawableChild(ButtonWidget.builder(onOff("Эффекты при ударе", VisualsConfig.hitEffects), b -> {
            VisualsConfig.hitEffects = !VisualsConfig.hitEffects;
            b.setMessage(onOff("Эффекты при ударе", VisualsConfig.hitEffects));
        }).dimensions(x, y, w, 20).build());
        y += step;

        addDrawableChild(ButtonWidget.builder(particleLabel(), b -> {
            VisualsConfig.particle = (VisualsConfig.particle + 1) % PARTICLE_NAMES.length;
            b.setMessage(particleLabel());
        }).dimensions(x, y, w, 20).build());
        y += step;

        addDrawableChild(new SliderWidget(x, y, w, 20, countLabel(VisualsConfig.particleCount),
                (VisualsConfig.particleCount - 1) / 39.0) {
            @Override
            protected void updateMessage() {
                setMessage(countLabel(1 + (int) Math.round(this.value * 39)));
            }

            @Override
            protected void applyValue() {
                VisualsConfig.particleCount = 1 + (int) Math.round(this.value * 39);
            }
        });
        y += step;

        addDrawableChild(ButtonWidget.builder(onOff("Вспышка экрана", VisualsConfig.flash), b -> {
            VisualsConfig.flash = !VisualsConfig.flash;
            b.setMessage(onOff("Вспышка экрана", VisualsConfig.flash));
        }).dimensions(x, y, w, 20).build());
        y += step;

        addDrawableChild(ButtonWidget.builder(onOff("Звук удара", VisualsConfig.sound), b -> {
            VisualsConfig.sound = !VisualsConfig.sound;
            b.setMessage(onOff("Звук удара", VisualsConfig.sound));
        }).dimensions(x, y, w, 20).build());
        y += step;

        addDrawableChild(ButtonWidget.builder(skyLabel(), b -> {
            VisualsConfig.sky = (VisualsConfig.sky + 1) % SkyPresets.NAMES.length;
            b.setMessage(skyLabel());
        }).dimensions(x, y, w, 20).build());
        y += step + 6;

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(x, y, w, 20).build());
    }

    private static Text onOff(String name, boolean value) {
        return Text.literal(name + ": " + (value ? "§aВкл" : "§cВыкл"));
    }

    private static Text particleLabel() {
        return Text.literal("Частицы: " + PARTICLE_NAMES[VisualsConfig.particle]);
    }

    private static Text countLabel(int n) {
        return Text.literal("Количество частиц: " + n);
    }

    private static Text skyLabel() {
        return Text.literal("Небо: " + SkyPresets.NAMES[VisualsConfig.sky]);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 105, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        VisualsConfig.save();
        super.close();
    }
}
