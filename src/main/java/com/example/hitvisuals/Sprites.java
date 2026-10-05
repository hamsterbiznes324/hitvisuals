package com.example.hitvisuals;

import net.minecraft.client.gui.DrawContext;

/** Пиксельные иконки: 0 - звезда, 1 - луна, 2 - череп. Хомяк - логотип HamsterVisuals. */
public final class Sprites {
    public static final String[][] DATA = {
        {
            "................",
            ".......##.......",
            ".......##.......",
            "......####......",
            "......####......",
            ".##############.",
            "..############..",
            "...##########...",
            "....########....",
            "....########....",
            "...####..####...",
            "...###....###...",
            "..###......###..",
            "..##........##..",
            "................",
            "................"
        },
        {
            "................",
            "...########.....",
            "..#######.......",
            ".######.........",
            "######..........",
            "######..........",
            "#####...........",
            "#####...........",
            "#####...........",
            "#####...........",
            "######..........",
            "######..........",
            ".######.........",
            "..#######.......",
            "...########.....",
            "................"
        },
        {
            "................",
            "....########....",
            "..############..",
            ".##############.",
            ".##############.",
            ".##...####...##.",
            ".##...####...##.",
            ".###.######.###.",
            ".######..######.",
            "..############..",
            "...###.##.###...",
            "...#.#.##.#.#...",
            "....#.#..#.#....",
            "................",
            "................",
            "................"
        }
    };

    public static final String[] HAMSTER = {
            "................",
            "...oo......oo...",
            "..oppo....oppo..",
            ".ofppfoooofppfo.",
            "offffffffffffffo",
            "offffffffffffffo",
            "offkkffffffkkffo",
            "offkkffffffkkffo",
            "offffffppffffffo",
            "offwwwwkkwwwwffo",
            "ofwwwwwwwwwwwwfo",
            ".owwwwwwwwwwwwo.",
            "..oowwwwwwwwoo..",
            "....oooooooo....",
            "................",
            "................"
        };

    public static void draw(DrawContext ctx, int sprite, int x, int y, int px, int color) {
        String[] rows = DATA[Math.floorMod(sprite, DATA.length)];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) == '#') {
                    int s = c;
                    while (c < row.length() && row.charAt(c) == '#') c++;
                    ctx.fill(x + s * px, y + r * px, x + c * px, y + (r + 1) * px, color);
                } else {
                    c++;
                }
            }
        }
    }

    private static int hamsterColor(char ch) {
        switch (ch) {
            case 'o': return 0xFF4A3224;
            case 'f': return 0xFFE5A15C;
            case 'p': return 0xFFFF9FB0;
            case 'k': return 0xFF1B1410;
            case 'w': return 0xFFFFF0D8;
            default: return 0;
        }
    }

    /** Цветной логотип-хомяк (16x16 пикселей, умноженных на px). */
    public static void drawHamster(DrawContext ctx, int x, int y, int px) {
        for (int r = 0; r < HAMSTER.length; r++) {
            String row = HAMSTER[r];
            int c = 0;
            while (c < row.length()) {
                char ch = row.charAt(c);
                if (ch == '.') {
                    c++;
                    continue;
                }
                int s = c;
                while (c < row.length() && row.charAt(c) == ch) c++;
                ctx.fill(x + s * px, y + r * px, x + c * px, y + (r + 1) * px, hamsterColor(ch));
            }
        }
    }

    private Sprites() {}
}
