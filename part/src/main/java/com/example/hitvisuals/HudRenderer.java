package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Всё, что рисуется поверх игры: вспышка, плашка цели, плеер, слова песни, метки, прицел. */
public final class HudRenderer {
    private static final long FLASH_MS = 350L;
    private static long lastHit = 0L;

    private static LivingEntity lastTarget = null;
    private static long lastTargetTime = 0L;
    private static float shownHp = -1f;
    private static float predictDiff = Float.NaN;

    private static MusicTracker.Lyrics lastLyrics = null;
    private static int lastLyricIdx = -2;

    public static void markHit() {
        lastHit = System.currentTimeMillis();
    }

    public static long lastHitTime() {
        return lastHit;
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;
        flash(ctx);
        if (mc.player == null || mc.world == null) return;
        Marks.render(ctx, mc);
        FloatTexts.render(ctx, mc);
        CrosshairRenderer.render(ctx, mc);
        target(ctx, mc);
        music(ctx, mc);
        Extras.render(ctx, mc);
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
        if (shownHp < 0) shownHp = hp;
        shownHp += (hp - shownHp) * 0.2f;

        String name = le.getName().getString();
        boolean isPlayer = le instanceof PlayerEntity;
        float dist = mc.player.distanceTo(le);

        float[] r = HudLayout.rect(HudLayout.TARGET, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        ctx.getMatrices().push();
        ctx.getMatrices().translate(r[0], r[1], 0f);
        ctx.getMatrices().scale(r[4], r[4], 1f);
        predictDiff = VisualsConfig.I.targetPredict
                ? (mc.player.getHealth() + mc.player.getAbsorptionAmount()) - (hp + le.getAbsorptionAmount())
                : Float.NaN;
        drawTargetCard(ctx, mc.textRenderer, name, hp, max, shownHp, le.getAbsorptionAmount(), dist,
                isPlayer && Social.isFriend(name), isPlayer && Social.isModUser(name), fade);
        predictDiff = Float.NaN;
        ctx.getMatrices().pop();
    }

    /** Рисует карточку цели в точке (0, 0). Размер 142 x 40. */
    public static void drawTargetCard(DrawContext ctx, TextRenderer tr, String name, float hp, float max,
                                      float shown, float abs, float dist, boolean friend, boolean modUser,
                                      float fade) {
        int acc = Ui.accent();
        int w = 142;
        int h = 40;
        int a = (int) (255 * fade);

        Ui.rrect(ctx, -1, -1, w + 2, h + 2, 6, Ui.argb((int) (0x77 * fade), acc));
        Ui.rrect(ctx, 0, 0, w, h, 5, Ui.argb((int) (0xE0 * fade), 0x0F1118));

        int nameMax = w - 16 - (modUser ? 16 : 0) - (friend ? 34 : 0);
        ctx.drawText(tr, tr.trimToWidth(name, nameMax), 8, 6, Ui.argb(a, 0xFFFFFF), true);
        if (!Float.isNaN(predictDiff)) {
            boolean win = predictDiff >= 0f;
            String ps = (win ? "WIN " : "LOSE ") + String.format(Locale.ROOT, "%.1f", Math.abs(predictDiff));
            int px = 8 + tr.getWidth(tr.trimToWidth(name, nameMax)) + 5;
            if (px + tr.getWidth(ps) * 0.8f < w - 8 - (modUser ? 16 : 0) - (friend ? 34 : 0)) {
                Ui.scaled(ctx, tr, ps, px, 7, Ui.argb(a, win ? 0x3DFF8A : 0xFF4D5E), 0.8f, true);
            }
        }
        int rx = w - 8;
        if (modUser) {
            rx -= 12;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(rx, 4, 0f);
            ctx.getMatrices().scale(0.75f, 0.75f, 1f);
            Sprites.drawHamster(ctx, 0, 0, 1);
            ctx.getMatrices().pop();
            rx -= 2;
        }
        if (friend) {
            String s = "ДРУГ";
            float sw = tr.getWidth(s) * 0.8f;
            Ui.scaled(ctx, tr, s, rx - sw, 7, Ui.argb(a, 0x3DFF8A), 0.8f, false);
        }

        int bx = 8;
        int by = 20;
        int bw = w - 16;
        Ui.rrect(ctx, bx, by, bw, 6, 3, Ui.argb((int) (0xFF * fade), 0x2B2F39));
        float ratio = Math.max(0f, Math.min(1f, shown / max));
        int fill = Math.max(ratio > 0 ? 4 : 0, (int) (bw * ratio));
        if (fill > 0) {
            Ui.rrect(ctx, bx, by, fill, 6, 3, Ui.argb(a, hpColor(hp / max)));
        }

        String hs = String.format(Locale.ROOT, "%.1f / %.0f HP", hp, max)
                + (abs > 0 ? String.format(Locale.ROOT, " +%.0f", abs) : "");
        Ui.scaled(ctx, tr, hs, bx, 30, Ui.argb(a, 0xB8BCC8), 0.8f, false);
        String ds = String.format(Locale.ROOT, "%.1f м", dist);
        float dw = tr.getWidth(ds) * 0.8f;
        Ui.scaled(ctx, tr, ds, bx + bw - dw, 30, Ui.argb(a, 0x8A90A0), 0.8f, false);
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
        int W = ctx.getScaledWindowWidth();
        int H = ctx.getScaledWindowHeight();

        // карточка трека
        float[] r = HudLayout.rect(HudLayout.MUSIC, W, H);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(r[0], r[1], 0f);
        ctx.getMatrices().scale(r[4], r[4], 1f);
        drawMusicCard(ctx, tr, MusicTracker.title(), MusicTracker.artist(), MusicTracker.position(),
                MusicTracker.duration(), MusicTracker.playing());
        ctx.getMatrices().pop();

        if (!c.musicLyrics) return;
        MusicTracker.Lyrics ly = MusicTracker.lyrics();
        if (ly != lastLyrics) {
            lastLyrics = ly;
            lastLyricIdx = -2;
        }

        double p = MusicTracker.position() + 0.15 + c.lyricsOffset;
        int idx = -1;
        if (ly != null) {
            for (int i = 0; i < ly.times().length; i++) {
                if (ly.times()[i] <= p) idx = i;
                else break;
            }
        }

        if (c.lyricsStyle == 0) {
            // буквы в мире перед игроком
            if (ly != null && idx >= 0 && idx != lastLyricIdx && MusicTracker.playing()) {
                lastLyricIdx = idx;
                String line = ly.lines()[idx];
                if (!line.isEmpty()) {
                    double next = idx + 1 < ly.times().length ? ly.times()[idx + 1] : ly.times()[idx] + 4;
                    long life = (long) Math.max(2500, Math.min(6500, (next - ly.times()[idx]) * 1000 + 1500));
                    Camera cam = mc.gameRenderer.getCamera();
                    Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());
                    Vec3d pos = cam.getPos().add(look.multiply(4.5)).add(0, 0.15, 0);
                    FloatTexts.spawn(line, pos.x, pos.y, pos.z, life, Ui.blend(0xFFFFFF, Ui.accent(), 0.45f),
                            0.5 * c.lyricsSize, true);
                }
            } else if (ly != null && idx != lastLyricIdx && idx >= 0) {
                lastLyricIdx = idx;
            }
            return;
        }

        // строки внизу экрана
        float[] lr = HudLayout.rect(HudLayout.LYRICS, W, H);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(lr[0], lr[1], 0f);
        ctx.getMatrices().scale(lr[4], lr[4], 1f);
        drawLyricsBox(ctx, tr, ly, idx, MusicTracker.lyricsState());
        ctx.getMatrices().pop();
    }

