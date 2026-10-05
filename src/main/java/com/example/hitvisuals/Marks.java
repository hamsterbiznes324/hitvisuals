package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Метки в мире: ставятся на то, куда ты смотришь, и видны на экране сквозь стены. */
public final class Marks {

    /** Ключ мира: адрес сервера или название одиночного мира. */
    public static String worldKey(MinecraftClient mc) {
        try {
            if (mc.getCurrentServerEntry() != null) {
                return "srv:" + mc.getCurrentServerEntry().address;
            }
            if (mc.getServer() != null) {
                return "sp:" + mc.getServer().getSaveProperties().getLevelName();
            }
        } catch (Exception ignored) {
        }
        return "local";
    }

    private static String dimKey(MinecraftClient mc) {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    /** Метки текущего мира и измерения. */
    public static List<VisualsConfig.Mark> current(MinecraftClient mc) {
        List<VisualsConfig.Mark> out = new ArrayList<>();
        if (mc.world == null) return out;
        String w = worldKey(mc);
        String d = dimKey(mc);
        for (VisualsConfig.Mark m : VisualsConfig.I.marks) {
            if (w.equals(m.world) && d.equals(m.dim)) out.add(m);
        }
        return out;
    }

    public static void remove(VisualsConfig.Mark m) {
        VisualsConfig.I.marks.remove(m);
        VisualsConfig.save();
    }

    public static void clearCurrent(MinecraftClient mc) {
        VisualsConfig.I.marks.removeAll(current(mc));
        VisualsConfig.save();
    }

    private static void say(ClientPlayerEntity p, String s) {
        p.sendMessage(Text.literal(s), true);
    }

    /** Если смотришь на существующую метку, убирает её, иначе ставит новую. */
    public static void toggleAtCrosshair(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d from = cam.getPos();
        Vec3d look = Vec3d.fromPolar(cam.getPitch(), cam.getYaw());

        VisualsConfig.Mark best = null;
        double bestDot = Math.cos(Math.toRadians(4.0));
        for (VisualsConfig.Mark m : current(mc)) {
            Vec3d d = new Vec3d(m.x - from.x, m.y - from.y, m.z - from.z);
            double len = d.length();
            if (len < 0.5) continue;
            double dot = d.multiply(1.0 / len).dotProduct(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = m;
            }
        }
        if (best != null) {
            remove(best);
            say(p, "Метка удалена: " + best.name);
            return;
        }

        HitResult hit = p.raycast(256.0, 1.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            say(p, "Некуда ставить метку");
            return;
        }
        Vec3d pos = hit.getPos();
        VisualsConfig.Mark m = new VisualsConfig.Mark();
        m.world = worldKey(mc);
        m.dim = dimKey(mc);
        m.name = "Метка " + (current(mc).size() + 1);
        m.x = pos.x;
        m.y = pos.y;
        m.z = pos.z;
        m.color = VisualsConfig.I.markColor;
        VisualsConfig.I.marks.add(m);
        VisualsConfig.save();
        say(p, "Метка поставлена: " + m.name);
    }

    /** Рисует метки поверх экрана. */
    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!VisualsConfig.I.showMarks || mc.player == null || mc.world == null) return;
        List<VisualsConfig.Mark> list = current(mc);
        if (list.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cyaw = Math.cos(yaw);
        double syaw = Math.sin(yaw);
        double cpit = Math.cos(pitch);
        double spit = Math.sin(pitch);

        // вперёд, вправо и вверх в координатах камеры
        double fx = -syaw * cpit;
        double fy = -spit;
        double fz = cyaw * cpit;
        double rx = -cyaw;
        double rz = -syaw;
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double focal = (h / 2.0) / Math.tan(fov / 2.0);
        int margin = 14;

        for (VisualsConfig.Mark m : list) {
            double dx = m.x - cp.x;
            double dy = m.y - cp.y;
            double dz = m.z - cp.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double xc = dx * rx + dz * rz;
            double yc = dx * ux + dy * uy + dz * uz;
            double zc = dx * fx + dy * fy + dz * fz;

            double sx;
            double sy;
            boolean edge = false;
            if (zc > 0.05) {
                sx = w / 2.0 + xc / zc * focal;
                sy = h / 2.0 - yc / zc * focal;
                if (sx < margin || sx > w - margin || sy < margin || sy > h - margin) {
                    edge = true;
                }
            } else {
                sx = 0;
                sy = 0;
                edge = true;
            }
            if (edge) {
                double vx = xc;
                double vy = -yc;
                double len = Math.sqrt(vx * vx + vy * vy);
                if (len < 1e-6) {
                    vx = 0;
                    vy = -1;
                    len = 1;
                }
                vx /= len;
                vy /= len;
                double tx = Math.abs(vx) < 1e-6 ? 1e9 : (w / 2.0 - margin) / Math.abs(vx);
                double ty = Math.abs(vy) < 1e-6 ? 1e9 : (h / 2.0 - margin) / Math.abs(vy);
                double t = Math.min(tx, ty);
                sx = w / 2.0 + vx * t;
                sy = h / 2.0 + vy * t;
            }

            int x = (int) sx;
            int y = (int) sy;
            int col = Ui.argb(edge ? 0xAA : 0xFF, m.color);
            diamond(ctx, x, y, edge ? 3 : 4, col);
            if (!edge) {
                diamond(ctx, x, y, 2, 0xFFFFFFFF);
                int nw = tr.getWidth(m.name);
                ctx.drawText(tr, m.name, x - nw / 2, y - 16, 0xFFFFFFFF, true);
            }
            String ds = String.format(Locale.ROOT, "%d м", (int) Math.round(dist));
            int dw = tr.getWidth(ds);
            Ui.scaled(ctx, tr, ds, x - dw * 0.75f / 2f, y + 7, Ui.argb(0xFF, m.color), 0.75f, true);
        }
    }

    private static void diamond(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private Marks() {}
}
