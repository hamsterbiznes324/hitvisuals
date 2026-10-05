package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

/** Всё, что рисуется поверх игры: вспышка, плашка цели, плеер, слова песни, метки. */
public final class HudRenderer {
    private static final long FLASH_MS = 350L;
    private static long lastHit = 0L;

    private static LivingEntity lastTarget = null;
    private static long lastTargetTime = 0L;
    private static float shownHp = -1f;

    public static void markHit() {
        lastHit = System.currentTimeMillis();
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        flash(ctx);
        if (mc.player == null || mc.world == null) return;
        Marks.render(ctx, mc);
        target(ctx, mc);
        music(ctx, mc);
    }

    // ------------------------------------------------------------ вспышка

    private static void flash(DrawContext ctx) {
        if (!VisualsConfig.I.flash) return;
        long elapsed = System.currentTimeMillis() - lastHit;
        if (elapsed < 0 || elapsed > FLASH_MS) return;
        float t = 1f - (elapsed / (float) FLASH_MS);
        int alpha = (int) (t * 90);
        if (alpha <= 0) return;
        ctx.fill(0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), Ui.argb(alpha, Ui.accent()));
    }

    // ------------------------------------------------------------ плашка цели

    private static int hpColor(float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        if (ratio > 0.5f) {
            return Ui.blend(0xFFD23F, 0x3DFF8A, (ratio - 0.5f) * 2f);
        }
        return Ui.blend(0xFF3B3B, 0xFFD23F, ratio * 2f);
    }

    private static void target(DrawContext ctx, MinecraftClient mc) {
        if (!VisualsConfig.I.targetHud) return;
        long now = System.currentTimeMillis();
        Entity t = mc.targetedEntity;
        if (t instanceof LivingEntity le && le != mc.player && le.isAlive()) {
            if (le != lastTarget) {
                shownHp = -1f;
            }
            lastTarget = le;
            lastTargetTime = now;
        }
        if (lastTarget == null) return;
        long age = now - lastTargetTime;
        if (age > 1500 || lastTarget.isRemoved()) return;
        float fade = age > 1000 ? (1500 - age) / 500f : 1f;

        LivingEntity le = lastTarget;
        float hp = le.getHealth();
        float max = Math.max(1f, le.getMaxHealth());
        float abs = le.getAbsorptionAmount();
        if (shownHp < 0) shownHp = hp;
        shownHp += (hp - shownHp) * 0.2f;

        TextRenderer tr = mc.textRenderer;
        int acc = Ui.accent();
        int w = 142;
        int h = 40;
        int x = ctx.getScaledWindowWidth() / 2 + 16;
        int y = ctx.getScaledWindowHeight() / 2 + 12;
        int a = (int) (255 * fade);

        String name = le.getName().getString();
        boolean isPlayer = le instanceof PlayerEntity;
        boolean friend = isPlayer && Social.isFriend(name);
        boolean modUser = isPlayer && Social.isModUser(name);

        Ui.rrect(ctx, x - 1, y - 1, w + 2, h + 2, 6, Ui.argb((int) (0x77 * fade), acc));
        Ui.rrect(ctx, x, y, w, h, 5, Ui.argb((int) (0xE0 * fade), 0x0F1118));

        int nameMax = w - 16 - (modUser ? 16 : 0) - (friend ? 34 : 0);
        ctx.drawText(tr, tr.trimToWidth(name, nameMax), x + 8, y + 6, Ui.argb(a, 0xFFFFFF), true);
        int rx = x + w - 8;
        if (modUser) {
            rx -= 12;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(rx, y + 4, 0f);
            ctx.getMatrices().scale(0.75f, 0.75f, 1f);
            Sprites.drawHamster(ctx, 0, 0, 1);
            ctx.getMatrices().pop();
            rx -= 2;
        }
        if (friend) {
            String s = "ДРУГ";
            float sw = tr.getWidth(s) * 0.8f;
            Ui.scaled(ctx, tr, s, rx - sw, y + 7, Ui.argb(a, 0x3DFF8A), 0.8f, false);
        }

        int bx = x + 8;
        int by = y + 20;
        int bw = w - 16;
        Ui.rrect(ctx, bx, by, bw, 6, 3, Ui.argb((int) (0xFF * fade), 0x2B2F39));
        float ratio = Math.max(0f, Math.min(1f, shownHp / max));
        int fill = Math.max(ratio > 0 ? 4 : 0, (int) (bw * ratio));
        if (fill > 0) {
            Ui.rrect(ctx, bx, by, fill, 6, 3, Ui.argb(a, hpColor(hp / max)));
        }

        String hs = String.format(Locale.ROOT, "%.1f / %.0f HP", hp, max) + (abs > 0 ? String.format(Locale.ROOT, " +%.0f", abs) : "");
        Ui.scaled(ctx, tr, hs, bx, y + 30, Ui.argb(a, 0xB8BCC8), 0.8f, false);
        String ds = String.format(Locale.ROOT, "%.1f м", mc.player.distanceTo(le));
        float dw = tr.getWidth(ds) * 0.8f;
        Ui.scaled(ctx, tr, ds, bx + bw - dw, y + 30, Ui.argb(a, 0x8A90A0), 0.8f, false);
    }

    // ------------------------------------------------------------ музыка и слова

    private static String fmt(double sec) {
        int t = (int) Math.max(0, sec);
        return (t / 60) + ":" + String.format(Locale.ROOT, "%02d", t % 60);
    }

    private static void music(DrawContext ctx, MinecraftClient mc) {
        VisualsConfig c = VisualsConfig.I;
        if (!c.musicHud || !MusicTracker.present()) return;

        TextRenderer tr = mc.textRenderer;
        int acc = Ui.accent();
        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();
        int cw = 214;
        int ch = 42;
        int x = W / 2 - cw / 2;
        int y = 6;
        boolean playing = MusicTracker.playing();

        Ui.rrect(ctx, x - 1, y - 1, cw + 2, ch + 2, 7, Ui.argb(0x66, acc));
        Ui.rrect(ctx, x, y, cw, ch, 6, 0xE00F1118);

        // «эквалайзер»
        long t = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            int bh = playing ? 4 + (int) (14 * (0.5 + 0.5 * Math.sin(t * 0.009 + i * 1.4))) : 4;
            ctx.fill(x + 11 + i * 5, y + 30 - bh, x + 14 + i * 5, y + 30, Ui.argb(0xFF, acc));
        }

        int tx = x + 38;
        int maxW = cw - 38 - 10;
        ctx.drawText(tr, tr.trimToWidth(MusicTracker.title(), maxW), tx, y + 7, 0xFFFFFFFF, true);

        String artist = MusicTracker.artist();
        if (artist.isEmpty()) artist = "—";
        Ui.scaled(ctx, tr, tr.trimToWidth(artist, (int) (maxW * 0.55f / 0.8f)), tx, y + 19, 0xFF8A90A0, 0.8f, false);

        String time = fmt(MusicTracker.position()) + " / " + fmt(MusicTracker.duration());
        float tw = tr.getWidth(time) * 0.75f;
        Ui.scaled(ctx, tr, time, x + cw - 10 - tw, y + 19, 0xFF8A90A0, 0.75f, false);

        double dur = MusicTracker.duration();
        float prog = dur > 0 ? (float) Math.max(0, Math.min(1, MusicTracker.position() / dur)) : 0f;
        int px = tx;
        int pw = maxW;
        Ui.rrect(ctx, px, y + 32, pw, 3, 1, 0xFF2B2F39);
        if (prog > 0) {
            Ui.rrect(ctx, px, y + 32, Math.max(2, (int) (pw * prog)), 3, 1, Ui.argb(0xFF, acc));
        }

        // слова песни
        if (!c.musicLyrics) return;
        MusicTracker.Lyrics ly = MusicTracker.lyrics();
        int baseY = H - 96;
        if (ly == null || ly.lines().length == 0) {
            String msg = MusicTracker.lyricsState() == MusicTracker.LY_LOADING
                    ? "Ищу слова песни..."
                    : "Слова не найдены";
            float mw = tr.getWidth(msg) * 0.8f;
            Ui.scaled(ctx, tr, msg, W / 2f - mw / 2f, baseY, 0xFF6C7280, 0.8f, true);
            return;
        }
        double p = MusicTracker.position() + 0.15;
        int idx = -1;
        for (int i = 0; i < ly.times().length; i++) {
            if (ly.times()[i] <= p) idx = i;
            else break;
        }
        lyricLine(ctx, tr, ly, idx - 1, W, baseY - 16, 0.9f, 0xFF8A90A0);
        lyricLine(ctx, tr, ly, idx, W, baseY, 1.4f, Ui.argb(0xFF, Ui.blend(0xFFFFFF, acc, 0.25f)));
        lyricLine(ctx, tr, ly, idx + 1, W, baseY + 18, 0.9f, 0xFF8A90A0);
    }

    private static void lyricLine(DrawContext ctx, TextRenderer tr, MusicTracker.Lyrics ly, int i,
                                  int screenW, int y, float scale, int color) {
        if (i < 0 || i >= ly.lines().length) return;
        String s = ly.lines()[i];
        if (s.isEmpty()) return;
        s = tr.trimToWidth(s, (int) ((screenW - 40) / scale));
        float w = tr.getWidth(s) * scale;
        Ui.scaled(ctx, tr, s, screenW / 2f - w / 2f, y, color, scale, true);
    }

    private HudRenderer() {}
}
