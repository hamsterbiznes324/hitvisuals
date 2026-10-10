package com.example.hitvisuals;

import java.awt.Color;

/** Пресеты цвета неба и тумана. */
public final class SkyPresets {
    public static final String[] NAMES = {
            "Выкл (обычное)", "Закат", "Ночь", "Неон", "Кровавое", "Радуга", "Свой цвет"
    };

    private static final int[] SKY = {0, 0xE8643C, 0x0A0F2E, 0xB400FF, 0x6B0000, 0, 0};
    private static final int[] FOG = {0, 0xFFB36B, 0x151B45, 0xFF2BD6, 0xA00000, 0, 0};

    public static boolean active() {
        int s = VisualsConfig.I.sky;
        return s > 0 && s < NAMES.length;
    }

    /** Цвет неба (RGB). */
    public static int skyRgb() {
        int i = VisualsConfig.I.sky;
        if (i == 5) return rainbow(0f);
        if (i == 6) return VisualsConfig.I.skyColor & 0xFFFFFF;
        return SKY[i];
    }

    /** Цвет тумана у горизонта (RGB). */
    public static int fogRgb() {
        int i = VisualsConfig.I.sky;
        if (i == 5) return rainbow(0.12f);
        if (i == 6) return Ui.blend(VisualsConfig.I.skyColor & 0xFFFFFF, 0xFFFFFF, 0.3f);
        return FOG[i];
    }

    private static int rainbow(float offset) {
        float hue = ((System.currentTimeMillis() % 12000L) / 12000f + offset) % 1f;
        return Color.HSBtoRGB(hue, 0.7f, 1f) & 0xFFFFFF;
    }

    private SkyPresets() {}
}