    /** Карточка плеера в точке (0, 0). Размер 214 x 42. */
    public static void drawMusicCard(DrawContext ctx, TextRenderer tr, String title, String artist,
                                     double pos, double dur, boolean playing) {
        int acc = Ui.accent();
        int cw = 214;
        int ch = 42;

        Ui.rrect(ctx, -1, -1, cw + 2, ch + 2, 7, Ui.argb(0x66, acc));
        Ui.rrect(ctx, 0, 0, cw, ch, 6, 0xE00F1118);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            int bh = playing ? 4 + (int) (14 * (0.5 + 0.5 * Math.sin(t * 0.009 + i * 1.4))) : 4;
            ctx.fill(11 + i * 5, 30 - bh, 14 + i * 5, 30, Ui.argb(0xFF, acc));
        }

        int tx = 38;
        int maxW = cw - 38 - 10;
        ctx.drawText(tr, tr.trimToWidth(title, maxW), tx, 7, 0xFFFFFFFF, true);

        String ar = artist == null || artist.isEmpty() ? "—" : artist;
        Ui.scaled(ctx, tr, tr.trimToWidth(ar, (int) (maxW * 0.55f / 0.8f)), tx, 19, 0xFF8A90A0, 0.8f, false);

        String time = fmt(pos) + " / " + fmt(dur);
        float tw = tr.getWidth(time) * 0.75f;
        Ui.scaled(ctx, tr, time, cw - 10 - tw, 19, 0xFF8A90A0, 0.75f, false);

        float prog = dur > 0 ? (float) Math.max(0, Math.min(1, pos / dur)) : 0f;
        Ui.rrect(ctx, tx, 32, maxW, 3, 1, 0xFF2B2F39);
        if (prog > 0) {
            Ui.rrect(ctx, tx, 32, Math.max(2, (int) (maxW * prog)), 3, 1, Ui.argb(0xFF, acc));
        }
    }

    /** Три строки слов в точке (0, 0). Размер 240 x 48. */
    public static void drawLyricsBox(DrawContext ctx, TextRenderer tr, MusicTracker.Lyrics ly, int idx, int state) {
        int acc = Ui.accent();
        if (ly == null || ly.lines().length == 0) {
            String msg = state == MusicTracker.LY_LOADING ? "Ищу слова песни..." : "Слова не найдены";
            float mw = tr.getWidth(msg) * 0.8f;
            Ui.scaled(ctx, tr, msg, 120 - mw / 2f, 18, 0xFF6C7280, 0.8f, true);
            return;
        }
        lyricLine(ctx, tr, ly, idx - 1, 240, 2, 0.8f, 0xFF8A90A0);
        lyricLine(ctx, tr, ly, idx, 240, 15, 1.25f, Ui.argb(0xFF, Ui.blend(0xFFFFFF, acc, 0.25f)));
        lyricLine(ctx, tr, ly, idx + 1, 240, 36, 0.8f, 0xFF8A90A0);
    }

    private static void lyricLine(DrawContext ctx, TextRenderer tr, MusicTracker.Lyrics ly, int i,
                                  int boxW, int y, float scale, int color) {
        if (i < 0 || i >= ly.lines().length) return;
        String s = ly.lines()[i];
        if (s.isEmpty()) return;
        s = tr.trimToWidth(s, (int) (boxW / scale));
        float w = tr.getWidth(s) * scale;
        Ui.scaled(ctx, tr, s, boxW / 2f - w / 2f, y, color, scale, true);
    }

    private HudRenderer() {}
}
