package com.example.hitvisuals;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Свои звуки мода. Создаются прямо в коде, никаких файлов не нужно. */
public final class CustomSounds {
    public static final String[] NAMES = {"Лазер", "Монета", "Бас", "Глитч", "Вуш", "Поп", "Победа"};
    public static final int KILL = 6;

    private static final int RATE = 44100;
    private static byte[][] data;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HamsterVisuals-Sound");
        t.setDaemon(true);
        return t;
    });

    private static synchronized byte[][] data() {
        if (data == null) {
            data = new byte[][]{laser(), coin(), bass(), glitch(), whoosh(), pop(), victory()};
        }
        return data;
    }

    public static void play(int index, float volume, float pitch) {
        if (index < 0 || index >= NAMES.length) return;
        POOL.submit(() -> {
            try {
                byte[] d = data()[index];
                AudioFormat fmt = new AudioFormat(RATE * Math.max(0.5f, Math.min(2f, pitch)), 16, 1, true, false);
                final Clip clip = AudioSystem.getClip();
                clip.open(fmt, d, 0, d.length);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl g = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                    g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
                }
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            } catch (Throwable ignored) {
            }
        });
    }

    // ------------------------------------------------------------ синтез

    private static byte[] toBytes(double[] s) {
        byte[] out = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.max(-32767, Math.min(32767, s[i] * 32767.0));
            out[i * 2] = (byte) (v & 0xFF);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
        }
        return out;
    }

    private static double attack(int i) {
        return Math.min(1.0, i / (RATE * 0.004));
    }

    private static void addSweep(double[] buf, int offset, double f0, double f1, double dur,
                                 double decay, double vol) {
        int n = (int) (RATE * dur);
        double phase = 0;
        for (int i = 0; i < n && offset + i < buf.length; i++) {
            double t = i / (double) n;
            double f = f0 * Math.pow(f1 / f0, t);
            phase += 2 * Math.PI * f / RATE;
            buf[offset + i] += Math.sin(phase) * Math.exp(-decay * t) * attack(i) * vol;
        }
    }

    private static byte[] laser() {
        double[] b = new double[(int) (RATE * 0.22)];
        addSweep(b, 0, 2000, 250, 0.2, 5, 0.6);
        return toBytes(b);
    }

    private static byte[] coin() {
        double[] b = new double[(int) (RATE * 0.32)];
        addSweep(b, 0, 988, 988, 0.07, 3, 0.5);
        addSweep(b, (int) (RATE * 0.06), 1319, 1319, 0.25, 6, 0.5);
        return toBytes(b);
    }

    private static byte[] bass() {
        double[] b = new double[(int) (RATE * 0.3)];
        addSweep(b, 0, 160, 45, 0.28, 6, 0.95);
        return toBytes(b);
    }

    private static byte[] glitch() {
        int n = (int) (RATE * 0.2);
        double[] b = new double[n];
        Random r = new Random(7);
        double held = 0;
        for (int i = 0; i < n; i++) {
            if (i % 45 == 0) held = r.nextDouble() * 2 - 1;
            double sq = Math.sin(2 * Math.PI * 330 * i / RATE) > 0 ? 0.4 : -0.4;
            double env = Math.exp(-4.0 * i / n) * attack(i);
            b[i] = (held * 0.5 + sq * 0.5) * env * 0.7;
        }
        return toBytes(b);
    }

    private static byte[] whoosh() {
        int n = (int) (RATE * 0.32);
        double[] b = new double[n];
        Random r = new Random(11);
        double lp = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double a = 0.02 + 0.45 * Math.sin(Math.PI * t);
            lp += a * ((r.nextDouble() * 2 - 1) - lp);
            b[i] = lp * Math.sin(Math.PI * t) * 1.6;
        }
        return toBytes(b);
    }

    private static byte[] pop() {
        double[] b = new double[(int) (RATE * 0.08)];
        addSweep(b, 0, 900, 180, 0.07, 14, 0.85);
        return toBytes(b);
    }

    private static byte[] victory() {
        double[] notes = {523.25, 659.25, 783.99, 1046.5};
        double[] b = new double[(int) (RATE * 0.55)];
        for (int i = 0; i < notes.length; i++) {
            addSweep(b, (int) (RATE * 0.075 * i), notes[i], notes[i], 0.3, 7, 0.45);
        }
        return toBytes(b);
    }

    private CustomSounds() {}
}
