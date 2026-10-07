package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

/** Меню HamsterVisuals: вкладки слева, настройки справа, поиск сверху. Открывается на Right Shift. */
public class VisualsScreen extends Screen {
    private static final String[] TABS = {
            "Удары", "Мир", "Игрок", "Руки и меч", "Прицел", "Музыка", "Друзья", "Метки", "Панели", "Тема"
    };
    private static final String[] TAB_TITLES = {
            "Удары", "Мир", "Игрок", "Руки и оружие", "Прицел", "Музыка и слова", "Друзья", "Метки",
            "Маленькие панели", "Тема и меню"
    };
    private static final String[] BLOCK_COLORS = {"Цвет темы", "Радужная", "Белая"};
    private static final String[] LYRICS_STYLES = {"Буквы в мире", "Строки внизу"};
    private static final String[] COLOR_MODES = {"Цвет темы", "Радужные", "Белые"};
    private static final String[] SWING_STYLES = {"Обычный", "Плавный", "Резкий"};
    private static final int[] SWATCHES = {
            0xFF3B5C, 0xFF8A3D, 0xFFD23F, 0x3DFF8A, 0x3DE0FF, 0x4D7CFF, 0xB45CFF, 0xFF5CD0, 0xF0F0F0
    };

    private static final int SIDE = 118;
    private static final int ROW_H = 34;
    private static final int GAP = 4;
    private static final int TAB_H = 20;

    private final Screen parent;
    private final List<List<Row>> pages = new ArrayList<>();
    private int tab = 0;
    private double scroll = 0;
    private double scrollTarget = 0;
    private Row dragging = null;
    private TextFieldWidget search;
    private TextFieldWidget friendField;
    private final long openedAt = System.currentTimeMillis();
    private long lastFrame = 0;

    public VisualsScreen(Screen parent) {
        super(Text.literal("HamsterVisuals"));
        this.parent = parent;
        build();
    }

    // ------------------------------------------------------------------ строки меню

    private static final class Row {
        static final int TOGGLE = 0;
        static final int SLIDER = 1;
        static final int CHOICE = 2;
        static final int HUE = 3;
        static final int SWATCH = 4;
        static final int INPUT = 5;
        static final int ITEM = 6;
        static final int BUTTON = 7;
        static final int LABEL = 8;

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
        Runnable action;
        boolean logo;
        String b1;
        Runnable r1;
        String b2;
        Runnable r2;
        float anim = 0f;
        float hov = 0f;

        Row(int type, String name, String desc) {
            this.type = type;
            this.name = name;
            this.desc = desc == null ? "" : desc;
        }

        static Row toggle(String n, String d, BooleanSupplier g, Consumer<Boolean> s) {
            Row r = new Row(TOGGLE, n, d);
            r.getB = g;
            r.setB = s;
            r.anim = g.getAsBoolean() ? 1f : 0f;
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

        static Row input(String n, String d) {
            return new Row(INPUT, n, d);
        }

        static Row button(String n, String d, Runnable action) {
            Row r = new Row(BUTTON, n, d);
            r.action = action;
            return r;
        }

        static Row label(String n, String d) {
            return new Row(LABEL, n, d);
        }

        static Row item(String n, String d, boolean logo, String b1, Runnable r1, String b2, Runnable r2) {
            Row r = new Row(ITEM, n, d);
            r.logo = logo;
            r.b1 = b1;
            r.r1 = r1;
            r.b2 = b2;
            r.r2 = r2;
            return r;
        }
    }

