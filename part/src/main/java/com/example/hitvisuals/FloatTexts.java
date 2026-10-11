package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Буквы, которые «висят» в мире перед игроком: появляются по одной, плывут вверх и пропадают.
 * Используются для слов песни и для надписи при убийстве.
 */
public final class FloatTexts {
    private static final class FT {
        String text;
        double x;
        double y;
        double z;
        long start;
        long life;
        int rgb;
        double worldH;
        boolean typewriter;
    }

    private static final List<FT> LIST = new ArrayList<>();

    public static void spawn(String text, double x, double y, double z, long lifeMs, int rgb,
                             double worldHeight, boolean typewriter) {
        if (text == null || text.isEmpty()) return;
        FT f = new FT();
        f.text = text;
        f.x = x;
        f.y = y;
        f.z = z;
        f.start = System.currentTimeMillis();
        f.life = lifeMs;
        f.rgb = rgb;
        f.worldH = worldHeight;
        f.typewriter = typewriter;
        LIST.add(f);
        while (LIST.size() > 8) {
            LIST.remove(0);
        }
    }

    public static void clear() {
        LIST.clear();
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (LIST.isEmpty()) return;
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        for (int idx = LIST.size() - 1; idx >= 0; idx--) {
            FT f = LIST.get(idx);
            long age = now - f.start;
            if (age > f.life) {
                LIST.remove(idx);
                continue;
            }
            double rise = age * 0.00028;
            Projection.P p = Projection.project(mc, sw, sh, f.x, f.y + rise, f.z);
            if (!p.front) continue;

            float s = (float) (p.focal * f.worldH / p.depth / 9.0);
            float tw = tr.getWidth(f.text);
            s = Math.min(s, (sw * 0.92f) / Math.max(1f, tw));
            s = Math.max(0.5f, Math.min(6f, s));

            float alphaIn = Math.min(1f, age / 180f);
            float alphaOut = Math.min(1f, (f.life - age) / 700f);
            float alpha = Math.max(0f, Math.min(alphaIn, alphaOut));

            int len = f.text.length();
            int shown = f.typewriter ? (int) Math.min(len, 1 + age / 38) : len;
            float startX = (float) p.x - tw * s / 2f;
            float y = (float) p.y - 4.5f * s;

            for (int i = 0; i < shown; i++) {
                float charAge = f.typewriter ? (age - i * 38f) : 999f;
                float pop = 1f + 0.7f * Math.max(0f, 1f - charAge / 170f);
                float ox = tr.getWidth(f.text.substring(0, i)) * s;
                int cw = tr.getWidth(String.valueOf(f.text.charAt(i)));
                int col = Ui.blend(0xFFFFFF, f.rgb, len <= 1 ? 0f : (float) i / (len - 1));
                int a = (int) (255 * alpha);
                if (a < 4) continue;
                float cxm = startX + ox + cw * s / 2f;
                float cym = y + 4.5f * s;
                ctx.getMatrices().push();
                ctx.getMatrices().translate(cxm, cym, 0f);
                ctx.getMatrices().scale(s * pop, s * pop, 1f);
                ctx.drawText(tr, String.valueOf(f.text.charAt(i)), -cw / 2, -4, Ui.argb(a, col), true);
                ctx.getMatrices().pop();
            }
        }
    }

    private FloatTexts() {}
}
