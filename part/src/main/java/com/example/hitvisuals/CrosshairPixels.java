package com.example.hitvisuals;

/** Данные прицела, нарисованного игроком: сетка 15 на 15, каждый символ - клетка. */
public final class CrosshairPixels {
    public static final int N = 15;
    /** Символы: 0 - пусто, 1 - цвет темы, 2..9 - цвета из палитры. */
    public static final int[] PALETTE = {
            0xF0F0F0, 0xFF3B5C, 0xFF8A3D, 0xFFD23F, 0x3DFF8A, 0x3DE0FF, 0x4D7CFF, 0xFF5CD0
    };

    public static String empty() {
        return "0".repeat(N * N);
    }

    public static String plus() {
        char[] a = empty().toCharArray();
        int c = N / 2;
        a[c * N + c] = '1';
        for (int i = 2; i <= 5; i++) {
            a[c * N + (c - i)] = '1';
            a[c * N + (c + i)] = '1';
            a[(c - i) * N + c] = '1';
            a[(c + i) * N + c] = '1';
        }
        return new String(a);
    }

    public static String dot() {
        char[] a = empty().toCharArray();
        int c = N / 2;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                a[(c + dy) * N + (c + dx)] = '1';
            }
        }
        return new String(a);
    }

    public static String ring() {
        char[] a = empty().toCharArray();
        int c = N / 2;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double d = Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
                if (d >= 4.6 && d <= 5.7) a[y * N + x] = '1';
            }
        }
        a[c * N + c] = '1';
        return new String(a);
    }

    public static String normalize(String s) {
        return s == null || s.length() != N * N ? plus() : s;
    }

    /** Цвет клетки в RGB или -1, если клетка пустая. */
    public static int rgb(char ch) {
        if (ch == '1') return Ui.accent();
        if (ch >= '2' && ch <= '9') return PALETTE[ch - '2'];
        return -1;
    }

    private CrosshairPixels() {}
}
