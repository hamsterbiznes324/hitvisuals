package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Меню HamsterVisuals: 5 вкладок, свёртываемые разделы, поиск и паки настроек. Открывается на Right Shift. */
public class VisualsScreen extends Screen {
    private static final String[] TABS = {"Бой", "Мир", "Игрок", "Экран", "Тема"};
    private static final String[] TAB_SUB = {
            "Удары, звуки, друзья", "Небо, свет, метки", "Руки, оружие, движение", "Прицел, плеер, панели", "Цвета и паки"
    };
    private static final String[] COLOR_MODES = {"Цвет темы", "Радужные", "Белые"};
    private static final String[] SWING_STYLES = {"Обычный", "Плавный", "Резкий"};
    private static final String[] BLOCK_COLORS = {"Цвет темы", "Радужная", "Белая"};
    private static final String[] LYRICS_STYLES = {"Буквы в мире", "Строки внизу"};
    private static final String[] PATTERNS = {"Разлёт", "Кольцо", "Спираль", "Фонтан", "Сфера"};
    private static final int[] SWATCHES = {
            0xFF3B5C, 0xFF8A3D, 0xFFD23F, 0x3DFF8A, 0x3DE0FF, 0x4D7CFF, 0xB45CFF, 0xFF5CD0, 0xF0F0F0
    };

    private static final int SIDE = 126;
    private static final int GAP = 4;
    private static final int TAB_H = 28;

    /** Какие разделы свёрнуты. Хранится, пока игра запущена. */
    private static final Set<String> COLLAPSED = new HashSet<>(List.of("Левая рука", "Панели", "Метки"));

    private final Screen parent;
    private final List<List<Row>> pages = new ArrayList<>();
    private int tab = 0;
    private double scroll = 0;
    private double scrollTarget = 0;
    private Row dragging = null;
    private TextFieldWidget search;
    private TextFieldWidget friendField;
    private TextFieldWidget profileField;
    private final long openedAt = System.currentTimeMillis();
    private long lastFrame = 0;
    private String toastText = "";
    private long toastUntil = 0;

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
        static final int SECTION = 9;
        static final int DYNAMIC = 10;

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
        Supplier<List<Row>> dyn;
        int field;
        String submitLabel = "";
        Runnable submit;
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

        static Row input(String n, String d, int field, String submitLabel, Runnable submit) {
            Row r = new Row(INPUT, n, d);
            r.field = field;
            r.submitLabel = submitLabel;
            r.submit = submit;
            return r;
        }

        static Row button(String n, String d, Runnable action) {
            Row r = new Row(BUTTON, n, d);
            r.action = action;
            return r;
        }

        static Row label(String n, String d) {
            return new Row(LABEL, n, d);
        }

        static Row section(String n) {
            return new Row(SECTION, n, "");
        }

