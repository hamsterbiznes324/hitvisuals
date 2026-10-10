package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Конструктор прицела: рисуешь свой прицел по клеточкам. */
public class CrosshairEditorScreen extends Screen {
    private static final int N = CrosshairPixels.N;

    private final Screen parent;
    private final char[] cells;
    private char brush = '1';
    private boolean mirror = false;
    private boolean painting = false;
    private boolean eraserTool = false;
    private boolean rmb = false;

    public CrosshairEditorScreen(Screen parent) {
        super(Text.literal("Конструктор прицела"));
        this.parent = parent;
        this.cells = CrosshairPixels.normalize(VisualsConfig.I.crossPixels).toCharArray();
    }

    private int cell() {
        return this.height >= 300 ? 13 : 10;
    }

    private int totalW() {
        return N * cell() + 16 + 150;
    }

    private int gx() {
        return (this.width - totalW()) / 2;
    }

    private int gy() {
        return (this.height - N * cell()) / 2;
    }

    private int sx() {
        return gx() + N * cell() + 16;
    }

    // кнопки справа: 0 очистить, 1 плюс, 2 точка, 3 круг, 4 симметрия, 5 готово
    private static final String[] BTN = {"Очистить", "Плюс", "Точка", "Круг", "Симметрия", "Готово"};

    private int[] btn(int i) {
        return new int[]{sx(), gy() + 112 + i * 24, 150, 20};
    }

    private int[] swatch(int i) {
        // 0 - цвет темы, 1..8 - палитра, 9 - ластик
        int col = i % 5;
        int row = i / 5;
        return new int[]{sx() + col * 30, gy() + 18 + row * 30, 26, 26};
    }

    private static boolean in(double mx, double my, int[] r) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private void setCell(int x, int y, char ch) {
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        cells[y * N + x] = ch;
        if (mirror) {
            cells[y * N + (N - 1 - x)] = ch;
            cells[(N - 1 - y) * N + x] = ch;
            cells[(N - 1 - y) * N + (N - 1 - x)] = ch;
        }
    }

    private void paintAt(double mx, double my) {
        int cs = cell();
        int x = (int) Math.floor((mx - gx()) / cs);
        int y = (int) Math.floor((my - gy()) / cs);
        if (x < 0 || y < 0 || x >= N || y >= N) return;
        setCell(x, y, (eraserTool || rmb) ? '0' : brush);
    }

    private void load(String s) {
        String n = CrosshairPixels.normalize(s);
        for (int i = 0; i < cells.length; i++) cells[i] = n.charAt(i);
    }