    private void build() {
        final VisualsConfig c = VisualsConfig.I;

        // 0 - Удары
        pages.add(List.of(
                Row.toggle("Эффекты при ударе", "Частицы, вспышка и звук при ударе по цели",
                        () -> c.hitEffects, v -> c.hitEffects = v),
                Row.choice("Частицы удара", "Какие частицы вылетают из цели",
                        ModParticles.NAMES, () -> c.hitParticle, v -> c.hitParticle = v),
                Row.slider("Количество частиц", "Сколько частиц появляется за один удар",
                        1, 40, 0, () -> c.particleCount, v -> c.particleCount = (int) Math.round(v)),
                Row.choice("Цвет частиц", "Цвет звёзд, лун и черепов",
                        COLOR_MODES, () -> c.particleColorMode, v -> c.particleColorMode = v),
                Row.toggle("Звук удара", "Включает звук при попадании",
                        () -> c.sound, v -> c.sound = v),
                Row.choice("Какой звук", "Звёздочка - свои звуки мода. Клик проигрывает звук",
                        HitSounds.NAMES, () -> c.hitSound, v -> {
                            c.hitSound = v;
                            HitSounds.preview();
                        }),
                Row.slider("Громкость звука", "Громкость звука удара",
                        0.0, 1.0, 2, () -> c.soundVolume, v -> c.soundVolume = v),
                Row.slider("Высота звука", "Выше число, тоньше звук",
                        0.5, 2.0, 2, () -> c.soundPitch, v -> c.soundPitch = v),
                Row.toggle("Вспышка экрана", "Короткая вспышка цветом темы при ударе",
                        () -> c.flash, v -> c.flash = v),
                Row.toggle("Эффект убийства", "Всплеск черепов, надпись и звук, когда добиваешь цель",
                        () -> c.killEffect, v -> c.killEffect = v)
        ));

        // 1 - Мир
        pages.add(List.of(
                Row.choice("Небо", "Готовые цвета неба и тумана",
                        SkyPresets.NAMES, () -> c.sky, v -> c.sky = v),
                Row.hue("Свой цвет неба", "Работает, если выбрано «Свой цвет»",
                        () -> c.skyColor, v -> c.skyColor = v),
                Row.toggle("Подсветка блока", "Цветная рамка на блоке, на который смотришь",
                        () -> c.blockHighlight, v -> c.blockHighlight = v),
                Row.choice("Цвет подсветки", "Цвет рамки и заливки блока",
                        BLOCK_COLORS, () -> c.blockColorMode, v -> c.blockColorMode = v),
                Row.slider("Заливка блока", "Насколько заметно закрашен блок",
                        0.0, 0.6, 2, () -> c.blockFill, v -> c.blockFill = v),
                Row.toggle("Fullbright", "Полная яркость, тёмных мест нет",
                        () -> c.fullbright, v -> c.fullbright = v),
                Row.toggle("Скрыть огонь", "Не показывать огонь на экране, когда горишь",
                        () -> c.hideFire, v -> c.hideFire = v)
        ));

        // 2 - Игрок
        pages.add(List.of(
                Row.toggle("Плашка цели", "Ник и здоровье, когда наводишься на моба или игрока",
                        () -> c.targetHud, v -> c.targetHud = v),
                Row.toggle("Трейл", "След из частиц за игроком при движении",
                        () -> c.trail, v -> c.trail = v),
                Row.choice("Частицы трейла", "Из чего состоит след",
                        ModParticles.NAMES, () -> c.trailParticle, v -> c.trailParticle = v),
                Row.toggle("Зум", "Держи клавишу C, чтобы приблизить картинку",
                        () -> c.zoomEnabled, v -> c.zoomEnabled = v),
                Row.slider("Сила зума", "Угол обзора при зуме, меньше число - ближе",
                        5, 60, 0, () -> c.zoomFov, v -> c.zoomFov = (int) Math.round(v)),
                Row.toggle("Плавный зум", "Приближение и возврат идут плавно",
                        () -> c.zoomSmooth, v -> c.zoomSmooth = v),
                Row.toggle("Медленнее мышь при зуме", "Чувствительность падает вместе с углом обзора",
                        () -> c.zoomSens, v -> c.zoomSens = v)
        ));

        // 3 - Руки и меч
        pages.add(List.of(
                Row.toggle("Своя рука", "Включает все настройки руки ниже",
                        () -> c.smallHands, v -> c.smallHands = v),
                Row.slider("Размер руки", "Чем меньше число, тем меньше рука",
                        0.3, 1.5, 2, () -> c.handScale, v -> c.handScale = v),
                Row.slider("Сдвиг вбок", "Влево или вправо",
                        -0.6, 0.6, 2, () -> c.handX, v -> c.handX = v),
                Row.slider("Сдвиг вверх", "Выше или ниже",
                        -0.6, 0.6, 2, () -> c.handY, v -> c.handY = v),
                Row.slider("Сдвиг вперёд", "Ближе или дальше от глаз",
                        -0.8, 0.8, 2, () -> c.handZ, v -> c.handZ = v),
                Row.slider("Поворот X", "Наклон вперёд и назад",
                        -90, 90, 0, () -> c.handRotX, v -> c.handRotX = v),
                Row.slider("Поворот Y", "Поворот вокруг вертикали",
                        -90, 90, 0, () -> c.handRotY, v -> c.handRotY = v),
                Row.slider("Поворот Z", "Крен руки",
                        -90, 90, 0, () -> c.handRotZ, v -> c.handRotZ = v),
                Row.choice("Удар мечом", "Стиль взмаха от первого лица",
                        SWING_STYLES, () -> c.swingStyle, v -> c.swingStyle = v),
                Row.choice("Скин меча", "Меняет вид всех мечей. Игра ненадолго перезагрузит текстуры",
                        SkinPacks.NAMES, () -> c.swordSkin, v -> {
                            c.swordSkin = v;
                            VisualsConfig.save();
                            SkinPacks.apply();
                        }),
                Row.choice("Скин булавы", "Меняет вид булавы",
                        SkinPacks.NAMES, () -> c.maceSkin, v -> {
                            c.maceSkin = v;
                            VisualsConfig.save();
                            SkinPacks.apply();
                        })
        ));

        // 4 - Прицел
        pages.add(List.of(
                Row.toggle("Свой прицел", "Заменяет стандартный прицел",
                        () -> c.customCrosshair, v -> c.customCrosshair = v),
                Row.choice("Вид прицела", "Форма прицела",
                        CrosshairRenderer.STYLES, () -> c.crossStyle, v -> c.crossStyle = v),
                Row.slider("Размер", "Длина линий или радиус",
                        1, 20, 0, () -> c.crossSize, v -> c.crossSize = (int) Math.round(v)),
                Row.slider("Толщина", "Толщина линий",
                        1, 6, 0, () -> c.crossThickness, v -> c.crossThickness = (int) Math.round(v)),
                Row.slider("Просвет", "Расстояние от центра до линий",
                        0, 14, 0, () -> c.crossGap, v -> c.crossGap = (int) Math.round(v)),
                Row.toggle("Чёрная обводка", "Обводка помогает видеть прицел на любом фоне",
                        () -> c.crossOutline, v -> c.crossOutline = v),
                Row.choice("Цвет прицела", "Откуда берётся цвет",
                        CrosshairRenderer.COLOR_MODES, () -> c.crossColorMode, v -> c.crossColorMode = v),
                Row.hue("Свой цвет прицела", "Работает, если выбрано «Свой цвет»",
                        () -> c.crossColor, v -> c.crossColor = v),
                Row.toggle("Расширяется в движении", "Прицел раскрывается при беге и ударах",
                        () -> c.crossDynamic, v -> c.crossDynamic = v),
                Row.toggle("Хит-маркер", "Крестик вокруг прицела при попадании",
                        () -> c.hitMarker, v -> c.hitMarker = v),
                Row.toggle("Скрыть стандартный прицел", "Убирает обычный прицел, когда включён свой",
                        () -> c.hideVanillaCross, v -> c.hideVanillaCross = v)
        ));

        // 5 - Музыка
        pages.add(List.of(
                Row.toggle("Плеер в игре", "Показывает трек, который играет на компьютере",
                        () -> c.musicHud, v -> c.musicHud = v),
                Row.toggle("Слова песни", "Строки текста по ходу песни (нужен интернет)",
                        () -> c.musicLyrics, v -> c.musicLyrics = v),
                Row.choice("Как показывать слова", "Буквы в мире перед тобой или строки внизу экрана",
                        LYRICS_STYLES, () -> c.lyricsStyle, v -> c.lyricsStyle = v),
                Row.slider("Размер букв", "Размер слов, которые висят в мире",
                        0.4, 2.0, 2, () -> c.lyricsSize, v -> c.lyricsSize = v),
                Row.slider("Сдвиг слов, сек", "Если слова спешат или опаздывают, подстрой тут",
                        -5.0, 5.0, 1, () -> c.lyricsOffset, v -> c.lyricsOffset = v),
                Row.label("Как это работает",
                        "Берёт трек из Spotify, браузера и других плееров. Только Windows.")
        ));

        // 6 - Друзья (дальше добавляются живые строки)
        pages.add(List.of(
                Row.toggle("Не бить друзей", "Удар по другу не пройдёт",
                        () -> c.protectFriends, v -> c.protectFriends = v),
                Row.toggle("Метка HamsterVisuals", "Твои сообщения в чате несут невидимую метку, по ней видно, что ты с модом",
                        () -> c.shareTag, v -> c.shareTag = v),
                Row.input("Добавить друга", "Впиши ник и нажми Enter")
        ));

        // 7 - Метки
        pages.add(List.of(
                Row.toggle("Показывать метки", "Метки видны на экране даже сквозь стены",
                        () -> c.showMarks, v -> c.showMarks = v),
                Row.swatch("Цвет новых меток", "Для меток, которые поставишь дальше",
                        () -> c.markColor, v -> c.markColor = v),
                Row.button("Убрать все метки здесь", "Только в текущем мире и измерении",
                        () -> Marks.clearCurrent(MinecraftClient.getInstance()))
        ));

        // 8 - Панели
        pages.add(List.of(
                Row.button("Двигать панели", "Открывает экран, где панели можно перетащить мышью",
                        () -> MinecraftClient.getInstance().setScreen(new HudEditScreen(this))),
                Row.button("Сбросить положение", "Вернуть панели на свои места",
                        HudLayout::reset),
                Row.slider("Размер плеера", "Размер карточки с треком",
                        0.4, 1.6, 2, () -> c.musicScale, v -> c.musicScale = v),
                Row.slider("Размер плашки цели", "Размер карточки со здоровьем",
                        0.4, 1.6, 2, () -> c.targetScale, v -> c.targetScale = v),
                Row.slider("Размер строк слов", "Для режима «Строки внизу»",
                        0.4, 1.6, 2, () -> c.lyricsBoxScale, v -> c.lyricsBoxScale = v),
                Row.label("Быстрый доступ",
                        "Клавиша Right Ctrl в игре. Её можно поменять в настройках управления.")
        ));

        // 9 - Тема
        pages.add(List.of(
                Row.swatch("Цвет темы", "Цвет меню, частиц и вспышки",
                        () -> c.accent, v -> c.accent = v),
                Row.hue("Оттенок темы", "Тонкая настройка цвета",
                        () -> c.accent, v -> c.accent = v),
                Row.toggle("Своё главное меню", "Заменяет стандартный экран при запуске игры",
                        () -> c.customTitle, v -> c.customTitle = v)
        ));
    }

