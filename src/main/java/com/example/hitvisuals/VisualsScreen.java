package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Тёмное меню с вкладками слева и списком настроек справа. Открывается на Right Shift. */
public class VisualsScreen extends Screen {
    private static final String[] TABS = {"Удары", "Мир", "Игрок", "Тема"};
    private static final String[] TAB_TITLES = {"Удары", "Мир", "Игрок", "Тема и меню"};
    private static final String[] COLOR_MODES = {"Цвет темы", "Радужные", "Белые"};
    private static final String[] SWING_STYLES = {"Обычный", "Плавный", "Резкий"};
    private static final int[] SWATCHES = {
            0xFF3B5C, 0xFF8A3D, 0xFFD23F, 0x3DFF8A, 0x3DE0FF, 0x4D7CFF, 0xB45CFF, 0xFF5CD0, 0xF0F0F0
    };

    private static final int PW = 470;
    private static final int PH = 290;
    private static final int SIDE = 112;
    private static final int ROW_H = 34;
    private static final int GAP = 4;

    private final Screen parent;
    private final List<List<Row>> pages = new ArrayList<>();
    private int tab = 0;
    private double scroll = 0;
    private Row dragging = null;

    public VisualsScreen(Screen parent) {
        super(Text.literal("Hit Visuals"));
        this.parent = parent;
        build();
    }

    // ---------------------------------------------------------------- строки меню

    private static final class Row {
        static final int TOGGLE = 0;
        static final int SLIDER = 1;
        static final int CHOICE = 2;
        static final int HUE = 3;
        static final int SWATCH = 4;

        final int type;
        final String name;
        final String desc;
        BooleanSupplier getB;
        Consumer<Boolean> setB;
        DoubleSupplier getD;
        DoubleConsumer setD;
        double min;
        double max;
        int decimals;
        IntSupplier getI;
        IntConsumer setI;
        String[] options;

        Row(int type, String name, String desc) {
            this.type = type;
            this.name = name;
            this.desc = desc;
        }

        static Row toggle(String n, String d, BooleanSupplier g, Consumer<Boolean> s) {
            Row r = new Row(TOGGLE, n, d);
            r.getB = g;
            r.setB = s;
            return r;
        }

        static Row slider(String n, String d, double min, double max, int decimals, DoubleSupplier g, DoubleConsumer s) {
            Row r = new Row(SLIDER, n, d);
            r.min = min;
            r.max = max;
            r.decimals = decimals;
            r.getD = g;
            r.setD = s;
            return r;
        }

        static Row choice(String n, String d, String[] options, IntSupplier g, IntConsumer s) {
            Row r = new Row(CHOICE, n, d);
            r.options = options;
            r.getI = g;
            r.setI = s;
            return r;
        }

        static Row hue(String n, String d, IntSupplier g, IntConsumer s) {
            Row r = new Row(HUE, n, d);
            r.getI = g;
            r.setI = s;
            return r;
        }

        static Row swatch(String n, String d, IntSupplier g, IntConsumer s) {
            Row r = new Row(SWATCH, n, d);
            r.getI = g;
            r.setI = s;
            return r;
        }
    }

