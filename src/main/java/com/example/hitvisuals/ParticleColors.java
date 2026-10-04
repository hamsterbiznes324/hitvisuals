package com.example.hitvisuals;

import java.awt.Color;

/** Выбор цвета для новых частиц по настройке. */
public final class ParticleColors {
    public static int pick() {
        switch (VisualsConfig.I.particleColorMode) {
            case 1:
                return Color.HSBtoRGB((float) Math.random(), 0.7f, 1f) & 0xFFFFFF;
            case 2:
                return 0xFFFFFF;
            default:
                return VisualsConfig.I.accent & 0xFFFFFF;
        }
    }

    private ParticleColors() {}
}