    private void saveToConfig() {
        VisualsConfig c = VisualsConfig.I;
        c.crossPixels = new String(cells);
        c.customCrosshair = true;
        c.crossStyle = 5;
        VisualsConfig.save();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = Ui.accent();
        ctx.fill(0, 0, this.width, this.height, 0xCC05060A);

        int cs = cell();
        int gx = gx();
        int gy = gy();
        int pad = 10;
        Ui.rrect(ctx, gx - pad - 1, gy - 26 - 1, totalW() + pad * 2 + 2, N * cs + 26 + pad + 28 + 2, 8, Ui.argb(0x77, acc));
        Ui.rrect(ctx, gx - pad, gy - 26, totalW() + pad * 2, N * cs + 26 + pad + 28, 7, 0xFA0E1015);
        Ui.scaled(ctx, this.textRenderer, "Конструктор прицела", gx, gy - 18, 0xFFFFFFFF, 1.3f, false);

        // сетка
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                boolean mid = x == N / 2 || y == N / 2;
                int bg = ((x + y) % 2 == 0) ? 0xFF14171E : 0xFF171A21;
                if (mid) bg = 0xFF1C2029;
                ctx.fill(gx + x * cs, gy + y * cs, gx + (x + 1) * cs, gy + (y + 1) * cs, bg);
                int rgb = CrosshairPixels.rgb(cells[y * N + x]);
                if (rgb >= 0) {
                    ctx.fill(gx + x * cs + 1, gy + y * cs + 1, gx + (x + 1) * cs - 1, gy + (y + 1) * cs - 1,
                            Ui.argb(0xFF, rgb));
                }
            }
        }
        // рамка вокруг сетки
        int gw = N * cs;
        ctx.fill(gx - 1, gy - 1, gx + gw + 1, gy, Ui.argb(0xFF, acc));
        ctx.fill(gx - 1, gy + gw, gx + gw + 1, gy + gw + 1, Ui.argb(0xFF, acc));
        ctx.fill(gx - 1, gy, gx, gy + gw, Ui.argb(0xFF, acc));
        ctx.fill(gx + gw, gy, gx + gw + 1, gy + gw, Ui.argb(0xFF, acc));

        // палитра
        ctx.drawText(this.textRenderer, "Цвет кисти", sx(), gy + 4, 0xFFB8BCC8, false);
        for (int i = 0; i < 10; i++) {
            int[] r = swatch(i);
            boolean sel;
            int col;
            if (i == 0) {
                sel = brush == '1' && !eraserTool;
                col = Ui.accent();
            } else if (i <= 8) {
                sel = brush == (char) ('2' + i - 1) && !eraserTool;
                col = CrosshairPixels.PALETTE[i - 1];
            } else {
                sel = eraserTool;
                col = 0x2B2F39;
            }
            if (sel) Ui.rrect(ctx, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, 5, 0xFFFFFFFF);
            Ui.rrect(ctx, r[0], r[1], r[2], r[3], 4, Ui.argb(0xFF, col));
            if (i == 9) {
                ctx.drawText(this.textRenderer, "X", r[0] + 10, r[1] + 9, 0xFFFFFFFF, false);
            }
        }

        // кнопки
        for (int i = 0; i < BTN.length; i++) {
            int[] r = btn(i);
            boolean hov = in(mouseX, mouseY, r);
            boolean on = i == 4 && mirror;
            int bg = on ? Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.6f))
                    : (hov ? Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.35f)) : 0xFF171A21);
            if (i == 5) bg = Ui.argb(0xFF, Ui.blend(0x171A21, acc, hov ? 0.85f : 0.65f));
            Ui.rrect(ctx, r[0], r[1], r[2], r[3], 4, bg);
            String label = BTN[i] + (i == 4 ? (mirror ? ": вкл" : ": выкл") : "");
            ctx.drawText(this.textRenderer, label, r[0] + (r[2] - this.textRenderer.getWidth(label)) / 2, r[1] + 6,
                    0xFFFFFFFF, false);
        }

        // предпросмотр и размер клетки
        int py = gy + 112 + BTN.length * 24 + 6;
        ctx.drawText(this.textRenderer, "Размер клетки: " + VisualsConfig.I.crossPixelSize, sx(), py, 0xFFB8BCC8, false);
        int[] minus = {sx() + 110, py - 3, 16, 14};
        int[] plus = {sx() + 130, py - 3, 16, 14};
        Ui.rrect(ctx, minus[0], minus[1], minus[2], minus[3], 3, 0xFF171A21);
        Ui.rrect(ctx, plus[0], plus[1], plus[2], plus[3], 3, 0xFF171A21);
        ctx.drawText(this.textRenderer, "-", minus[0] + 6, minus[1] + 3, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, "+", plus[0] + 5, plus[1] + 3, 0xFFFFFFFF, false);

        // маленький предпросмотр слева внизу
        int pvx = gx + 36;
        int pvy = gy + N * cs + 22;
        Ui.rrect(ctx, pvx - 24, pvy - 14, 48, 28, 4, 0xFF0A0C10);
        CrosshairRenderer.drawPixels(ctx, pvx, pvy, new String(cells), 1, VisualsConfig.I.crossOutline);
        ctx.drawText(this.textRenderer, "ЛКМ - рисовать, ПКМ - стирать", gx + 80, gy + N * cs + 18, 0xFF8A90A0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // палитра
        for (int i = 0; i < 10; i++) {
            if (in(mouseX, mouseY, swatch(i))) {
                if (i == 0) {
                    brush = '1';
                    eraserTool = false;
                } else if (i <= 8) {
                    brush = (char) ('2' + i - 1);
                    eraserTool = false;
                } else {
                    eraserTool = true;
                }
                return true;
            }
        }
        // кнопки
        for (int i = 0; i < BTN.length; i++) {
            if (in(mouseX, mouseY, btn(i))) {
                switch (i) {
                    case 0 -> load(CrosshairPixels.empty());
                    case 1 -> load(CrosshairPixels.plus());
                    case 2 -> load(CrosshairPixels.dot());
                    case 3 -> load(CrosshairPixels.ring());
                    case 4 -> mirror = !mirror;
                    default -> close();
                }
                return true;
            }
        }
        int py = gy() + 112 + BTN.length * 24 + 6;
        VisualsConfig c = VisualsConfig.I;
        if (in(mouseX, mouseY, new int[]{sx() + 110, py - 3, 16, 14})) {
            c.crossPixelSize = Math.max(1, c.crossPixelSize - 1);
            return true;
        }
        if (in(mouseX, mouseY, new int[]{sx() + 130, py - 3, 16, 14})) {
            c.crossPixelSize = Math.min(4, c.crossPixelSize + 1);
            return true;
        }
        // сетка
        int cs = cell();
        if (mouseX >= gx() && mouseX < gx() + N * cs && mouseY >= gy() && mouseY < gy() + N * cs) {
            painting = true;
            rmb = button == 1;
            paintAt(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (painting) {
            paintAt(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (painting) {
            painting = false;
            rmb = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        saveToConfig();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
