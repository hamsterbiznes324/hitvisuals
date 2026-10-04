package com.example.hitvisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки мода. Сохраняются в config/hitvisuals.json */
public final class VisualsConfig {
    public static boolean hitEffects = true;
    public static int particle = 0;
    public static int particleCount = 12;
    public static boolean flash = true;
    public static boolean sound = true;
    public static int sky = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hitvisuals.json");

    private static class Data {
        boolean hitEffects = true;
        int particle = 0;
        int particleCount = 12;
        boolean flash = true;
        boolean sound = true;
        int sky = 0;
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            hitEffects = d.hitEffects;
            particle = d.particle;
            particleCount = Math.max(1, Math.min(40, d.particleCount));
            flash = d.flash;
            sound = d.sound;
            sky = d.sky;
        } catch (Exception e) {
            System.err.println("[HitVisuals] Не удалось прочитать конфиг: " + e);
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.hitEffects = hitEffects;
            d.particle = particle;
            d.particleCount = particleCount;
            d.flash = flash;
            d.sound = sound;
            d.sky = sky;
            Files.writeString(FILE, GSON.toJson(d));
        } catch (Exception e) {
            System.err.println("[HitVisuals] Не удалось сохранить конфиг: " + e);
        }
    }

    private VisualsConfig() {}
}
