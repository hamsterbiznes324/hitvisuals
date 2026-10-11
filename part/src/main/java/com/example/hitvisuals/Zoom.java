package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;

/** Зум по зажатой клавише: плавно меняет угол обзора и чувствительность мыши. */
public final class Zoom {
    private static boolean active = false;
    private static double cur = 70;
    private static int savedFov = 70;
    private static double savedSens = 0.5;
    private static long lastNanos = 0;

    public static void update(MinecraftClient mc) {
        VisualsConfig c = VisualsConfig.I;
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;

        boolean want = c.zoomEnabled && HitVisualsClient.zoomKey != null && HitVisualsClient.zoomKey.isPressed()
                && mc.currentScreen == null && mc.player != null;

        if (want) {
            if (!active) {
                savedFov = mc.options.getFov().getValue();
                savedSens = mc.options.getMouseSensitivity().getValue();
                cur = savedFov;
                active = true;
            }
            double k = c.zoomSmooth ? 1.0 - Math.exp(-dt * 14.0) : 1.0;
            cur += (c.zoomFov - cur) * k;
            mc.options.getFov().setValue((int) Math.round(cur));
            if (c.zoomSens) {
                double ratio = Math.max(0.15, c.zoomFov / (double) Math.max(1, savedFov));
                mc.options.getMouseSensitivity().setValue(savedSens * ratio);
            }
        } else if (active) {
            double k = c.zoomSmooth ? 1.0 - Math.exp(-dt * 14.0) : 1.0;
            cur += (savedFov - cur) * k;
            mc.options.getMouseSensitivity().setValue(savedSens);
            if (Math.abs(savedFov - cur) < 0.6) {
                mc.options.getFov().setValue(savedFov);
                active = false;
            } else {
                mc.options.getFov().setValue((int) Math.round(cur));
            }
        }
    }

    /** Возвращает настройки на место (при выходе из игры). */
    public static void restore() {
        if (!active) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.getFov().setValue(savedFov);
        mc.options.getMouseSensitivity().setValue(savedSens);
        active = false;
    }

    private Zoom() {}
}