    private void build() {
        final VisualsConfig c = VisualsConfig.I;

        pages.add(List.of(
                Row.toggle("Эффекты при ударе", "Частицы, вспышка и звук при ударе по мобу",
                        () -> c.hitEffects, v -> c.hitEffects = v),
                Row.choice("Частицы удара", "Какие частицы вылетают из цели",
                        ModParticles.NAMES, () -> c.hitParticle, v -> c.hitParticle = v),
                Row.slider("Количество частиц", "Сколько частиц появляется за один удар",
                        1, 40, 0, () -> c.particleCount, v -> c.particleCount = (int) Math.round(v)),
                Row.choice("Цвет частиц", "Цвет звёзд, лун и черепов",
                        COLOR_MODES, () -> c.particleColorMode, v -> c.particleColorMode = v),
                Row.toggle("Вспышка экрана", "Короткая вспышка цветом темы при ударе",
                        () -> c.flash, v -> c.flash = v),
                Row.toggle("Звук удара", "Тихий звон при попадании",
                        () -> c.sound, v -> c.sound = v)
        ));

        pages.add(List.of(
                Row.choice("Небо", "Готовые цвета неба и тумана",
                        SkyPresets.NAMES, () -> c.sky, v -> c.sky = v),
                Row.hue("Свой цвет неба", "Работает, если выбрано «Свой цвет»",
                        () -> c.skyColor, v -> c.skyColor = v),
                Row.toggle("Fullbright", "Полная яркость, тёмных мест нет",
                        () -> c.fullbright, v -> c.fullbright = v)
        ));

        pages.add(List.of(
                Row.toggle("Трейл", "След из частиц за игроком при движении",
                        () -> c.trail, v -> c.trail = v),
                Row.choice("Частицы трейла", "Из чего состоит след",
                        ModParticles.NAMES, () -> c.trailParticle, v -> c.trailParticle = v),
                Row.toggle("Маленькие руки", "Уменьшает руку и предмет от первого лица",
                        () -> c.smallHands, v -> c.smallHands = v),
                Row.slider("Размер рук", "Чем меньше число, тем меньше рука",
                        0.3, 1.0, 2, () -> c.handScale, v -> c.handScale = v),
                Row.choice("Удар мечом", "Стиль взмаха от первого лица",
                        SWING_STYLES, () -> c.swingStyle, v -> c.swingStyle = v)
        ));

        pages.add(List.of(
                Row.swatch("Цвет темы", "Быстрый выбор цвета меню и частиц",
                        () -> c.accent, v -> c.accent = v),
                Row.hue("Оттенок темы", "Тонкая настройка цвета",
                        () -> c.accent, v -> c.accent = v),
                Row.toggle("Своё главное меню", "Заменяет стандартный экран при запуске игры",
                        () -> c.customTitle, v -> c.customTitle = v)
        ));
    }

    // ---------------------------------------------------------------- геометрия

    private int px() {
        return (this.width - PW) / 2;
    }

    private int py() {
        return (this.height - PH) / 2;
    }

    private int cx0() {
        return px() + SIDE + 10;
    }

    private int cy0() {
        return py() + 44;
    }

    private int cw0() {
        return PW - SIDE - 20;
    }

    private int ch0() {
        return PH - 54;
    }

