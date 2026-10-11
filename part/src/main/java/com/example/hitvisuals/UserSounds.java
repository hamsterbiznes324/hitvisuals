package com.example.hitvisuals;

import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Свои звуки удара: положи .wav файлы в config/hamstervisuals/sounds. */
public final class UserSounds {
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HamsterVisuals-usersound");
        t.setDaemon(true);
        return t;
    });

    public static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("hamstervisuals").resolve("sounds");
        try {
            Files.createDirectories(d);
        } catch (Exception ignored) {
        }
        return d;
    }

    /** Имена всех .wav файлов в папке. */
    public static List<String> list() {
        List<String> out = new ArrayList<>();
        File[] fs = dir().toFile().listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".wav")) out.add(f.getName());
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static void openFolder() {
        try {
            File d = dir().toFile();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) new ProcessBuilder("explorer", d.getAbsolutePath()).start();
            else if (os.contains("mac")) new ProcessBuilder("open", d.getAbsolutePath()).start();
            else new ProcessBuilder("xdg-open", d.getAbsolutePath()).start();
        } catch (Exception ignored) {
        }
    }

    /** Проигрывает выбранный файл. Возвращает false, если файла нет. */
    public static boolean play(String name, float volume) {
        if (name == null || name.isEmpty()) return false;
        File f = dir().resolve(name).toFile();
        if (!f.isFile()) return false;
        POOL.execute(() -> {
            try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl g = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                    g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
                }
                clip.addLineListener(ev -> {
                    if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) clip.close();
                });
                clip.start();
            } catch (Exception ignored) {
            }
        });
        return true;
    }

    private UserSounds() {}
}