    private List<Row> dynamic(int t) {
        List<Row> out = new ArrayList<>();
        VisualsConfig c = VisualsConfig.I;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (t == 6) {
            if (c.friends.isEmpty()) {
                out.add(Row.label("Друзей пока нет", "Впиши ник выше или наведись на игрока и нажми G"));
            }
            for (String f : new ArrayList<>(c.friends)) {
                out.add(Row.item(f, "Друг", Social.isModUser(f), null, null, "X", () -> Social.removeFriend(f)));
            }
            List<String> users = Social.modUsers();
            out.add(Row.label("Игроки с HamsterVisuals",
                    users.isEmpty() ? "Появятся, когда напишут в чат" : "Узнаём по метке в чате"));
            for (String u : users) {
                out.add(Row.item(u, "Играет с HamsterVisuals", true,
                        Social.isFriend(u) ? null : "+ друг", () -> Social.addFriend(u), null, null));
            }
        } else if (t == 7) {
            if (mc.world == null) {
                out.add(Row.label("Метки видны только в мире", "Открой это меню, когда зайдёшь в мир"));
            } else {
                String key = HitVisualsClient.markKey == null ? "B"
                        : HitVisualsClient.markKey.getBoundKeyLocalizedText().getString();
                List<VisualsConfig.Mark> marks = Marks.current(mc);
                if (marks.isEmpty()) {
                    out.add(Row.label("Меток нет", "Смотри на место и нажми " + key));
                }
                for (VisualsConfig.Mark m : new ArrayList<>(marks)) {
                    String where = String.format(Locale.ROOT, "%d, %d, %d", (int) m.x, (int) m.y, (int) m.z);
                    out.add(Row.item(m.name, where, false, null, null, "X", () -> Marks.remove(m)));
                }
            }
        }
        return out;
    }