        static Row dynamic(Supplier<List<Row>> s) {
            Row r = new Row(DYNAMIC, "", "");
            r.dyn = s;
            return r;
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

    private void toast(String s) {
        toastText = s;
        toastUntil = System.currentTimeMillis() + 1900;
    }

    private void build() {
        final VisualsConfig c = VisualsConfig.I;
        final MinecraftClient mc = MinecraftClient.getInstance();

        // ---------------------------------------------------------- 0. Бой
        List<Row> fight = new ArrayList<>();
        fight.add(Row.section("Эффекты удара"));
        fight.add(Row.toggle("Эффекты при ударе", "Частицы, вспышка и звук при ударе по цели",
                () -> c.hitEffects, v -> c.hitEffects = v));
        fight.add(Row.choice("Частицы удара", "Какие частицы вылетают из цели",
                ModParticles.NAMES, () -> c.hitParticle, v -> c.hitParticle = v));
        fight.add(Row.choice("Узор частиц", "Как частицы разлетаются",
                PATTERNS, () -> c.hitPattern, v -> c.hitPattern = v));
        fight.add(Row.slider("Количество частиц", "Сколько частиц появляется за один удар",
                1, 40, 0, () -> c.particleCount, v -> c.particleCount = (int) Math.round(v)));
        fight.add(Row.choice("Цвет частиц", "Цвет своих частиц",
                COLOR_MODES, () -> c.particleColorMode, v -> c.particleColorMode = v));
        fight.add(Row.toggle("Вспышка экрана", "Короткая вспышка цветом темы при ударе",
                () -> c.flash, v -> c.flash = v));
        fight.add(Row.toggle("Эффект убийства", "Всплеск частиц, надпись и звук, когда добиваешь цель",
                () -> c.killEffect, v -> c.killEffect = v));
        fight.add(Row.section("Звук удара"));
        fight.add(Row.toggle("Звук удара", "Включает звук при попадании",
                () -> c.sound, v -> c.sound = v));
        fight.add(Row.choice("Какой звук", "Звёздочка - свои звуки мода. Клик проигрывает звук",
                HitSounds.NAMES, () -> c.hitSound, v -> {
                    c.hitSound = v;
                    HitSounds.preview();
                }));
        fight.add(Row.slider("Громкость", "Громкость звука удара",
                0.0, 1.0, 2, () -> c.soundVolume, v -> c.soundVolume = v));
        fight.add(Row.slider("Высота звука", "Выше число, тоньше звук",
                0.5, 2.0, 2, () -> c.soundPitch, v -> c.soundPitch = v));
        fight.add(Row.section("Друзья"));
        fight.add(Row.toggle("Не бить друзей", "Удар по другу не пройдёт",
                () -> c.protectFriends, v -> c.protectFriends = v));
        fight.add(Row.toggle("Метка HamsterVisuals", "В твоих сообщениях в чате будет невидимая метка, по ней видно, что ты с модом",
                () -> c.shareTag, v -> c.shareTag = v));
        fight.add(Row.input("Добавить друга", "Впиши ник и нажми Enter", 0, "Добавить", this::addFriendFromField));
        fight.add(Row.dynamic(this::friendRows));
        pages.add(fight);

        // ---------------------------------------------------------- 1. Мир
        List<Row> world = new ArrayList<>();
        world.add(Row.section("Небо и свет"));
        world.add(Row.choice("Небо", "Готовые цвета неба и тумана",
                SkyPresets.NAMES, () -> c.sky, v -> c.sky = v));
        world.add(Row.hue("Свой цвет неба", "Работает, если выбрано «Свой цвет»",
                () -> c.skyColor, v -> c.skyColor = v));
        world.add(Row.toggle("Fullbright", "Полная яркость, тёмных мест нет",
                () -> c.fullbright, v -> c.fullbright = v));
        world.add(Row.toggle("Скрыть огонь", "Не показывать огонь на экране, когда горишь",
                () -> c.hideFire, v -> c.hideFire = v));
        world.add(Row.section("Подсветка блока"));
        world.add(Row.toggle("Подсветка блока", "Цветная рамка на блоке, на который смотришь",
                () -> c.blockHighlight, v -> c.blockHighlight = v));
        world.add(Row.choice("Цвет подсветки", "Цвет рамки и заливки блока",
                BLOCK_COLORS, () -> c.blockColorMode, v -> c.blockColorMode = v));
        world.add(Row.slider("Заливка блока", "Насколько заметно закрашен блок",
                0.0, 0.6, 2, () -> c.blockFill, v -> c.blockFill = v));
        world.add(Row.section("Метки"));
        world.add(Row.toggle("Показывать метки", "Метки видны на экране даже сквозь стены",
                () -> c.showMarks, v -> c.showMarks = v));
        world.add(Row.swatch("Цвет новых меток", "Для меток, которые поставишь дальше",
                () -> c.markColor, v -> c.markColor = v));
        world.add(Row.button("Убрать все метки здесь", "Только в текущем мире и измерении",
                () -> Marks.clearCurrent(mc)));
        world.add(Row.dynamic(this::markRows));
        pages.add(world);

        // ---------------------------------------------------------- 2. Игрок
        List<Row> player = new ArrayList<>();
        player.add(Row.section("Правая рука"));
        player.add(Row.toggle("Свои руки", "Главный выключатель: включает настройки обеих рук",
                () -> c.smallHands, v -> c.smallHands = v));
        player.add(Row.slider("Размер", "Чем меньше число, тем меньше рука",
                0.3, 1.5, 2, () -> c.handScale, v -> c.handScale = v));
        player.add(Row.slider("Сдвиг вбок", "Влево или вправо",
                -0.6, 0.6, 2, () -> c.handX, v -> c.handX = v));
        player.add(Row.slider("Сдвиг вверх", "Выше или ниже",
                -0.6, 0.6, 2, () -> c.handY, v -> c.handY = v));
        player.add(Row.slider("Сдвиг вперёд", "Ближе или дальше от глаз",
                -0.8, 0.8, 2, () -> c.handZ, v -> c.handZ = v));
        player.add(Row.slider("Поворот X", "Наклон вперёд и назад",
                -90, 90, 0, () -> c.handRotX, v -> c.handRotX = v));
        player.add(Row.slider("Поворот Y", "Поворот вокруг вертикали",
                -90, 90, 0, () -> c.handRotY, v -> c.handRotY = v));
        player.add(Row.slider("Поворот Z", "Крен руки",
                -90, 90, 0, () -> c.handRotZ, v -> c.handRotZ = v));
        player.add(Row.section("Левая рука"));
        player.add(Row.toggle("Отдельные настройки", "Выключено: левая рука зеркалит правую",
                () -> c.leftSeparate, v -> c.leftSeparate = v));
        player.add(Row.slider("Размер", "Размер левой руки",
                0.3, 1.5, 2, () -> c.leftScale, v -> c.leftScale = v));
        player.add(Row.slider("Сдвиг вбок", "Влево или вправо",
                -0.6, 0.6, 2, () -> c.leftX, v -> c.leftX = v));
        player.add(Row.slider("Сдвиг вверх", "Выше или ниже",
                -0.6, 0.6, 2, () -> c.leftY, v -> c.leftY = v));
        player.add(Row.slider("Сдвиг вперёд", "Ближе или дальше от глаз",
                -0.8, 0.8, 2, () -> c.leftZ, v -> c.leftZ = v));
        player.add(Row.slider("Поворот X", "Наклон вперёд и назад",
                -90, 90, 0, () -> c.leftRotX, v -> c.leftRotX = v));
        player.add(Row.slider("Поворот Y", "Поворот вокруг вертикали",
                -90, 90, 0, () -> c.leftRotY, v -> c.leftRotY = v));
        player.add(Row.slider("Поворот Z", "Крен руки",
                -90, 90, 0, () -> c.leftRotZ, v -> c.leftRotZ = v));
        player.add(Row.section("Поза меча"));
        player.add(Row.toggle("Меч лежит горизонтально", "Меч в руке крупно и почти горизонтально, лезвием влево. Если не так, подгони ползунками ниже",
                () -> c.swordPose, v -> c.swordPose = v));
        player.add(Row.slider("Меч: размер", "Больше число, крупнее меч",
                0.5, 2.0, 2, () -> c.swordScale, v -> c.swordScale = v));
        player.add(Row.slider("Меч: вбок", "Влево или вправо",
                -0.8, 0.8, 2, () -> c.swordX, v -> c.swordX = v));
        player.add(Row.slider("Меч: вверх", "Выше или ниже",
                -0.8, 0.8, 2, () -> c.swordY, v -> c.swordY = v));
        player.add(Row.slider("Меч: вперёд", "Ближе или дальше от глаз",
                -0.8, 0.8, 2, () -> c.swordZ, v -> c.swordZ = v));
        player.add(Row.slider("Меч: наклон X", "Лезвие к себе или от себя",
                -120, 120, 0, () -> c.swordRotX, v -> c.swordRotX = v));
        player.add(Row.slider("Меч: поворот Y", "Куда смотрит остриё",
                -120, 120, 0, () -> c.swordRotY, v -> c.swordRotY = v));
        player.add(Row.slider("Меч: крен Z", "Угол лезвия, 72 примерно горизонталь",
                -120, 120, 0, () -> c.swordRotZ, v -> c.swordRotZ = v));
        player.add(Row.button("Сбросить позу меча", "Вернуть положение меча по умолчанию", () -> {
            c.swordScale = 1.25; c.swordX = -0.05; c.swordY = 0.10; c.swordZ = 0.05;
            c.swordRotX = 0; c.swordRotY = -12; c.swordRotZ = 72;
        }));
        player.add(Row.section("Меч и скины"));
        player.add(Row.choice("Удар мечом", "Стиль взмаха от первого лица",
                SWING_STYLES, () -> c.swingStyle, v -> c.swingStyle = v));
        player.add(Row.choice("Скин меча", "Меняет вид всех мечей. Игра ненадолго перезагрузит текстуры",
                SkinPacks.NAMES, () -> c.swordSkin, v -> {
                    c.swordSkin = v;
                    VisualsConfig.save();
                    SkinPacks.apply();
                }));
        player.add(Row.choice("Скин булавы", "Меняет вид булавы",
                SkinPacks.NAMES, () -> c.maceSkin, v -> {
                    c.maceSkin = v;
                    VisualsConfig.save();
                    SkinPacks.apply();
                }));
        player.add(Row.section("Движение и обзор"));
        player.add(Row.toggle("Трейл", "След из частиц за игроком при движении",
                () -> c.trail, v -> c.trail = v));
        player.add(Row.choice("Частицы трейла", "Из чего состоит след",
                ModParticles.NAMES, () -> c.trailParticle, v -> c.trailParticle = v));
        player.add(Row.toggle("Зум", "Держи клавишу C, чтобы приблизить картинку",
                () -> c.zoomEnabled, v -> c.zoomEnabled = v));
        player.add(Row.slider("Сила зума", "Угол обзора при зуме, меньше число - ближе",
                5, 60, 0, () -> c.zoomFov, v -> c.zoomFov = (int) Math.round(v)));
        player.add(Row.toggle("Плавный зум", "Приближение и возврат идут плавно",
                () -> c.zoomSmooth, v -> c.zoomSmooth = v));
        player.add(Row.toggle("Медленнее мышь при зуме", "Чувствительность падает вместе с углом обзора",
                () -> c.zoomSens, v -> c.zoomSens = v));
        pages.add(player);

        // ---------------------------------------------------------- 3. Экран
        List<Row> screen = new ArrayList<>();
        screen.add(Row.section("Прицел"));
        screen.add(Row.toggle("Свой прицел", "Заменяет стандартный прицел",
                () -> c.customCrosshair, v -> c.customCrosshair = v));
        screen.add(Row.choice("Вид прицела", "Форма. «Свой рисунок» берётся из конструктора",
                CrosshairRenderer.STYLES, () -> c.crossStyle, v -> c.crossStyle = v));
        screen.add(Row.button("Конструктор прицела", "Нарисуй свой прицел по клеточкам",
                () -> MinecraftClient.getInstance().setScreen(new CrosshairEditorScreen(this))));
        screen.add(Row.slider("Размер", "Длина линий или радиус",
                1, 20, 0, () -> c.crossSize, v -> c.crossSize = (int) Math.round(v)));
        screen.add(Row.slider("Толщина", "Толщина линий",
                1, 6, 0, () -> c.crossThickness, v -> c.crossThickness = (int) Math.round(v)));
        screen.add(Row.slider("Просвет", "Расстояние от центра до линий",
                0, 14, 0, () -> c.crossGap, v -> c.crossGap = (int) Math.round(v)));
        screen.add(Row.slider("Размер клетки рисунка", "Для режима «Свой рисунок»",
                1, 4, 0, () -> c.crossPixelSize, v -> c.crossPixelSize = (int) Math.round(v)));
        screen.add(Row.toggle("Чёрная обводка", "Помогает видеть прицел на любом фоне",
                () -> c.crossOutline, v -> c.crossOutline = v));
        screen.add(Row.choice("Цвет прицела", "Откуда берётся цвет",
                CrosshairRenderer.COLOR_MODES, () -> c.crossColorMode, v -> c.crossColorMode = v));
        screen.add(Row.hue("Свой цвет прицела", "Работает, если выбрано «Свой цвет»",
                () -> c.crossColor, v -> c.crossColor = v));
        screen.add(Row.toggle("Расширяется в движении", "Прицел раскрывается при беге и ударах",
                () -> c.crossDynamic, v -> c.crossDynamic = v));
        screen.add(Row.toggle("Хит-маркер", "Крестик вокруг прицела при попадании",
                () -> c.hitMarker, v -> c.hitMarker = v));
        screen.add(Row.toggle("Скрыть стандартный прицел", "Убирает обычный прицел, когда включён свой",
                () -> c.hideVanillaCross, v -> c.hideVanillaCross = v));
        screen.add(Row.section("Информация на экране"));
        screen.add(Row.toggle("Водяной знак", "Название и FPS в углу экрана",
                () -> c.watermark, v -> c.watermark = v));
        screen.add(Row.toggle("Панель информации", "Координаты, пинг, CPS и счётчик ударов",
                () -> c.infoPanel, v -> c.infoPanel = v));
        screen.add(Row.toggle("Счётчик ударов", "Сколько ударов нанесено (в панели информации)",
                () -> c.hitCounter, v -> c.hitCounter = v));
        screen.add(Row.button("Обнулить счётчик ударов", "Начать счёт заново", Extras::resetHits));
        screen.add(Row.toggle("Клавиши и CPS", "WASD и кнопки мыши внизу справа",
                () -> c.keystrokes, v -> c.keystrokes = v));
        screen.add(Row.toggle("Красные края при низком HP", "Экран пульсирует красным, когда здоровья мало",
                () -> c.lowHealthFx, v -> c.lowHealthFx = v));
        screen.add(Row.section("Плашка цели"));
        screen.add(Row.toggle("WIN / LOSE", "Показывает, кто выигрывает по здоровью: ты или цель",
                () -> c.targetPredict, v -> c.targetPredict = v));
        screen.add(Row.toggle("Плашка цели", "Ник и здоровье, когда наводишься на моба или игрока",
                () -> c.targetHud, v -> c.targetHud = v));
        screen.add(Row.section("Музыка и слова"));
        screen.add(Row.toggle("Плеер в игре", "Показывает трек, который играет на компьютере",
                () -> c.musicHud, v -> c.musicHud = v));
        screen.add(Row.toggle("Слова песни", "Строки текста по ходу песни (нужен интернет)",
                () -> c.musicLyrics, v -> c.musicLyrics = v));
        screen.add(Row.choice("Как показывать слова", "Буквы в мире перед тобой или строки внизу экрана",
                LYRICS_STYLES, () -> c.lyricsStyle, v -> c.lyricsStyle = v));
        screen.add(Row.slider("Размер букв", "Размер слов, которые висят в мире",
                0.4, 2.0, 2, () -> c.lyricsSize, v -> c.lyricsSize = v));
        screen.add(Row.slider("Сдвиг слов, сек", "Если слова спешат или опаздывают, подстрой тут",
                -5.0, 5.0, 1, () -> c.lyricsOffset, v -> c.lyricsOffset = v));
        screen.add(Row.label("Как это работает",
                "Берёт трек из Spotify, браузера и других плееров. Только Windows."));
        screen.add(Row.section("Панели"));
        screen.add(Row.button("Двигать панели", "Зажми панель мышью и тяни. В игре: клавиша Right Ctrl",
                () -> MinecraftClient.getInstance().setScreen(new HudEditScreen(this))));
        screen.add(Row.button("Сбросить положение", "Вернуть панели на свои места",
                HudLayout::reset));
        screen.add(Row.slider("Плеер: размер", "Размер карточки с треком",
                0.4, 1.6, 2, () -> c.musicScale, v -> c.musicScale = v));
        screen.add(Row.slider("Плеер: по горизонтали", "Положение центра",
                0, 1, 2, () -> c.musicX, v -> c.musicX = v));
        screen.add(Row.slider("Плеер: по вертикали", "Положение верха",
                0, 1, 2, () -> c.musicY, v -> c.musicY = v));
        screen.add(Row.slider("Цель: размер", "Размер карточки со здоровьем",
                0.4, 1.6, 2, () -> c.targetScale, v -> c.targetScale = v));
        screen.add(Row.slider("Цель: по горизонтали", "Положение центра",
                0, 1, 2, () -> c.targetX, v -> c.targetX = v));
        screen.add(Row.slider("Цель: по вертикали", "Положение верха",
                0, 1, 2, () -> c.targetY, v -> c.targetY = v));
        screen.add(Row.slider("Слова: размер", "Для режима «Строки внизу»",
                0.4, 1.6, 2, () -> c.lyricsBoxScale, v -> c.lyricsBoxScale = v));
        screen.add(Row.slider("Слова: по горизонтали", "Положение центра",
                0, 1, 2, () -> c.lyricsX, v -> c.lyricsX = v));
        screen.add(Row.slider("Слова: по вертикали", "Положение верха",
                0, 1, 2, () -> c.lyricsY, v -> c.lyricsY = v));
        pages.add(screen);

        // ---------------------------------------------------------- 4. Тема
        List<Row> theme = new ArrayList<>();
        theme.add(Row.section("Цвета"));
        theme.add(Row.swatch("Цвет темы", "Цвет меню, частиц и вспышки",
                () -> c.accent, v -> c.accent = v));
        theme.add(Row.hue("Оттенок темы", "Тонкая настройка цвета",
                () -> c.accent, v -> c.accent = v));
        theme.add(Row.toggle("Своё главное меню", "Заменяет стандартный экран при запуске игры",
                () -> c.customTitle, v -> c.customTitle = v));
        theme.add(Row.section("Паки настроек"));
        theme.add(Row.input("Сохранить пак", "Впиши имя и нажми Enter. Сохранится всё, кроме друзей и меток",
                1, "Сохранить", this::saveProfileFromField));
        theme.add(Row.dynamic(this::profileRows));
        pages.add(theme);
    }

    // ------------------------------------------------------------------ живые списки

    private List<Row> friendRows() {
        List<Row> out = new ArrayList<>();
        VisualsConfig c = VisualsConfig.I;
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
        return out;
    }

    private List<Row> markRows() {
        List<Row> out = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            out.add(Row.label("Метки видны только в мире", "Открой это меню, когда зайдёшь в мир"));
            return out;
        }
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
        return out;
    }

