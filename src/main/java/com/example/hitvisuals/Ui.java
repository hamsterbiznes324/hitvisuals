package com.example.hitvisuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Вспомогательные функции для рисования интерфейса. */
public final class Ui {
    public static int accent() {
        return VisualsConfig.I.accent & 0xFFFFFF;
    }

    /** Собирает ARGB из прозрачности (0-255) и RGB. */
    public static int argb(int a, int rgb) {
        return ((a & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    /** Смешивает два RGB-цвета. t = 0 даёт первый, t = 1 даёт второй. */
    public static int blend(int rgb1, int rgb2, float t) {
        int r = (int) (((rgb1 >> 16) & 255) * (1 - t) + ((rgb2 >> 16) & 255) * t);
        int g = (int) (((rgb1 >> 8) & 255) * (1 - t) + ((rgb2 >> 8) & 255) * t);
        int b = (int) ((rgb1 & 255) * (1 - t) + (rgb2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Скруглённый прямоугольник без наложения полос (прозрачные цвета не темнеют). */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) {
            c.fill(x, y + r, x + w, y + h - r, color);
        }
    }

    /** Текст с масштабом. */
    public static void scaled(DrawContext c, TextRenderer tr, String s, float x, float y, int color, float scale, boolean shadow) {
        c.getMatrices().push();
        c.getMatrices().translate(x, y, 0f);
        c.getMatrices().scale(scale, scale, 1f);
        c.drawText(tr, s, 0, 0, color, shadow);
        c.getMatrices().pop();
    }

    private Ui() {}
}