    private String query() {
        return search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
    }

    private List<Row> current() {
        List<Row> out = new ArrayList<>();
        String q = query();
        if (!q.isEmpty()) {
            for (List<Row> page : pages) {
                for (Row r : page) {
                    if (r.type == Row.LABEL || r.type == Row.INPUT) continue;
                    if (r.name.toLowerCase(Locale.ROOT).contains(q) || r.desc.toLowerCase(Locale.ROOT).contains(q)) {
                        out.add(r);
                    }
                }
            }
            return out;
        }
        out.addAll(pages.get(tab));
        out.addAll(dynamic(tab));
        return out;
    }

    // ------------------------------------------------------------------ геометрия

    private int pw() {
        return Math.min(510, this.width - 16);
    }

    private int ph() {
        return Math.min(316, this.height - 16);
    }

    private int px() {
        return (this.width - pw()) / 2;
    }

    private int py() {
        return (this.height - ph()) / 2;
    }

    private int cx0() {
        return px() + SIDE + 10;
    }

    private int cy0() {
        return py() + 52;
    }

    private int cw0() {
        return pw() - SIDE - 20;
    }

    private int ch0() {
        return ph() - 62;
    }

    private int maxScroll(int rows) {
        int total = rows * (ROW_H + GAP) - GAP;
        return Math.max(0, total - ch0());
    }