    private List<Row> profileRows() {
        List<Row> out = new ArrayList<>();
        out.add(Row.label("Готовые наборы", "Нажми «Применить», чтобы сразу поменять настройки"));
        for (int i = 0; i < Profiles.BUILTIN.length; i++) {
            final int k = i;
            out.add(Row.item(Profiles.BUILTIN[i], "Готовый набор", false, "Применить", () -> {
                Profiles.applyBuiltin(k);
                toast("Применён набор «" + Profiles.BUILTIN[k] + "»");
            }, null, null));
        }
        List<String> saved = Profiles.list();
        out.add(Row.label("Твои паки",
                saved.isEmpty() ? "Пока пусто. Впиши имя выше и нажми «Сохранить»" : "Лежат в config/hamstervisuals"));
        for (String n : saved) {
            out.add(Row.item(n, "Свой пак", false, "Загрузить", () -> {
                if (Profiles.load(n)) toast("Загружен пак «" + n + "»");
                else toast("Не удалось загрузить");
            }, "X", () -> {
                Profiles.delete(n);
                toast("Пак «" + n + "» удалён");
            }));
        }
        return out;
    }

    private void addFriendFromField() {
        if (friendField == null) return;
        String n = friendField.getText().trim();
        if (n.isEmpty()) {
            toast("Впиши ник");
            return;
        }
        Social.addFriend(n);
        friendField.setText("");
        toast("Друг добавлен: " + n);
    }

