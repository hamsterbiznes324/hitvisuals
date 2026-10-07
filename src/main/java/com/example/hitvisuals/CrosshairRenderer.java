package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

/** Свой прицел и хит-маркер при попадании. */
public final class CrosshairRenderer {
    public static final String[] STYLES = {"Крест", "Точка", "Круг", "Крест и точка", "Квадрат"};
    public static final String[] COLOR_MODES = {"Цвет темы", "Белый", "Радужный", "Свой цвет"};

    private static int color(VisualsConfig c) {
        switch (c.crossColorMode) {
            case 1:
                return 0xFFFFFF;
            case 2:
                return Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.7f, 1f) & 0xFFFFFF;
            case 3:
                return c.crossColor & 0xFFFFFF;
            default:
                return Ui.accent();
        }
    }

    private static void rect(DrawContext ctx, int x1, int y1, int x2, int y2, int col, boolean outline) {
        if (outline) {
            ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xB0000000);
        }
        ctx.fill(x1, y1, x2, y2, col);
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        VisualsConfig c = VisualsConfig.I;
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;

        long since = System.currentTimeMillis() - HudRenderer.lastHitTime();

        // хит-маркер: четыре чёрточки по диагоналям
        if (c.hitMarker && since >= 0 && since < 240) {
            int a = (int) (255 * (1f - since / 240f));
            int col = Ui.argb(a, 0xFFFFFF);
            for (int i = 0; i < 4; i++) {
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int d = 4 + i;
                        ctx.fill(cx + sx * d, cy + sy * d, cx + sx * d + 1, cy + sy * d + 1, col);
                    }
                }
            }
        }

        if (!c.customCrosshair) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        int spread = 0;
        if (c.crossDynamic && mc.player != null) {
            if (since >= 0 && since < 160) spread += 3;
            if (mc.player.getVelocity().horizontalLength() > 0.06) spread += 1;
            if (mc.player.isSprinting()) spread += 1;
        }

        int col = Ui.argb(0xFF, color(c));
        boolean ol = c.crossOutline;
        int gap = c.crossGap + spread;
        int len = c.crossSize;
        int th = Math.max(1, c.crossThickness);
        int half = th / 2;

        switch (c.crossStyle) {
            case 1 -> dot(ctx, cx, cy, th, col, ol);
            case 2 -> circle(ctx, cx, cy, len + gap / 2, th, col);
            case 3 -> {
                bars(ctx, cx, cy, gap, len, th, half, col, ol);
                dot(ctx, cx, cy, th, col, ol);
            }
            case 4 -> {
                int r = len + gap / 2;
                rect(ctx, cx - r, cy - r, cx + r + 1, cy - r + th, col, ol);
                rect(ctx, cx - r, cy + r + 1 - th, cx + r + 1, cy + r + 1, col, ol);
                rect(ctx, cx - r, cy - r, cx - r + th, cy + r + 1, col, ol);
                rect(ctx, cx + r + 1 - th, cy - r, cx + r + 1, cy + r + 1, col, ol);
            }
            default -> bars(ctx, cx, cy, gap, len, th, half, col, ol);
        }
    }

    private static void bars(DrawContext ctx, int cx, int cy, int gap, int len, int th, int half, int col, boolean ol) {
        rect(ctx, cx - gap - len, cy - half, cx - gap, cy - half + th, col, ol);
        rect(ctx, cx + gap, cy - half, cx + gap + len, cy - half + th, col, ol);
        rect(ctx, cx - half, cy - gap - len, cx - half + th, cy - gap, col, ol);
        rect(ctx, cx - half, cy + gap, cx - half + th, cy + gap + len, col, ol);
    }

    private static void dot(DrawContext ctx, int cx, int cy, int th, int col, boolean ol) {
        int h = th;
        rect(ctx, cx - h, cy - h, cx + h + 1, cy + h + 1, col, ol);
    }

    private static void circle(DrawContext ctx, int cx, int cy, int r, int th, int col) {
        for (int a = 0; a < 360; a += 5) {
            double rad = Math.toRadians(a);
            int x = cx + (int) Math.round(Math.cos(rad) * r);
            int y = cy + (int) Math.round(Math.sin(rad) * r);
            ctx.fill(x, y, x + th, y + th, col);
        }
    }

    private CrosshairRenderer() {}
}