    private static float hueOf(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null)[0];
    }

    // ------------------------------------------------------------------ инициализация

    @Override
    protected void init() {
        String oldSearch = search == null ? "" : search.getText();
        String oldFriend = friendField == null ? "" : friendField.getText();

        search = new TextFieldWidget(this.textRenderer, 0, 0, 120, 12, Text.literal(""));
        search.setMaxLength(32);
        search.setDrawsBackground(false);
        search.setText(oldSearch);

        friendField = new TextFieldWidget(this.textRenderer, 0, 0, 120, 12, Text.literal(""));
        friendField.setMaxLength(16);
        friendField.setDrawsBackground(false);
        friendField.setText(oldFriend);
        friendField.visible = false;

        addDrawableChild(search);
        addDrawableChild(friendField);
    }

    private void addFriendFromField() {
        if (friendField == null) return;
        Social.addFriend(friendField.getText());
        friendField.setText("");
    }

    // ------------------------------------------------------------------ рисование

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long nowN = System.nanoTime();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (nowN - lastFrame) / 1e9f);
        lastFrame = nowN;
        float k = 1f - (float) Math.exp(-dt * 14f);
        long age = System.currentTimeMillis() - openedAt;
        float open = Math.min(1f, age / 200f);

        List<Row> rows = current();
        scrollTarget = Math.max(0, Math.min(maxScroll(rows.size()), scrollTarget));
        scroll += (scrollTarget - scroll) * k;

        int acc = Ui.accent();
        int x0 = px();
        int y0 = py();
        int pw = pw();
        int ph = ph();

        ctx.fill(0, 0, this.width, this.height, Ui.argb((int) (0x99 * open), 0x000000));

        // панель
        Ui.rrect(ctx, x0 - 1, y0 - 1, pw + 2, ph + 2, 8, Ui.argb(0x77, acc));
        Ui.rrect(ctx, x0, y0, pw, ph, 7, 0xFA0E1015);
        Ui.rrect(ctx, x0 + 4, y0 + 4, SIDE - 4, ph - 8, 5, 0xFF12141A);

        // логотип
        Sprites.drawHamster(ctx, x0 + 10, y0 + 10, 1);
        ctx.drawText(this.textRenderer, "Hamster", x0 + 30, y0 + 12, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, "Visuals", x0 + 30, y0 + 22, Ui.argb(0xFF, acc), false);

        // вкладки
        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 8;
            int ty = y0 + 40 + i * TAB_H;
            boolean sel = i == tab && query().isEmpty();
            boolean hov = mouseX >= tx && mouseX < tx + SIDE - 16 && mouseY >= ty && mouseY < ty + TAB_H - 2;
            if (sel) {
                Ui.rrect(ctx, tx, ty, SIDE - 16, TAB_H - 2, 4, Ui.argb(0x33, acc));
                Ui.rrect(ctx, tx, ty + 4, 3, TAB_H - 10, 1, Ui.argb(0xFF, acc));
            } else if (hov) {
                Ui.rrect(ctx, tx, ty, SIDE - 16, TAB_H - 2, 4, 0x22FFFFFF);
            }
            String label = TABS[i];
            if (i == 6 && !VisualsConfig.I.friends.isEmpty()) label += " (" + VisualsConfig.I.friends.size() + ")";
            ctx.drawText(this.textRenderer, label, tx + 12, ty + 6,
                    sel ? Ui.argb(0xFF, acc) : 0xFFB8BCC8, false);
        }
        if (ph >= 290) {
            Ui.scaled(ctx, this.textRenderer, "Right Shift - закрыть", x0 + 12, y0 + ph - 16, 0xFF6C7280, 0.75f, false);
        }

        // заголовок
        String q = query();
        String head = q.isEmpty() ? TAB_TITLES[tab] : "Поиск";
        Ui.scaled(ctx, this.textRenderer, head, cx0(), y0 + 10, 0xFFFFFFFF, 1.5f, false);
        Ui.scaled(ctx, this.textRenderer, "Клик - переключить, правая кнопка - назад по списку",
                cx0(), y0 + 28, 0xFF6C7280, 0.75f, false);

        // поиск
        int sx = x0 + pw - 10 - 140;
        int sy = y0 + 10;
        boolean sf = search.isFocused();
        if (sf) Ui.rrect(ctx, sx - 1, sy - 1, 142, 20, 5, Ui.argb(0xAA, acc));
        Ui.rrect(ctx, sx, sy, 140, 18, 4, 0xFF171A21);
        search.setX(sx + 8);
        search.setY(sy + 5);
        search.setWidth(124);
        search.visible = true;
        search.render(ctx, mouseX, mouseY, delta);
        if (search.getText().isEmpty() && !sf) {
            ctx.drawText(this.textRenderer, "Поиск...", sx + 8, sy + 5, 0xFF6C7280, false);
        }

        // список
        friendField.visible = false;
        int cx = cx0();
        int cy = cy0();
        int cw = cw0();
        int ch = ch0();
        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            float e = Math.max(0f, Math.min(1f, (age - i * 25L) / 220f));
            e = 1f - (1f - e) * (1f - e) * (1f - e);
            int ry = cy + i * (ROW_H + GAP) - (int) scroll + (int) ((1f - e) * 10f);
            if (ry + ROW_H < cy || ry > cy + ch) continue;
            boolean inside = mouseY >= cy && mouseY < cy + ch;
            drawRow(ctx, r, cx, ry, cw, mouseX, mouseY, inside, k, delta);
        }
        ctx.disableScissor();

        if (rows.isEmpty()) {
            ctx.drawText(this.textRenderer, "Ничего не найдено", cx + 6, cy + 8, 0xFF6C7280, false);
        }

        // полоса прокрутки
        int max = maxScroll(rows.size());
        if (max > 0) {
            int total = rows.size() * (ROW_H + GAP) - GAP;
            int th = Math.max(16, (int) ((double) ch * ch / total));
            int ty = cy + (int) ((ch - th) * (scroll / max));
            Ui.rrect(ctx, cx + cw + 3, cy, 3, ch, 1, 0x22FFFFFF);
            Ui.rrect(ctx, cx + cw + 3, ty, 3, th, 1, Ui.argb(0xFF, acc));
        }
    }

    private void drawRow(DrawContext ctx, Row r, int x, int y, int w, int mx, int my,
                         boolean allowHover, float k, float delta) {
        int acc = Ui.accent();
        boolean hover = allowHover && mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        r.hov += ((hover ? 1f : 0f) - r.hov) * k;

        if (r.type == Row.LABEL) {
            ctx.drawText(this.textRenderer, r.name, x + 4, y + 7, Ui.argb(0xFF, acc), false);
            Ui.scaled(ctx, this.textRenderer, this.textRenderer.trimToWidth(r.desc, (int) ((w - 8) / 0.75f)),
                    x + 4, y + 20, 0xFF8A90A0, 0.75f, false);
            return;
        }

        if (r.type == Row.TOGGLE) {
            r.anim += ((r.getB.getAsBoolean() ? 1f : 0f) - r.anim) * k;
        }
        float on = r.type == Row.TOGGLE ? r.anim : 0f;

        int base = Ui.blend(0x171A21, 0x20242E, r.hov);
        int bg = Ui.blend(base, Ui.blend(0x171A21, acc, 0.16f), on);
        Ui.rrect(ctx, x, y, w, ROW_H, 4, Ui.argb(0xFF, bg));
        if (on > 0.02f) {
            Ui.rrect(ctx, x, y + 5, 3, ROW_H - 10, 1, Ui.argb((int) (255 * on), acc));
        }

        int nameX = x + 12;
        if (r.type == Row.ITEM && r.logo) {
            Sprites.drawHamster(ctx, x + 10, y + 9, 1);
            nameX = x + 32;
        }
        int right = x + w - 14;
        int textMax = (r.type == Row.ITEM ? w - (nameX - x) - 100 : w - (nameX - x) - 160);
        ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(r.name, Math.max(40, textMax + 40)),
                nameX, y + 7, 0xFFEDEEF3, false);
        Ui.scaled(ctx, this.textRenderer,
                this.textRenderer.trimToWidth(r.desc, (int) (Math.max(40, textMax + 40) / 0.75f)),
                nameX, y + 20, 0xFF8A90A0, 0.75f, false);

        int cx = x + w - 150;
        int cw = 136;

        switch (r.type) {
            case Row.TOGGLE -> {
                int bx = right - 28;
                int by = y + (ROW_H - 14) / 2;
                Ui.rrect(ctx, bx, by, 28, 14, 7, Ui.argb(0xFF, Ui.blend(0x2B2F39, acc, on)));
                int kx = bx + 2 + (int) (14 * on);
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
            case Row.BUTTON -> ctx.drawText(this.textRenderer, ">", right - 6, y + 13, Ui.argb(0xFF, acc), false);
            case Row.INPUT -> {
                int fx = nameX + 0;
                int fw = w - 12 - 96;
                // поле ввода рисуется справа от подписи, поэтому подпись сжимаем
                int boxX = x + w - 14 - 78 - 8 - 120;
                Ui.rrect(ctx, boxX, y + 7, 120, 20, 4, 0xFF0E1015);
                friendField.setX(boxX + 6);
                friendField.setY(y + 13);
                friendField.setWidth(108);
                friendField.visible = true;
                friendField.render(ctx, mx, my, delta);
                if (friendField.getText().isEmpty() && !friendField.isFocused()) {
                    ctx.drawText(this.textRenderer, "Ник...", boxX + 6, y + 13, 0xFF6C7280, false);
                }
                int[] b = inputButton(x, w, y);
                Ui.rrect(ctx, b[0], b[1], b[2], b[3], 4, Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.55f)));
                String bl = "Добавить";
                ctx.drawText(this.textRenderer, bl, b[0] + (b[2] - this.textRenderer.getWidth(bl)) / 2, b[1] + 6,
                        0xFFFFFFFF, false);
            }
            case Row.ITEM -> {
                if (r.b2 != null) {
                    int[] b = itemButton(x, w, y, 2, r);
                    Ui.rrect(ctx, b[0], b[1], b[2], b[3], 4, 0xFF5A2530);
                    ctx.drawText(this.textRenderer, r.b2, b[0] + (b[2] - this.textRenderer.getWidth(r.b2)) / 2,
                            b[1] + 6, 0xFFFFFFFF, false);
                }
                if (r.b1 != null) {
                    int[] b = itemButton(x, w, y, 1, r);
                    Ui.rrect(ctx, b[0], b[1], b[2], b[3], 4, Ui.argb(0xFF, Ui.blend(0x171A21, acc, 0.55f)));
                    ctx.drawText(this.textRenderer, r.b1, b[0] + (b[2] - this.textRenderer.getWidth(r.b1)) / 2,
                            b[1] + 6, 0xFFFFFFFF, false);
                }
            }
            default -> {
            }
        }
    }

    private int[] inputButton(int x, int w, int y) {
        return new int[]{x + w - 14 - 78, y + 7, 78, 20};
    }

    /** idx 2 - правая кнопка (удалить), idx 1 - левее неё. */
    private int[] itemButton(int x, int w, int y, int idx, Row r) {
        int bw2 = r.b2 == null ? 0 : 24;
        if (idx == 2) {
            return new int[]{x + w - 12 - bw2, y + 7, bw2, 20};
        }
        int bw1 = this.textRenderer.getWidth(r.b1) + 14;
        return new int[]{x + w - 12 - bw2 - (bw2 > 0 ? 6 : 0) - bw1, y + 7, bw1, 20};
    }

    private static boolean in(int mx, int my, int[] b) {
        return mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
    }

    // ------------------------------------------------------------------ мышь и клавиши

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (search.isMouseOver(mouseX, mouseY) || friendField.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        this.setFocused(null);

        for (int i = 0; i < TABS.length; i++) {
            int tx = px() + 8;
            int ty = py() + 40 + i * TAB_H;
            if (mx >= tx && mx < tx + SIDE - 16 && my >= ty && my < ty + TAB_H - 2) {
                tab = i;
                scroll = 0;
                scrollTarget = 0;
                dragging = null;
                search.setText("");
                return true;
            }
        }

        if (mx >= cx0() && mx < cx0() + cw0() && my >= cy0() && my < cy0() + ch0()) {
            List<Row> rows = current();
            for (int i = 0; i < rows.size(); i++) {
                int ry = cy0() + i * (ROW_H + GAP) - (int) scroll;
                if (my >= ry && my < ry + ROW_H) {
                    if (clickRow(rows.get(i), mx, my, ry, button)) return true;
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickRow(Row r, int mx, int my, int ry, int button) {
        int x = cx0();
        int w = cw0();
        switch (r.type) {
            case Row.TOGGLE -> {
                r.setB.accept(!r.getB.getAsBoolean());
                return true;
            }
            case Row.CHOICE -> {
                int n = r.options.length;
                int v = r.getI.getAsInt() + (button == 1 ? -1 : 1);
                r.setI.accept(Math.floorMod(v, n));
                return true;
            }
            case Row.SLIDER, Row.HUE -> {
                if (mx >= x + w - 156) {
                    dragging = r;
                    drag(r, mx);
                    return true;
                }
                return false;
            }
            case Row.SWATCH -> {
                int total = SWATCHES.length * 18 - 4;
                int sx = x + w - 14 - total;
                int rel = mx - sx;
                if (rel >= 0 && rel < total && rel % 18 < 14) {
                    r.setI.accept(SWATCHES[rel / 18]);
                    return true;
                }
                return false;
            }
            case Row.BUTTON -> {
                r.action.run();
                return true;
            }
            case Row.INPUT -> {
                if (in(mx, my, inputButton(x, w, ry))) {
                    addFriendFromField();
                    return true;
                }
                return false;
            }
            case Row.ITEM -> {
                if (r.b2 != null && in(mx, my, itemButton(x, w, ry, 2, r))) {
                    r.r2.run();
                    return true;
                }
                if (r.b1 != null && in(mx, my, itemButton(x, w, ry, 1, r))) {
                    r.r1.run();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
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
        scrollTarget = Math.max(0, Math.min(maxScroll(current().size()), scrollTarget - verticalAmount * 24));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = (search != null && search.isFocused()) || (friendField != null && friendField.isFocused());
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !typing) {
            close();
            return true;
        }
        if (friendField != null && friendField.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            addFriendFromField();
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
