package com.example.hitvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Своё главное меню HamsterVisuals вместо стандартного. */
public class CustomTitleScreen extends Screen {
    private static final String TITLE = "HAMSTERVISUALS";
    private static final String[] LABELS = {
            "Одиночная игра", "Сетевая игра", "Настройки", "Меню HamsterVisuals", "Выйти из игры"
    };
    private static final String[] PHRASES = {
            "клиент для Minecraft 1.21.4",
            "хомяки правят миром",
            "частицы, музыка и метки в одном моде",
            "Right Shift открывает меню в игре"
    };
    private static final int[] SWATCHES = {
            0xFF3B5C, 0xFF8A3D, 0xFFD23F, 0x3DFF8A, 0x3DE0FF, 0x4D7CFF, 0xB45CFF, 0xFF5CD0, 0xF0F0F0
    };
    private static final int BW = 230;
    private static final int BH = 26;
    private static final int BG = 7;

    private final long openedAt = System.currentTimeMillis();
    private final float[] hover = new float[LABELS.length];

    public CustomTitleScreen() {
        super(Text.literal("HamsterVisuals"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    // ------------------------------------------------------------ раскладка

    private int logoPx() {
        return this.height >= 340 ? 4 : (this.height >= 270 ? 3 : 2);
    }

    private float titleScale() {
        float tw = this.textRenderer.getWidth(TITLE);
        return Math.max(1.5f, Math.min(4f, (this.width - 40f) / tw));
    }

    private int blockHeight() {
        return 16 * logoPx() + 6 + (int) (8 * titleScale()) + 36;
    }

    private int startY() {
        int total = blockHeight() + LABELS.length * (BH + BG);
        return Math.max(6, (this.height - 46 - total) / 2);
    }

    private int buttonY(int i) {
        return startY() + blockHeight() + i * (BH + BG);
    }

    private int swatchX(int i) {
        int total = SWATCHES.length * 16 - 6;
        return this.width / 2 - total / 2 + i * 16;
    }

    private int swatchY() {
        return this.height - 36;
    }

    private static long hash(int i) {
        long h = (i + 1) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h & Long.MAX_VALUE;
    }

    // ------------------------------------------------------------ рисование

    private void aurora(DrawContext ctx, long t, int acc) {
        for (int i = 0; i < 3; i++) {
            int rgb = Ui.blend(acc, i == 1 ? 0x3DE0FF : (i == 2 ? 0xFF5CD0 : acc), 0.45f);
            int cy = (int) (this.height * (0.28 + 0.2 * i) + Math.sin(t * 0.0005 + i * 2.1) * this.height * 0.06);
            int bh = (int) (this.height * 0.2);
            ctx.fillGradient(0, cy - bh, this.width, cy, Ui.argb(0, rgb), Ui.argb(0x2C, rgb));
            ctx.fillGradient(0, cy, this.width, cy + bh, Ui.argb(0x2C, rgb), Ui.argb(0, rgb));
        }
    }

    private void layers(DrawContext ctx, long t, int acc) {
        // три слоя летающих значков: дальние мелкие и тусклые, ближние крупные
        int[] counts = {12, 9, 6};
        int[] sizes = {1, 2, 3};
        double[] speeds = {0.010, 0.020, 0.036};
        int[] alphas = {26, 42, 60};
        int k = 0;
        for (int layer = 0; layer < 3; layer++) {
            for (int j = 0; j < counts[layer]; j++, k++) {
                long h = hash(k);
                double fx = (h & 1023) / 1023.0;
                double off = ((h >> 10) & 1023) / 1023.0;
                int type = (int) ((h >> 20) % 3);
                double travel = this.height + 80.0;
                double ph = (((t % 1000000L) * speeds[layer]) + off * travel) % travel;
                int y = (int) (this.height + 40 - ph);
                int x = (int) (fx * this.width + Math.sin(t * 0.001 + k) * (8 + layer * 8));
                int alpha = alphas[layer] + (int) (16 * Math.sin(t * 0.002 + k * 1.7));
                Sprites.draw(ctx, type, x, y, sizes[layer], Ui.argb(Math.max(8, alpha), k % 5 == 0 ? 0xFFFFFF : acc));
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = Ui.accent();
        TextRenderer tr = this.textRenderer;
        long t = System.currentTimeMillis();
        long age = t - openedAt;

        ctx.fillGradient(0, 0, this.width, this.height, 0xFF05060A,
                Ui.argb(0xFF, Ui.blend(0x05060A, acc, 0.24f)));
        aurora(ctx, t, acc);
        layers(ctx, t, acc);

        // хомяк-талисман: покачивается и моргает
        int lp = logoPx();
        int top = startY();
        int bob = (int) (Math.sin(t * 0.003) * 2);
        boolean blink = (t % 3200L) < 140L;
        int lx = this.width / 2 - 8 * lp;
        Ui.rrect(ctx, lx - 6, top + bob - 4, 16 * lp + 12, 16 * lp + 8, 10, Ui.argb(0x22, acc));
        Sprites.drawHamsterBlink(ctx, lx, top + bob, lp, blink);

        // название: каждая буква чуть качается
        float sc = titleScale();
        float tw = tr.getWidth(TITLE) * sc;
        float tx = this.width / 2f - tw / 2f;
        float ty = top + 16 * lp + 6;
        for (int i = 0; i < TITLE.length(); i++) {
            String ch = String.valueOf(TITLE.charAt(i));
            float ox = tr.getWidth(TITLE.substring(0, i)) * sc;
            float wy = (float) Math.sin(t * 0.004 + i * 0.55) * 2f;
            Ui.scaled(ctx, tr, ch, tx + ox + 2, ty + wy + 2, Ui.argb(0xFF, acc), sc, false);
            Ui.scaled(ctx, tr, ch, tx + ox, ty + wy, 0xFFFFFFFF, sc, false);
        }

        // подзаголовок со сменой фраз
        int pi = (int) ((t / 4000L) % PHRASES.length);
        double ph = (t % 4000L) / 4000.0;
        int pa = (int) (255 * Math.min(1.0, Math.min(ph * 6, (1 - ph) * 6)));
        String sub = PHRASES[pi];
        float sw = tr.getWidth(sub);
        Ui.scaled(ctx, tr, sub, this.width / 2f - sw / 2f, ty + 8 * sc + 7, Ui.argb(Math.max(0, pa), 0x9AA0B0), 1f, false);
        ctx.fill(this.width / 2 - 30, (int) (ty + 8 * sc + 21), this.width / 2 + 30, (int) (ty + 8 * sc + 23),
                Ui.argb(0xFF, acc));

        // кнопки: выезжают слева и подсвечиваются при наведении
        int baseX = this.width / 2 - BW / 2;
        for (int i = 0; i < LABELS.length; i++) {
            float e = Math.max(0f, Math.min(1f, (age - i * 70L) / 360f));
            e = 1f - (1f - e) * (1f - e) * (1f - e);
            int slide = (int) ((1f - e) * -70f);
            int by = buttonY(i);
            boolean hov = mouseX >= baseX && mouseX < baseX + BW && mouseY >= by && mouseY < by + BH;
            hover[i] += ((hov ? 1f : 0f) - hover[i]) * 0.25f;
            float hv = hover[i];
            int grow = (int) (10 * hv);
            int bx = baseX - grow / 2 + slide;
            int bw = BW + grow;

            if (hv > 0.02f) {
                Ui.rrect(ctx, bx - 1, by - 1, bw + 2, BH + 2, 5, Ui.argb((int) (0xAA * hv), acc));
            }
            Ui.rrect(ctx, bx, by, bw, BH, 4, Ui.argb(0xE0, Ui.blend(0x141620, Ui.blend(0x141620, acc, 0.45f), hv)));
            Ui.rrect(ctx, bx, by + 4, 3 + (int) (3 * hv), BH - 8, 1, Ui.argb(0xFF, Ui.blend(Ui.blend(0x141620, acc, 0.4f), acc, hv)));

            // значок слева
            int ix = bx + 14;
            int iy = by + (BH - 16) / 2;
            int icol = Ui.argb(0xFF, Ui.blend(0xB8BCC8, 0xFFFFFF, hv));
            switch (i) {
                case 0 -> Sprites.draw(ctx, 0, ix, iy, 1, icol);
                case 1 -> Sprites.draw(ctx, 1, ix, iy, 1, icol);
                case 2 -> Sprites.draw(ctx, 2, ix, iy, 1, icol);
                case 3 -> {
                    ctx.getMatrices().push();
                    ctx.getMatrices().translate(ix, iy + 2, 0f);
                    ctx.getMatrices().scale(0.85f, 0.85f, 1f);
                    Sprites.drawHamster(ctx, 0, 0, 1);
                    ctx.getMatrices().pop();
                }
                default -> {
                    for (int d = 0; d < 10; d++) {
                        ctx.fill(ix + 3 + d, iy + 3 + d, ix + 4 + d, iy + 4 + d, icol);
                        ctx.fill(ix + 12 - d, iy + 3 + d, ix + 13 - d, iy + 4 + d, icol);
                    }
                }
            }

            int lw = tr.getWidth(LABELS[i]);
            ctx.drawText(tr, LABELS[i], bx + (bw - lw) / 2 + 8, by + (BH - 8) / 2 + 1, 0xFFFFFFFF, true);
        }

        // быстрый выбор цвета темы
        for (int i = 0; i < SWATCHES.length; i++) {
            int sx = swatchX(i);
            int sy = swatchY();
            if ((Ui.accent()) == SWATCHES[i]) {
                Ui.rrect(ctx, sx - 2, sy - 2, 14, 14, 4, 0xFFFFFFFF);
            }
            Ui.rrect(ctx, sx, sy, 10, 10, 3, Ui.argb(0xFF, SWATCHES[i]));
        }
        String hint = "Right Shift - меню HamsterVisuals";
        int hw = tr.getWidth(hint);
        ctx.drawText(tr, hint, this.width / 2 - hw / 2, this.height - 18, 0xFF6C7280, false);

        // подвал: версия и ник
        Ui.scaled(ctx, tr, "HamsterVisuals 1.4.0  |  Minecraft 1.21.4", 8, this.height - 12, 0xFF5A606E, 0.8f, false);
        if (this.client != null && this.client.getSession() != null) {
            String name = this.client.getSession().getUsername();
            float nw = tr.getWidth(name) * 0.9f;
            Ui.scaled(ctx, tr, name, this.width - nw - 28, this.height - 16, 0xFFB8BCC8, 0.9f, false);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(this.width - 22, this.height - 20, 0f);
            ctx.getMatrices().scale(0.9f, 0.9f, 1f);
            Sprites.drawHamster(ctx, 0, 0, 1);
            ctx.getMatrices().pop();
        }

        // что играет сейчас
        if (VisualsConfig.I.musicHud && MusicTracker.present()) {
            String np = "Играет: " + MusicTracker.title()
                    + (MusicTracker.artist().isEmpty() ? "" : " - " + MusicTracker.artist());
            Ui.scaled(ctx, tr, tr.trimToWidth(np, (int) ((this.width * 0.45f) / 0.85f)), 8, this.height - 26,
                    Ui.argb(0xFF, acc), 0.85f, false);
        }
    }

    // ------------------------------------------------------------ ввод

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
            for (int i = 0; i < SWATCHES.length; i++) {
                int sx = swatchX(i);
                int sy = swatchY();
                if (mouseX >= sx && mouseX < sx + 10 && mouseY >= sy && mouseY < sy + 10) {
                    VisualsConfig.I.accent = SWATCHES[i];
                    VisualsConfig.save();
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
