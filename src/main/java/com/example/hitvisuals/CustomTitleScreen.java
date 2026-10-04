package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Своё главное меню вместо стандартного. */
public class CustomTitleScreen extends Screen {
    private static final String[] LABELS = {
            "Одиночная игра", "Сетевая игра", "Настройки", "Меню Hit Visuals", "Выйти из игры"
    };
    private static final int BW = 230;
    private static final int BH = 26;
    private static final int BG = 7;
    private static final int TITLE_BLOCK = 78;

    public CustomTitleScreen() {
        super(Text.literal("Hit Visuals"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private int startY() {
        int total = TITLE_BLOCK + LABELS.length * (BH + BG);
        return Math.max(8, (this.height - total) / 2);
    }

    private int buttonY(int i) {
        return startY() + TITLE_BLOCK + i * (BH + BG);
    }

    private static long hash(int i) {
        long h = (i + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h & Long.MAX_VALUE;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = Ui.accent();

        // фон
        ctx.fillGradient(0, 0, this.width, this.height, 0xFF06070B,
                Ui.argb(0xFF, Ui.blend(0x06070B, acc, 0.22f)));

        // летающие значки: звёзды, луны, черепа
        long t = System.currentTimeMillis();
        for (int i = 0; i < 26; i++) {
            long h = hash(i);
            double fx = (h & 1023) / 1023.0;
            double speed = 0.012 + (((h >> 10) & 255) / 255.0) * 0.03;
            double off = ((h >> 18) & 1023) / 1023.0;
            int type = (int) ((h >> 28) % 3);
            int size = 2 + (int) ((h >> 30) & 1);
            double travel = this.height + 80.0;
            double ph = (((t % 1000000L) * speed) + off * travel) % travel;
            int y = (int) (this.height + 40 - ph);
            int x = (int) (fx * this.width + Math.sin(t * 0.001 + i) * 18);
            int alpha = 38 + (int) (30 * (0.5 + 0.5 * Math.sin(t * 0.002 + i * 1.7)));
            Sprites.draw(ctx, type, x, y, size, Ui.argb(alpha, i % 4 == 0 ? 0xFFFFFF : acc));
        }

        // заголовок
        String title = "HIT VISUALS";
        float sc = 5f;
        float tw = this.textRenderer.getWidth(title) * sc;
        float tx = this.width / 2f - tw / 2f;
        float ty = startY();
        Ui.scaled(ctx, this.textRenderer, title, tx + 3, ty + 3, Ui.argb(0xFF, acc), sc, false);
        Ui.scaled(ctx, this.textRenderer, title, tx, ty, 0xFFFFFFFF, sc, false);
        String sub = "клиент для Minecraft 1.21.4";
        float sw = this.textRenderer.getWidth(sub);
        Ui.scaled(ctx, this.textRenderer, sub, this.width / 2f - sw / 2f, ty + 8 * sc + 6, 0xFF8A90A0, 1f, false);
        ctx.fill(this.width / 2 - 30, (int) (ty + 8 * sc + 20), this.width / 2 + 30, (int) (ty + 8 * sc + 22),
                Ui.argb(0xFF, acc));

        // кнопки
        int bx = this.width / 2 - BW / 2;
        for (int i = 0; i < LABELS.length; i++) {
            int by = buttonY(i);
            boolean hov = mouseX >= bx && mouseX < bx + BW && mouseY >= by && mouseY < by + BH;
            if (hov) {
                Ui.rrect(ctx, bx - 1, by - 1, BW + 2, BH + 2, 5, Ui.argb(0xAA, acc));
            }
            Ui.rrect(ctx, bx, by, BW, BH, 4,
                    hov ? Ui.argb(0xFF, Ui.blend(0x14161C, acc, 0.35f)) : 0xE0141620);
            Ui.rrect(ctx, bx, by + 4, 3, BH - 8, 1, hov ? Ui.argb(0xFF, acc) : Ui.argb(0x66, acc));
            int lw = this.textRenderer.getWidth(LABELS[i]);
            ctx.drawText(this.textRenderer, LABELS[i], bx + (BW - lw) / 2, by + (BH - 8) / 2 + 1, 0xFFFFFFFF, true);
        }

        String hint = "Right Shift - меню Hit Visuals";
        int hw = this.textRenderer.getWidth(hint);
        ctx.drawText(this.textRenderer, hint, this.width / 2 - hw / 2, this.height - 14, 0xFF6C7280, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx = this.width / 2 - BW / 2;
            for (int i = 0; i < LABELS.length; i++) {
                int by = buttonY(i);
                if (mouseX >= bx && mouseX < bx + BW && mouseY >= by && mouseY < by + BH) {
                    activate(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void activate(int i) {
        if (this.client == null) return;
        switch (i) {
            case 0 -> this.client.setScreen(new SelectWorldScreen(this));
            case 1 -> this.client.setScreen(new MultiplayerScreen(this));
            case 2 -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case 3 -> this.client.setScreen(new VisualsScreen(this));
            default -> this.client.scheduleStop();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && this.client != null) {
            this.client.setScreen(new VisualsScreen(this));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
