package com.example.hitvisuals;

/** Положение и размер маленьких панелей на экране. */
public final class HudLayout {
    public static final int MUSIC = 0;
    public static final int TARGET = 1;
    public static final int LYRICS = 2;
    public static final String[] NAMES = {"Плеер", "Цель", "Слова"};
    public static final int[] W = {214, 142, 240};
    public static final int[] H = {42, 40, 48};

    public static double cx(int id) {
        VisualsConfig c = VisualsConfig.I;
        return id == MUSIC ? c.musicX : (id == TARGET ? c.targetX : c.lyricsX);
    }

    public static double top(int id) {
        VisualsConfig c = VisualsConfig.I;
        return id == MUSIC ? c.musicY : (id == TARGET ? c.targetY : c.lyricsY);
    }

    public static double scale(int id) {
        VisualsConfig c = VisualsConfig.I;
        return id == MUSIC ? c.musicScale : (id == TARGET ? c.targetScale : c.lyricsBoxScale);
    }

    public static void setScale(int id, double s) {
        s = Math.max(0.4, Math.min(1.6, s));
        VisualsConfig c = VisualsConfig.I;
        if (id == MUSIC) c.musicScale = s;
        else if (id == TARGET) c.targetScale = s;
        else c.lyricsBoxScale = s;
    }

    public static void setPos(int id, double cx, double top) {
        cx = Math.max(0, Math.min(1, cx));
        top = Math.max(0, Math.min(1, top));
        VisualsConfig c = VisualsConfig.I;
        if (id == MUSIC) {
            c.musicX = cx;
            c.musicY = top;
        } else if (id == TARGET) {
            c.targetX = cx;
            c.targetY = top;
        } else {
            c.lyricsX = cx;
            c.lyricsY = top;
        }
    }

    /** Прямоугольник панели на экране: {left, top, width, height, scale}. */
    public static float[] rect(int id, int sw, int sh) {
        float sc = (float) scale(id);
        float w = W[id] * sc;
        float h = H[id] * sc;
        float left = (float) (cx(id) * sw - w / 2f);
        float top = (float) (top(id) * sh);
        left = Math.max(0, Math.min(sw - w, left));
        top = Math.max(0, Math.min(sh - h, top));
        return new float[]{left, top, w, h, sc};
    }

    public static void setFromRect(int id, float left, float top, int sw, int sh) {
        float sc = (float) scale(id);
        float w = W[id] * sc;
        setPos(id, (left + w / 2f) / sw, top / sh);
    }

    public static void reset() {
        VisualsConfig c = VisualsConfig.I;
        c.musicX = 0.5;
        c.musicY = 0.012;
        c.musicScale = 0.75;
        c.targetX = 0.60;
        c.targetY = 0.56;
        c.targetScale = 0.70;
        c.lyricsX = 0.5;
        c.lyricsY = 0.78;
        c.lyricsBoxScale = 1.0;
    }

    private HudLayout() {}
}
