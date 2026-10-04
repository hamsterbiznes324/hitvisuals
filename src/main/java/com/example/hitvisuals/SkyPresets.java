package com.example.hitvisuals;

import java.awt.Color;

/** Пресеты цвета неба и тумана. */
public final class SkyPresets {
    public static final String[] NAMES = {
            "Выкл (обычное)", "Закат", "Ночь", "Неон", "Кровавое", "Радуга"
    };

    private static final int[] SKY = {0, 0xE8643C, 0x0A0F2E, 0xB400FF, 0x6B0000, 0};
    private static final int[] FOG = {0, 0xFFB36B, 0x151B45, 0xFF2BD6, 0xA00000, 0};

    public static boolean active() {
        return VisualsConfig.sky > 0 && VisualsConfig.sky < NAMES.length;
    }

    /** Цвет неба в формате RGB. */
    public static int skyRgb() {
        int i = VisualsConfig.sky;
        if (i == 5) return rainbow(0f);
        return SKY[i];
    }

    /** Цвет тумана (у горизонта) в формате RGB. */
    public static int fogRgb() {
        int i = VisualsConfig.sky;
        if (i == 5) return rainbow(0.12f);
        return FOG[i];
    }

    private static int rainbow(float offset) {
        float hue = ((System.currentTimeMillis() % 12000L) / 12000f + offset) % 1f;
        return Color.HSBtoRGB(hue, 0.7f, 1f) & 0xFFFFFF;
    }

    private SkyPresets() {}
}