    private static float hueOf(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null)[0];
    }

    // ---------------------------------------------------------------- рисование

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = Ui.accent();
        int x0 = px();
        int y0 = py();

        ctx.fill(0, 0, this.width, this.height, 0x99000000);

        Ui.rrect(ctx, x0 - 1, y0 - 1, PW + 2, PH + 2, 8, Ui.argb(0x77, acc));
        Ui.rrect(ctx, x0, y0, PW, PH, 7, 0xFA0E1015);
        Ui.rrect(ctx, x0 + 4, y0 + 4, SIDE - 4, PH - 8, 5, 0xFF12141A);

        // логотип
        Ui.rrect(ctx, x0 + 12, y0 + 12, 22, 22, 5, Ui.argb(0xFF, acc));
        ctx.drawText(this.textRenderer, "HV", x0 + 17, y0 + 19, 0xFFFFFFFF, true);
        ctx.drawText(this.textRenderer, "Hit Visuals", x0 + 40, y0 + 18, 0xFFFFFFFF, false);

        // вкладки
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 8;
            int ty = y0 + 54 + i * 24;
            boolean sel = i == tab;
            boolean hov = mouseX >= tx && mouseX < tx + SIDE - 16 && mouseY >= ty && mouseY < ty + 20;
            if (sel) {
                Ui.rrect(ctx, tx, ty, SIDE - 16, 20, 4, Ui.argb(0x33, acc));
                Ui.rrect(ctx, tx, ty + 4, 3, 12, 1, Ui.argb(0xFF, acc));
            } else if (hov) {
                Ui.rrect(ctx, tx, ty, SIDE - 16, 20, 4, 0x22FFFFFF);
            }
            ctx.drawText(this.textRenderer, TABS[i], tx + 12, ty + 6,
                    sel ? Ui.argb(0xFF, acc) : 0xFFB8BCC8, false);
        }
        Ui.scaled(ctx, this.textRenderer, "Right Shift - закрыть", x0 + 12, y0 + PH - 18, 0xFF6C7280, 0.75f, false);

        // заголовок справа
        Ui.scaled(ctx, this.textRenderer, TAB_TITLES[tab], cx0(), y0 + 12, 0xFFFFFFFF, 1.5f, false);
        Ui.scaled(ctx, this.textRenderer, "Клик - переключить, правая кнопка - назад по списку",
                cx0(), y0 + 29, 0xFF6C7280, 0.75f, false);
        Sprites.draw(ctx, 0, x0 + PW - 76, y0 + 10, 1, Ui.argb(0xFF, acc));
        Sprites.draw(ctx, 1, x0 + PW - 56, y0 + 10, 1, Ui.argb(0xFF, acc));
        Sprites.draw(ctx, 2, x0 + PW - 36, y0 + 10, 1, Ui.argb(0xFF, acc));

        // список
        int cx = cx0();
        int cy = cy0();
        int cw = cw0();
        int ch = ch0();
        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        List<Row> rows = pages.get(tab);
        for (int i = 0; i < rows.size(); i++) {
            int ry = cy + i * (ROW_H + GAP) - (int) scroll;
            if (ry + ROW_H < cy || ry > cy + ch) continue;
            boolean inside = mouseY >= cy && mouseY < cy + ch;
            drawRow(ctx, rows.get(i), cx, ry, cw, mouseX, mouseY, inside);
        }
        ctx.disableScissor();
    }

    private void drawRow(DrawContext ctx, Row r, int x, int y, int w, int mx, int my, boolean allowHover) {
        int acc = Ui.accent();
        boolean hover = allowHover && mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        boolean on = r.type == Row.TOGGLE && r.getB.getAsBoolean();

        int bg = hover ? 0xFF1E222B : 0xFF171A21;
        if (on) bg = Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.14f));
        Ui.rrect(ctx, x, y, w, ROW_H, 4, bg);
        if (on) Ui.rrect(ctx, x, y + 5, 3, ROW_H - 10, 1, Ui.argb(0xFF, acc));

        ctx.drawText(this.textRenderer, r.name, x + 12, y + 7, 0xFFEDEEF3, false);
        Ui.scaled(ctx, this.textRenderer, r.desc, x + 12, y + 20, 0xFF8A90A0, 0.75f, false);

        int right = x + w - 14;
        int cx = x + w - 150;
        int cw = 136;

        switch (r.type) {
            case Row.TOGGLE -> {
                int bx = right - 28;
                int by = y + (ROW_H - 14) / 2;
                Ui.rrect(ctx, bx, by, 28, 14, 7, on ? Ui.argb(0xFF, acc) : 0xFF2B2F39);
                int kx = on ? bx + 28 - 12 : bx + 2;
                Ui.rrect(ctx, kx, by + 2, 10, 10, 5, 0xFFFFFFFF);
            }
            case Row.CHOICE -> {
                int idx = Math.floorMod(r.getI.getAsInt(), r.options.length);
                String s = "<  " + r.options[idx] + "  >";
                int sw = this.textRenderer.getWidth(s);
                ctx.drawText(this.textRenderer, s, right - sw, y + 13, Ui.argb(0xFF, acc), false);
            }
            case Row.SLIDER -> {
                double v = r.getD.getAsDouble();
                float f = (float) Math.max(0, Math.min(1, (v - r.min) / (r.max - r.min)));
                String vs = r.decimals == 0
                        ? String.valueOf((int) Math.round(v))
                        : String.format(Locale.ROOT, "%." + r.decimals + "f", v);
                ctx.drawText(this.textRenderer, vs, right - this.textRenderer.getWidth(vs), y + 6, 0xFFB8BCC8, false);
                Ui.rrect(ctx, cx, y + 21, cw, 4, 2, 0xFF2B2F39);
                Ui.rrect(ctx, cx, y + 21, Math.max(4, (int) (cw * f)), 4, 2, Ui.argb(0xFF, acc));
                Ui.rrect(ctx, cx + (int) (cw * f) - 4, y + 18, 8, 10, 3, 0xFFFFFFFF);
            }
            case Row.HUE -> {
                int rgb = r.getI.getAsInt();
                for (int i = 0; i < cw; i += 2) {
                    int col = Color.HSBtoRGB(i / (float) cw, 0.7f, 1f) & 0xFFFFFF;
                    ctx.fill(cx + i, y + 19, cx + Math.min(i + 2, cw), y + 27, Ui.argb(0xFF, col));
                }
                int kx = cx + (int) (hueOf(rgb) * (cw - 3));
                ctx.fill(kx - 1, y + 16, kx + 4, y + 30, 0xFFFFFFFF);
                ctx.fill(kx, y + 17, kx + 3, y + 29, Ui.argb(0xFF, rgb));
                Ui.rrect(ctx, right - 10, y + 4, 10, 10, 3, Ui.argb(0xFF, rgb));
            }
            case Row.SWATCH -> {
                int cur = r.getI.getAsInt() & 0xFFFFFF;
                int total = SWATCHES.length * 18 - 4;
                int sx = right - total;
                int sy = y + (ROW_H - 14) / 2;
                for (int i = 0; i < SWATCHES.length; i++) {
                    int px = sx + i * 18;
                    if (SWATCHES[i] == cur) {
                        Ui.rrect(ctx, px - 2, sy - 2, 18, 18, 4, 0xFFFFFFFF);
                    }
                    Ui.rrect(ctx, px, sy, 14, 14, 3, Ui.argb(0xFF, SWATCHES[i]));
                }
            }
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- мышь и клавиши

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        for (int i = 0; i < TABS.length; i++) {
            int tx = px() + 8;
            int ty = py() + 54 + i * 24;
            if (mx >= tx && mx < tx + SIDE - 16 && my >= ty && my < ty + 20) {
                tab = i;
                scroll = 0;
                dragging = null;
                return true;
            }
        }

        if (mx >= cx0() && mx < cx0() + cw0() && my >= cy0() && my < cy0() + ch0()) {
            List<Row> rows = pages.get(tab);
            for (int i = 0; i < rows.size(); i++) {
                int ry = cy0() + i * (ROW_H + GAP) - (int) scroll;
                if (my >= ry && my < ry + ROW_H) {
                    clickRow(rows.get(i), mx, button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickRow(Row r, int mx, int button) {
        int x = cx0();
        int w = cw0();
        switch (r.type) {
            case Row.TOGGLE -> r.setB.accept(!r.getB.getAsBoolean());
            case Row.CHOICE -> {
                int n = r.options.length;
                int v = r.getI.getAsInt() + (button == 1 ? -1 : 1);
                r.setI.accept(Math.floorMod(v, n));
            }
            case Row.SLIDER, Row.HUE -> {
                if (mx >= x + w - 156) {
                    dragging = r;
                    drag(r, mx);
                }
            }
            case Row.SWATCH -> {
                int total = SWATCHES.length * 18 - 4;
                int sx = x + w - 14 - total;
                int rel = mx - sx;
                if (rel >= 0 && rel < total && rel % 18 < 14) {
                    r.setI.accept(SWATCHES[rel / 18]);
                }
            }
            default -> {
            }
        }
    }

    private void drag(Row r, int mx) {
        int cx = cx0() + cw0() - 150;
        float f = Math.max(0f, Math.min(1f, (mx - cx) / 136f));
        if (r.type == Row.SLIDER) {
            double v = r.min + f * (r.max - r.min);
            if (r.decimals == 0) v = Math.round(v);
            r.setD.accept(v);
        } else if (r.type == Row.HUE) {
            r.setI.accept(Color.HSBtoRGB(Math.min(f, 0.999f), 0.7f, 1f) & 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            drag(dragging, (int) mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            VisualsConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int n = pages.get(tab).size();
        int total = n * (ROW_H + GAP) - GAP;
        int max = Math.max(0, total - ch0());
        scroll = Math.max(0, Math.min(max, scroll - verticalAmount * 16));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        VisualsConfig.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
