package com.example.hitvisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Все настройки мода. Хранятся в config/hitvisuals.json */
public final class VisualsConfig {
    public static VisualsConfig I = new VisualsConfig();

    /** Метка в мире. */
    public static class Mark {
        public String world = "";
        public String dim = "";
        public String name = "";
        public double x;
        public double y;
        public double z;
        public int color = 0xFF3B5C;
    }

    // тема
    public int accent = 0xB45CFF;
    public boolean customTitle = true;

    // удары
    public boolean hitEffects = true;
    public int hitParticle = 3;
    public int particleCount = 12;
    public int particleColorMode = 0;
    public boolean flash = true;
    public boolean sound = true;
    public int hitSound = 0;
    public double soundVolume = 0.8;
    public double soundPitch = 1.0;

    // мир
    public int sky = 0;
    public int skyColor = 0x5A2DFF;
    public boolean fullbright = false;
    public boolean hideFire = false;

    // игрок
    public boolean trail = false;
    public int trailParticle = 3;
    public boolean smallHands = false;
    public double handScale = 0.6;
    public int swingStyle = 0;
    public boolean targetHud = true;

    // музыка
    public boolean musicHud = false;
    public boolean musicLyrics = true;

    // друзья и метки
    public boolean protectFriends = true;
    public boolean shareTag = true;
    public List<String> friends = new ArrayList<>();
    public boolean showMarks = true;
    public int markColor = 0xFF3B5C;
    public List<Mark> marks = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hitvisuals.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            VisualsConfig c = GSON.fromJson(Files.readString(f), VisualsConfig.class);
            if (c != null) {
                c.clamp();
                I = c;
            }
        } catch (Exception e) {
            System.err.println("[HamsterVisuals] Не удалось прочитать конфиг: " + e);
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(I));
        } catch (Exception e) {
            System.err.println("[HamsterVisuals] Не удалось сохранить конфиг: " + e);
        }
    }

    private void clamp() {
        accent &= 0xFFFFFF;
        skyColor &= 0xFFFFFF;
        markColor &= 0xFFFFFF;
        hitParticle = Math.max(0, Math.min(ModParticles.NAMES.length - 1, hitParticle));
        trailParticle = Math.max(0, Math.min(ModParticles.NAMES.length - 1, trailParticle));
        particleCount = Math.max(1, Math.min(40, particleCount));
        particleColorMode = Math.max(0, Math.min(2, particleColorMode));
        hitSound = Math.max(0, Math.min(HitSounds.NAMES.length - 1, hitSound));
        soundVolume = Math.max(0.0, Math.min(1.0, soundVolume));
        soundPitch = Math.max(0.5, Math.min(2.0, soundPitch));
        sky = Math.max(0, Math.min(SkyPresets.NAMES.length - 1, sky));
        swingStyle = Math.max(0, Math.min(2, swingStyle));
        handScale = Math.max(0.3, Math.min(1.0, handScale));
        if (friends == null) friends = new ArrayList<>();
        if (marks == null) marks = new ArrayList<>();
    }
}
