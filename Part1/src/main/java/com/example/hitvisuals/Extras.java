package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.Locale;

/** Дополнительные панели: водяной знак, информация, клавиши и CPS, красные края при низком здоровье. */
public final class Extras {
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastL = false;
    private static boolean lastR = false;
    private static int hits = 0;

    public static void hit() {
        hits++;
    }

    public static void resetHits() {
        hits = 0;
    }

    private static int cps(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        VisualsConfig c = VisualsConfig.I;
        long now = System.currentTimeMillis();
        TextRenderer tr = mc.textRenderer;

        boolean l = mc.options.attackKey.isPressed();
        boolean r = mc.options.useKey.isPressed();
        if (l && !lastL) LEFT.addLast(now);
        if (r && !lastR) RIGHT.addLast(now);
        lastL = l;
        lastR = r;

        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();
        int acc = Ui.accent();

        lowHealth(ctx, mc, W, H, now);

        int y = 6;
        if (c.watermark) {
            String t1 = "Hamster";
            String t2 = "Visuals";
            String fps = mc.getCurrentFps() + " FPS";
            int w = 10 + tr.getWidth(t1 + t2) + 10 + tr.getWidth(fps) + 10;
            Ui.rrect(ctx, 6, y, w, 16, 5, Ui.argb(0xD0, 0x0F1118));
            Ui.rrect(ctx, 6, y + 3, 2, 10, 1, Ui.argb(0xFF, acc));
            ctx.drawText(tr, t1, 14, y + 4, 0xFFFFFFFF, true);
            ctx.drawText(tr, t2, 14 + tr.getWidth(t1), y + 4, Ui.argb(0xFF, acc), true);
            ctx.drawText(tr, fps, 14 + tr.getWidth(t1 + t2) + 10, y + 4, 0xFFB8BCC8, false);
            y += 20;
        }

        if (c.infoPanel && mc.player != null) {
            int ping = 0;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            String[] lines = {
                    String.format(Locale.ROOT, "XYZ  %.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    "Пинг  " + ping + " мс",
                    "CPS  " + cps(LEFT, now) + " | " + cps(RIGHT, now) + (c.hitCounter ? "    Удары  " + hits : "")
            };
            int w = 0;
            for (String s : lines) w = Math.max(w, tr.getWidth(s));
            w += 16;
            int h = lines.length * 11 + 8;
            Ui.rrect(ctx, 6, y, w, h, 5, Ui.argb(0xC8, 0x0F1118));
            Ui.rrect(ctx, 6, y + 4, 2, h - 8, 1, Ui.argb(0xFF, acc));
            for (int i = 0; i < lines.length; i++) {
                ctx.drawText(tr, lines[i], 14, y + 5 + i * 11, 0xFFD8DBE6, false);
            }
        }

        if (c.keystrokes) {
            int size = 20;
            int gap = 2;
            int bx = W - 6 - (size * 3 + gap * 2);
            int by = H - 6 - (size * 3 + gap * 2);
            key(ctx, tr, bx + size + gap, by, size, "W", mc.options.forwardKey, acc);
            key(ctx, tr, bx, by + size + gap, size, "A", mc.options.leftKey, acc);
            key(ctx, tr, bx + size + gap, by + size + gap, size, "S", mc.options.backKey, acc);
            key(ctx, tr, bx + 2 * (size + gap), by + size + gap, size, "D", mc.options.rightKey, acc);
            int wide = size * 3 + gap * 2;
            int hw = (wide - gap) / 2;
            int my = by + 2 * (size + gap);
            mouse(ctx, tr, bx, my, hw, size, "ЛКМ " + cps(LEFT, now), l, acc);
            mouse(ctx, tr, bx + hw + gap, my, hw, size, "ПКМ " + cps(RIGHT, now), r, acc);
        }
    }

    private static void key(DrawContext ctx, TextRenderer tr, int x, int y, int s, String label, KeyBinding k, int acc) {
        mouse(ctx, tr, x, y, s, s, label, k.isPressed(), acc);
    }

    private static void mouse(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean down, int acc) {
        int bg = down ? Ui.argb(0xE0, Ui.blend(0x14161E, acc, 0.6f)) : Ui.argb(0xC0, 0x0F1118);
        Ui.rrect(ctx, x, y, w, h, 4, bg);
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
    }

    private static void lowHealth(DrawContext ctx, MinecraftClient mc, int W, int H, long now) {
        if (!VisualsConfig.I.lowHealthFx || mc.player == null) return;
        float hp = mc.player.getHealth();
        if (hp > 6f || hp <= 0f) return;
        float k = (6f - hp) / 6f;
        float pulse = 0.65f + 0.35f * (float) Math.sin(now * 0.006);
        int a = (int) (0x70 * k * pulse);
        if (a <= 0) return;
        int e = Math.max(30, H / 6);
        ctx.fillGradient(0, 0, W, e, Ui.argb(a, 0xFF1030), 0x00000000);
        ctx.fillGradient(0, H - e, W, H, 0x00000000, Ui.argb(a, 0xFF1030));
    }

    private Extras() {}
}
