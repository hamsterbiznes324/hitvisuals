package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Экран, где маленькие панели можно двигать мышью и менять им размер колесом. */
public class HudEditScreen extends Screen {
    private final Screen parent;
    private int dragId = -1;
    private float offX;
    private float offY;

    public HudEditScreen(Screen parent) {
        super(Text.literal("Положение панелей"));
        this.parent = parent;
    }

    private int[] ids() {
        if (VisualsConfig.I.lyricsStyle == 1) {
            return new int[]{HudLayout.MUSIC, HudLayout.TARGET, HudLayout.LYRICS};
        }
        return new int[]{HudLayout.MUSIC, HudLayout.TARGET};
    }

    private int hit(double mx, double my) {
        int[] ids = ids();
        for (int k = ids.length - 1; k >= 0; k--) {
            float[] r = HudLayout.rect(ids[k], this.width, this.height);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                return ids[k];
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = Ui.accent();
        ctx.fill(0, 0, this.width, this.height, 0x66000000);

        // перекрестие по центру, чтобы было удобно выравнивать
        ctx.fill(this.width / 2, 0, this.width / 2 + 1, this.height, 0x22FFFFFF);
        ctx.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x22FFFFFF);

        int hover = dragId >= 0 ? dragId : hit(mouseX, mouseY);

        for (int id : ids()) {
            float[] r = HudLayout.rect(id, this.width, this.height);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(r[0], r[1], 0f);
            ctx.getMatrices().scale(r[4], r[4], 1f);
            if (id == HudLayout.MUSIC) {
                HudRenderer.drawMusicCard(ctx, this.textRenderer, "Название трека", "Исполнитель", 83, 221, true);
            } else if (id == HudLayout.TARGET) {
                HudRenderer.drawTargetCard(ctx, this.textRenderer, "Zombie", 14f, 20f, 14f, 0f, 3.2f,
                        false, false, 1f);
            } else {
                MusicTracker.Lyrics demo = new MusicTracker.Lyrics(new double[]{0, 4, 8},
                        new String[]{"Прошлая строка", "Сейчас поётся эта", "Дальше будет эта"});
                HudRenderer.drawLyricsBox(ctx, this.textRenderer, demo, 1, MusicTracker.LY_OK);
            }
            ctx.getMatrices().pop();

            int border = id == hover ? Ui.argb(0xFF, acc) : Ui.argb(0x88, acc);
            int x1 = (int) r[0] - 2;
            int y1 = (int) r[1] - 2;
            int x2 = (int) (r[0] + r[2]) + 2;
            int y2 = (int) (r[1] + r[3]) + 2;
            ctx.fill(x1, y1, x2, y1 + 1, border);
            ctx.fill(x1, y2 - 1, x2, y2, border);
            ctx.fill(x1, y1, x1 + 1, y2, border);
            ctx.fill(x2 - 1, y1, x2, y2, border);
            Ui.scaled(ctx, this.textRenderer, HudLayout.NAMES[id] + "  x" + String.format("%.2f", HudLayout.scale(id)),
                    x1, y1 - 9, Ui.argb(0xFF, acc), 0.8f, true);
        }

        String help = "Тяни панели мышью, колесо - размер. Esc - готово";
        int hw = this.textRenderer.getWidth(help);
        ctx.drawText(this.textRenderer, help, this.width / 2 - hw / 2, this.height / 2 - 4, 0xFFFFFFFF, true);

        // кнопка сброса
        int bw = 110;
        int bx = this.width / 2 - bw / 2;
        int by = this.height - 34;
        boolean hov = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 20;
        Ui.rrect(ctx, bx, by, bw, 20, 4, hov ? Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.5f)) : 0xFF171A21);
        String rs = "Сбросить";
        ctx.drawText(this.textRenderer, rs, bx + (bw - this.textRenderer.getWidth(rs)) / 2, by + 6, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bw = 110;
        int bx = this.width / 2 - bw / 2;
        int by = this.height - 34;
        if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + 20) {
            HudLayout.reset();
            return true;
        }
        int id = hit(mouseX, mouseY);
        if (id >= 0 && button == 0) {
            float[] r = HudLayout.rect(id, this.width, this.height);
            dragId = id;
            offX = (float) mouseX - r[0];
            offY = (float) mouseY - r[1];
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragId >= 0) {
            HudLayout.setFromRect(dragId, (float) mouseX - offX, (float) mouseY - offY, this.width, this.height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragId >= 0) {
            dragId = -1;
            VisualsConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int id = hit(mouseX, mouseY);
        if (id >= 0) {
            HudLayout.setScale(id, HudLayout.scale(id) + verticalAmount * 0.05);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