    private void saveProfileFromField() {
        if (profileField == null) return;
        String n = Profiles.safe(profileField.getText());
        if (n.isEmpty()) {
            toast("Впиши имя пака");
            return;
        }
        if (Profiles.save(n)) {
            profileField.setText("");
            toast("Пак «" + n + "» сохранён");
        } else {
            toast("Не удалось сохранить пак");
        }
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
                    if (r.type == Row.LABEL || r.type == Row.INPUT || r.type == Row.SECTION || r.type == Row.DYNAMIC) continue;
                    if (r.name.toLowerCase(Locale.ROOT).contains(q) || r.desc.toLowerCase(Locale.ROOT).contains(q)) {
                        out.add(r);
                    }
                }
            }
            return out;
        }
        boolean open = true;
        for (Row r : pages.get(tab)) {
            if (r.type == Row.SECTION) {
                out.add(r);
                open = !COLLAPSED.contains(r.name);
                continue;
            }
            if (!open) continue;
            if (r.type == Row.DYNAMIC) out.addAll(r.dyn.get());
            else out.add(r);
        }
        return out;
    }

    // ------------------------------------------------------------------ геометрия

    private int pw() {
        return Math.min(600, this.width - 16);
    }

    private int ph() {
        return Math.min(360, this.height - 16);
    }

    private int px() {
        return (this.width - pw()) / 2;
    }

    private int py() {
        return (this.height - ph()) / 2;
    }

    private int cx0() {
        return px() + SIDE + 14;
    }

    private int cy0() {
        return py() + 58;
    }

    private int cw0() {
        return pw() - SIDE - 30;
    }

    private int ch0() {
        return ph() - 58 - 30;
    }

    private static int rowH(Row r) {
        if (r.type == Row.SECTION) return 24;
        if (r.type == Row.LABEL) return 30;
        return 34;
    }

    private static int[] tops(List<Row> rows) {
        int[] t = new int[rows.size()];
        int y = 0;
        for (int i = 0; i < t.length; i++) {
            t[i] = y;
            y += rowH(rows.get(i)) + GAP;
        }
        return t;
    }

    private static int totalH(List<Row> rows) {
        int y = 0;
        for (Row r : rows) y += rowH(r) + GAP;
        return Math.max(0, y - GAP);
    }

    private int maxScroll(List<Row> rows) {
        return Math.max(0, totalH(rows) - ch0());
    }

    private static float hueOf(int rgb) {
        return Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null)[0];
    }

    // ------------------------------------------------------------------ инициализация

    private TextFieldWidget newField(String old, int max) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, 0, 0, 120, 12, Text.literal(""));
        f.setMaxLength(max);
        f.setDrawsBackground(false);
        f.setText(old);
        return f;
    }

    @Override
    protected void init() {
        String os = search == null ? "" : search.getText();
        String of = friendField == null ? "" : friendField.getText();
        String op = profileField == null ? "" : profileField.getText();

        search = newField(os, 32);
        friendField = newField(of, 16);
        profileField = newField(op, 24);
        friendField.visible = false;
        profileField.visible = false;

        addDrawableChild(search);
        addDrawableChild(friendField);
        addDrawableChild(profileField);
    }

    // ------------------------------------------------------------------ иконки вкладок

    private void icon(DrawContext ctx, int i, int x, int y, int col) {
        switch (i) {
            case 0 -> { // скрещённые мечи
                for (int d = 0; d < 11; d++) {
                    ctx.fill(x + d, y + d, x + d + 1, y + d + 1, col);
                    ctx.fill(x + 10 - d, y + d, x + 11 - d, y + d + 1, col);
                }
                ctx.fill(x + 3, y + 7, x + 8, y + 8, col);
            }
            case 1 -> { // глобус
                for (int a = 0; a < 360; a += 20) {
                    double r = Math.toRadians(a);
                    int px = x + 5 + (int) Math.round(Math.cos(r) * 5);
                    int py = y + 5 + (int) Math.round(Math.sin(r) * 5);
                    ctx.fill(px, py, px + 1, py + 1, col);
                }
                ctx.fill(x, y + 5, x + 11, y + 6, col);
                ctx.fill(x + 5, y, x + 6, y + 11, col);
            }
            case 2 -> { // человек
                ctx.fill(x + 3, y, x + 8, y + 5, col);
                ctx.fill(x + 1, y + 6, x + 10, y + 11, col);
            }
            case 3 -> { // экран
                ctx.fill(x, y + 1, x + 11, y + 2, col);
                ctx.fill(x, y + 8, x + 11, y + 9, col);
                ctx.fill(x, y + 1, x + 1, y + 9, col);
                ctx.fill(x + 10, y + 1, x + 11, y + 9, col);
                ctx.fill(x + 4, y + 9, x + 7, y + 11, col);
            }
            default -> { // палитра
                ctx.fill(x + 1, y + 1, x + 6, y + 6, col);
                ctx.fill(x + 6, y + 1, x + 11, y + 6, Ui.argb(0xAA, col & 0xFFFFFF));
                ctx.fill(x + 3, y + 6, x + 8, y + 11, Ui.argb(0x66, col & 0xFFFFFF));
            }
        }
    }

    // ------------------------------------------------------------------ рисование

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long nowN = System.nanoTime();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (nowN - lastFrame) / 1e9f);
        lastFrame = nowN;
        float k = 1f - (float) Math.exp(-dt * 14f);
        long nowMs = System.currentTimeMillis();
        long age = nowMs - openedAt;
        float open = Math.min(1f, age / 200f);

        List<Row> rows = current();
        int[] tops = tops(rows);
        scrollTarget = Math.max(0, Math.min(maxScroll(rows), scrollTarget));
        scroll += (scrollTarget - scroll) * k;

        int acc = Ui.accent();
        int x0 = px();
        int y0 = py();
        int pw = pw();
        int ph = ph();

        ctx.fill(0, 0, this.width, this.height, Ui.argb((int) (0xA0 * open), 0x000000));

        // панель: свечение, рамка, тело и мягкий градиент
        int glow = 0x1E + (int) (0x14 * Math.sin(nowMs * 0.003));
        Ui.rrect(ctx, x0 - 3, y0 - 3, pw + 6, ph + 6, 10, Ui.argb(glow, acc));
        Ui.rrect(ctx, x0 - 1, y0 - 1, pw + 2, ph + 2, 8, Ui.argb(0x99, acc));
        Ui.rrect(ctx, x0, y0, pw, ph, 7, 0xFC0C0E13);
        ctx.fillGradient(x0 + 4, y0 + 4, x0 + pw - 4, y0 + 70, Ui.argb(0x2A, acc), 0x00000000);

        // боковая панель
        Ui.rrect(ctx, x0 + 5, y0 + 5, SIDE - 6, ph - 10, 6, 0xFF0F1118);
        ctx.fillGradient(x0 + 8, y0 + 8, x0 + SIDE - 4, y0 + 60, Ui.argb(0x2E, acc), 0x00000000);
        Sprites.drawHamster(ctx, x0 + 12, y0 + 12, 2);
        Ui.scaled(ctx, this.textRenderer, "Hamster", x0 + 48, y0 + 14, 0xFFFFFFFF, 1.15f, true);
        Ui.scaled(ctx, this.textRenderer, "Visuals", x0 + 48, y0 + 26, Ui.argb(0xFF, acc), 1.15f, true);

        for (int i = 0; i < TABS.length; i++) {
            int tx = x0 + 10;
            int ty = y0 + 52 + i * TAB_H;
            int tw = SIDE - 18;
            boolean sel = i == tab && query().isEmpty();
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= ty && mouseY < ty + TAB_H - 3;
            if (sel) {
                Ui.rrect(ctx, tx - 1, ty - 1, tw + 2, TAB_H - 1, 6, Ui.argb(0x55, acc));
                Ui.rrect(ctx, tx, ty, tw, TAB_H - 3, 5, Ui.argb(0xFF, Ui.blend(0x14161E, acc, 0.28f)));
                Ui.rrect(ctx, tx + 2, ty + 5, 3, TAB_H - 13, 1, Ui.argb(0xFF, acc));
            } else if (hov) {
                Ui.rrect(ctx, tx, ty, tw, TAB_H - 3, 5, 0xFF171A23);
            }
            int icol = sel ? Ui.argb(0xFF, acc) : (hov ? 0xFFE8EAF2 : 0xFF8A90A0);
            icon(ctx, i, tx + 11, ty + 7, icol);
            ctx.drawText(this.textRenderer, TABS[i], tx + 30, ty + 8, sel ? 0xFFFFFFFF : (hov ? 0xFFE8EAF2 : 0xFFB8BCC8), false);
        }
        Ui.scaled(ctx, this.textRenderer, "v1.4  |  Right Shift - закрыть", x0 + 12, y0 + ph - 16, 0xFF5A606E, 0.72f, false);

        // заголовок вкладки
        String q = query();
        String head = q.isEmpty() ? TABS[tab] : "Поиск";
        String sub = q.isEmpty() ? TAB_SUB[tab] : "Найдено строк: " + rows.size();
        Ui.scaled(ctx, this.textRenderer, head, cx0(), y0 + 11, 0xFFFFFFFF, 1.7f, false);
        Ui.scaled(ctx, this.textRenderer, sub, cx0(), y0 + 33, 0xFF6C7280, 0.8f, false);
        ctx.fill(cx0(), y0 + 48, cx0() + cw0(), y0 + 49, Ui.argb(0x44, acc));

        // поиск и кнопка «Сохранить»
        int sw = 130;
        int sx = x0 + pw - 12 - sw;
        int sy = y0 + 12;
        boolean sf = search.isFocused();
        if (sf) Ui.rrect(ctx, sx - 1, sy - 1, sw + 2, 22, 6, Ui.argb(0xAA, acc));
        Ui.rrect(ctx, sx, sy, sw, 20, 5, 0xFF161922);
        search.setX(sx + 8);
        search.setY(sy + 6);
        search.setWidth(sw - 16);
        search.visible = true;
        search.render(ctx, mouseX, mouseY, delta);
        if (search.getText().isEmpty() && !sf) {
            ctx.drawText(this.textRenderer, "Поиск...", sx + 8, sy + 6, 0xFF6C7280, false);
        }
        int[] sb = saveBtn();
        boolean sbh = mouseX >= sb[0] && mouseX < sb[0] + sb[2] && mouseY >= sb[1] && mouseY < sb[1] + sb[3];
        Ui.rrect(ctx, sb[0], sb[1], sb[2], sb[3], 5, Ui.argb(0xFF, Ui.blend(0x161922, acc, sbh ? 0.7f : 0.45f)));
        String sl = "Сохранить";
        ctx.drawText(this.textRenderer, sl, sb[0] + (sb[2] - this.textRenderer.getWidth(sl)) / 2, sb[1] + 6, 0xFFFFFFFF, false);

        // список
        friendField.visible = false;
        profileField.visible = false;
        int cx = cx0();
        int cy = cy0();
        int cw = cw0();
        int ch = ch0();
        ctx.enableScissor(cx, cy, cx + cw + 8, cy + ch);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            float e = Math.max(0f, Math.min(1f, (age - i * 22L) / 220f));
            e = 1f - (1f - e) * (1f - e) * (1f - e);
            int ry = cy + tops[i] - (int) scroll + (int) ((1f - e) * 10f);
            int rh = rowH(r);
            if (ry + rh < cy || ry > cy + ch) continue;
            boolean inside = mouseY >= cy && mouseY < cy + ch;
            drawRow(ctx, r, cx, ry, cw, rh, mouseX, mouseY, inside, k, delta);
        }
        ctx.disableScissor();

        if (rows.isEmpty()) {
            ctx.drawText(this.textRenderer, "Ничего не найдено", cx + 6, cy + 8, 0xFF6C7280, false);
        }

        // полоса прокрутки
        int max = maxScroll(rows);
        if (max > 0) {
            int total = totalH(rows);
            int th = Math.max(18, (int) ((double) ch * ch / total));
            int ty = cy + (int) ((ch - th) * (scroll / max));
            Ui.rrect(ctx, cx + cw + 4, cy, 3, ch, 1, 0x22FFFFFF);
            Ui.rrect(ctx, cx + cw + 4, ty, 3, th, 1, Ui.argb(0xFF, acc));
        }

        // нижняя строка
        ctx.fill(cx0(), y0 + ph - 26, cx0() + cw0(), y0 + ph - 25, 0x22FFFFFF);
        String status = VisualsConfig.I.musicHud && MusicTracker.present()
                ? "Играет: " + this.textRenderer.trimToWidth(MusicTracker.title(), 150) : "Настройки сохраняются сами";
        Ui.scaled(ctx, this.textRenderer, status, cx0(), y0 + ph - 18, 0xFF6C7280, 0.8f, false);

        // всплывающее сообщение
        if (nowMs < toastUntil && !toastText.isEmpty()) {
            long left = toastUntil - nowMs;
            int a = (int) (255 * Math.min(1f, left / 300f));
            int tw = this.textRenderer.getWidth(toastText) + 24;
            int tx = x0 + pw / 2 - tw / 2 + SIDE / 2;
            int ty = y0 + ph - 46;
            Ui.rrect(ctx, tx - 1, ty - 1, tw + 2, 24, 8, Ui.argb((int) (a * 0.7f), acc));
            Ui.rrect(ctx, tx, ty, tw, 22, 7, Ui.argb(a, 0x12141B));
            ctx.drawText(this.textRenderer, toastText, tx + 12, ty + 7, Ui.argb(a, 0xFFFFFF), false);
        }
    }

    private int[] saveBtn() {
        return new int[]{px() + pw() - 12 - 130 - 8 - 76, py() + 12, 76, 20};
    }

    private void drawRow(DrawContext ctx, Row r, int x, int y, int w, int h, int mx, int my,
                         boolean allowHover, float k, float delta) {
        int acc = Ui.accent();
        boolean hover = allowHover && mx >= x && mx < x + w && my >= y && my < y + h;
        r.hov += ((hover ? 1f : 0f) - r.hov) * k;

        if (r.type == Row.SECTION) {
            boolean closed = COLLAPSED.contains(r.name);
            Ui.rrect(ctx, x, y, w, h, 5, Ui.argb(0xFF, Ui.blend(0x12151C, acc, 0.10f + 0.10f * r.hov)));
            Ui.rrect(ctx, x, y + 4, 3, h - 8, 1, Ui.argb(0xFF, acc));
            ctx.drawText(this.textRenderer, closed ? ">" : "v", x + 10, y + 8, Ui.argb(0xFF, acc), false);
            ctx.drawText(this.textRenderer, r.name.toUpperCase(Locale.ROOT), x + 24, y + 8, Ui.argb(0xFF, acc), false);
            return;
        }

        if (r.type == Row.LABEL) {
            ctx.drawText(this.textRenderer, r.name, x + 4, y + 5, Ui.argb(0xFF, acc), false);
            Ui.scaled(ctx, this.textRenderer, this.textRenderer.trimToWidth(r.desc, (int) ((w - 8) / 0.75f)),
                    x + 4, y + 18, 0xFF8A90A0, 0.75f, false);
            return;
        }

        if (r.type == Row.TOGGLE) {
            r.anim += ((r.getB.getAsBoolean() ? 1f : 0f) - r.anim) * k;
        }
        float on = r.type == Row.TOGGLE ? r.anim : 0f;

        float glowAmt = Math.max(on * 0.6f, r.hov * 0.45f);
        if (glowAmt > 0.03f) {
            Ui.rrect(ctx, x - 1, y - 1, w + 2, h + 2, 6, Ui.argb((int) (0x70 * glowAmt), acc));
        }
        int base = Ui.blend(0x151821, 0x1D212C, r.hov);
        int bg = Ui.blend(base, Ui.blend(0x151821, acc, 0.17f), on);
        Ui.rrect(ctx, x, y, w, h, 5, Ui.argb(0xFF, bg));
        if (on > 0.02f) {
            Ui.rrect(ctx, x, y + 6, 3, h - 12, 1, Ui.argb((int) (255 * on), acc));
        }

        int nameX = x + 14;
        if (r.type == Row.ITEM && r.logo) {
            Sprites.drawHamster(ctx, x + 10, y + 9, 1);
            nameX = x + 32;
        }
        int right = x + w - 14;
        int textMax = (r.type == Row.ITEM || r.type == Row.INPUT) ? w - (nameX - x) - 120 : w - (nameX - x) - 160;
        textMax = Math.max(60, textMax);
        int nameCol = r.type == Row.BUTTON ? Ui.argb(0xFF, Ui.blend(0xEDEEF3, acc, 0.4f)) : 0xFFEDEEF3;
        ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(r.name, textMax + 50), nameX, y + 8, nameCol, false);
        Ui.scaled(ctx, this.textRenderer, this.textRenderer.trimToWidth(r.desc, (int) ((textMax + 50) / 0.75f)),
                nameX, y + 21, 0xFF8A90A0, 0.75f, false);

        int cx = x + w - 150;
        int cw = 136;

        switch (r.type) {
            case Row.TOGGLE -> {
                int bx = right - 30;
                int by = y + (h - 16) / 2;
                Ui.rrect(ctx, bx, by, 30, 16, 8, Ui.argb(0xFF, Ui.blend(0x2B2F39, acc, on)));
                int kx = bx + 2 + (int) (14 * on);
                Ui.rrect(ctx, kx, by + 2, 12, 12, 6, 0xFFFFFFFF);
            }
            case Row.CHOICE -> {
                int idx = Math.floorMod(r.getI.getAsInt(), r.options.length);
                String s = r.options[idx];
                int sw = this.textRenderer.getWidth(s);
                int chipW = sw + 38;
                int chipX = right - chipW;
                Ui.rrect(ctx, chipX, y + 8, chipW, 18, 5, Ui.argb(0xFF, Ui.blend(0x1A1E28, acc, 0.22f)));
                ctx.drawText(this.textRenderer, "<", chipX + 7, y + 13, Ui.argb(0xFF, acc), false);
                ctx.drawText(this.textRenderer, s, chipX + 19, y + 13, 0xFFFFFFFF, false);
                ctx.drawText(this.textRenderer, ">", chipX + chipW - 13, y + 13, Ui.argb(0xFF, acc), false);
            }
            case Row.SLIDER -> {
                double v = r.getD.getAsDouble();
                float f = (float) Math.max(0, Math.min(1, (v - r.min) / (r.max - r.min)));
                String vs = r.decimals == 0
                        ? String.valueOf((int) Math.round(v))
                        : String.format(Locale.ROOT, "%." + r.decimals + "f", v);
                ctx.drawText(this.textRenderer, vs, right - this.textRenderer.getWidth(vs), y + 6, 0xFFE8EAF2, false);
                Ui.rrect(ctx, cx, y + 22, cw, 5, 2, 0xFF262A35);
                Ui.rrect(ctx, cx, y + 22, Math.max(5, (int) (cw * f)), 5, 2, Ui.argb(0xFF, acc));
                Ui.rrect(ctx, cx + (int) (cw * f) - 4, y + 19, 8, 11, 3, 0xFFFFFFFF);
            }
            case Row.HUE -> {
                int rgb = r.getI.getAsInt();
                for (int i = 0; i < cw; i += 2) {
                    int col = Color.HSBtoRGB(i / (float) cw, 0.7f, 1f) & 0xFFFFFF;
                    ctx.fill(cx + i, y + 21, cx + Math.min(i + 2, cw), y + 29, Ui.argb(0xFF, col));
                }
                int kx = cx + (int) (hueOf(rgb) * (cw - 3));
                ctx.fill(kx - 1, y + 18, kx + 4, y + 32, 0xFFFFFFFF);
                ctx.fill(kx, y + 19, kx + 3, y + 31, Ui.argb(0xFF, rgb));
                Ui.rrect(ctx, right - 12, y + 4, 12, 12, 4, Ui.argb(0xFF, rgb));
            }
            case Row.SWATCH -> {
                int cur = r.getI.getAsInt() & 0xFFFFFF;
                int total = SWATCHES.length * 18 - 4;
                int sx = right - total;
                int sy = y + (h - 14) / 2;
                for (int i = 0; i < SWATCHES.length; i++) {
                    int px = sx + i * 18;
                    if (SWATCHES[i] == cur) {
                        Ui.rrect(ctx, px - 2, sy - 2, 18, 18, 5, 0xFFFFFFFF);
                    }
                    Ui.rrect(ctx, px, sy, 14, 14, 4, Ui.argb(0xFF, SWATCHES[i]));
                }
            }
            case Row.BUTTON -> {
                int bx = right - 22;
                Ui.rrect(ctx, bx, y + 8, 22, 18, 5, Ui.argb(0xFF, Ui.blend(0x1A1E28, acc, 0.35f + 0.3f * r.hov)));
                ctx.drawText(this.textRenderer, ">", bx + 8, y + 13, 0xFFFFFFFF, false);
            }
            case Row.INPUT -> {
                TextFieldWidget f = r.field == 0 ? friendField : profileField;
                int[] b = inputButton(x, w, y);
                int boxW = 130;
                int boxX = b[0] - 8 - boxW;
                Ui.rrect(ctx, boxX, y + 7, boxW, 20, 5, 0xFF0C0E13);
                if (f.isFocused()) {
                    Ui.rrect(ctx, boxX - 1, y + 6, boxW + 2, 22, 6, Ui.argb(0x88, acc));
                    Ui.rrect(ctx, boxX, y + 7, boxW, 20, 5, 0xFF0C0E13);
                }
                f.setX(boxX + 7);
                f.setY(y + 13);
                f.setWidth(boxW - 14);
                f.visible = true;
                f.render(ctx, mx, my, delta);
                if (f.getText().isEmpty() && !f.isFocused()) {
                    ctx.drawText(this.textRenderer, r.field == 0 ? "Ник..." : "Имя пака...", boxX + 7, y + 13, 0xFF6C7280, false);
                }
                Ui.rrect(ctx, b[0], b[1], b[2], b[3], 5, Ui.argb(0xFF, Ui.blend(0x1A1E28, acc, 0.55f)));
                ctx.drawText(this.textRenderer, r.submitLabel,
                        b[0] + (b[2] - this.textRenderer.getWidth(r.submitLabel)) / 2, b[1] + 6, 0xFFFFFFFF, false);
            }
            case Row.ITEM -> {
                if (r.b2 != null) {
                    int[] b = itemButton(x, w, y, 2, r);
                    Ui.rrect(ctx, b[0], b[1], b[2], b[3], 5, 0xFF5A2530);
                    ctx.drawText(this.textRenderer, r.b2, b[0] + (b[2] - this.textRenderer.getWidth(r.b2)) / 2,
                            b[1] + 6, 0xFFFFFFFF, false);
                }
                if (r.b1 != null) {
                    int[] b = itemButton(x, w, y, 1, r);
                    Ui.rrect(ctx, b[0], b[1], b[2], b[3], 5, Ui.argb(0xFF, Ui.blend(0x1A1E28, acc, 0.55f)));
                    ctx.drawText(this.textRenderer, r.b1, b[0] + (b[2] - this.textRenderer.getWidth(r.b1)) / 2,
                            b[1] + 6, 0xFFFFFFFF, false);
                }
            }
            default -> {
            }
        }
    }

    private int[] inputButton(int x, int w, int y) {
        return new int[]{x + w - 14 - 84, y + 7, 84, 20};
    }

    /** idx 2 - правая кнопка (удалить), idx 1 - левее неё. */
    private int[] itemButton(int x, int w, int y, int idx, Row r) {
        int bw2 = r.b2 == null ? 0 : 24;
        if (idx == 2) {
            return new int[]{x + w - 12 - bw2, y + 7, bw2, 20};
        }
        int bw1 = this.textRenderer.getWidth(r.b1) + 16;
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

        if (search.isMouseOver(mouseX, mouseY) || friendField.isMouseOver(mouseX, mouseY)
                || profileField.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        this.setFocused(null);

        if (in(mx, my, saveBtn())) {
            VisualsConfig.save();
            toast("Настройки сохранены");
            return true;
        }

        for (int i = 0; i < TABS.length; i++) {
            int tx = px() + 10;
            int ty = py() + 52 + i * TAB_H;
            if (mx >= tx && mx < tx + SIDE - 18 && my >= ty && my < ty + TAB_H - 3) {
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
            int[] tops = tops(rows);
            for (int i = 0; i < rows.size(); i++) {
                int ry = cy0() + tops[i] - (int) scroll;
                int rh = rowH(rows.get(i));
                if (my >= ry && my < ry + rh) {
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
            case Row.SECTION -> {
                if (!COLLAPSED.remove(r.name)) COLLAPSED.add(r.name);
                return true;
            }
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
                    r.submit.run();
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
        scrollTarget = Math.max(0, Math.min(maxScroll(current()), scrollTarget - verticalAmount * 28));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = (search != null && search.isFocused())
                || (friendField != null && friendField.isFocused())
                || (profileField != null && profileField.isFocused());
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && !typing) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (friendField != null && friendField.isFocused()) {
                addFriendFromField();
                return true;
            }
            if (profileField != null && profileField.isFocused()) {
                saveProfileFromField();
                return true;
            }
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
