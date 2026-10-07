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
    public boolean killEffect = true;

    // мир
    public int sky = 0;
    public int skyColor = 0x5A2DFF;
    public boolean fullbright = false;
    public boolean hideFire = false;
    public boolean blockHighlight = true;
    public int blockColorMode = 0;
    public double blockFill = 0.15;

    // игрок
    public boolean trail = false;
    public int trailParticle = 3;
    public boolean targetHud = true;
    public boolean zoomEnabled = true;
    public int zoomFov = 25;
    public boolean zoomSmooth = true;
    public boolean zoomSens = true;

    // руки и оружие
    public boolean smallHands = false;
    public double handScale = 0.6;
    public double handX = 0;
    public double handY = 0;
    public double handZ = 0;
    public double handRotX = 0;
    public double handRotY = 0;
    public double handRotZ = 0;
    public int swingStyle = 0;
    public int swordSkin = 0;
    public int maceSkin = 0;

    // прицел
    public boolean customCrosshair = false;
    public int crossStyle = 0;
    public int crossSize = 6;
    public int crossThickness = 1;
    public int crossGap = 3;
    public boolean crossOutline = true;
    public int crossColorMode = 0;
    public int crossColor = 0xFFFFFF;
    public boolean crossDynamic = true;
    public boolean hitMarker = true;
    public boolean hideVanillaCross = true;

    // музыка
    public boolean musicHud = false;
    public boolean musicLyrics = true;
    public int lyricsStyle = 0;
    public double lyricsSize = 1.0;
    public double lyricsOffset = 0.0;

    // положение панелей (x - центр, y - верх, в долях экрана)
    public double musicX = 0.5;
    public double musicY = 0.012;
    public double musicScale = 0.75;
    public double targetX = 0.60;
    public double targetY = 0.56;
    public double targetScale = 0.70;
    public double lyricsX = 0.5;
    public double lyricsY = 0.78;
    public double lyricsBoxScale = 1.0;

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

    private static int ci(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double cd(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void clamp() {
        accent &= 0xFFFFFF;
        skyColor &= 0xFFFFFF;
        markColor &= 0xFFFFFF;
        crossColor &= 0xFFFFFF;
        hitParticle = ci(hitParticle, 0, ModParticles.NAMES.length - 1);
        trailParticle = ci(trailParticle, 0, ModParticles.NAMES.length - 1);
        particleCount = ci(particleCount, 1, 40);
        particleColorMode = ci(particleColorMode, 0, 2);
        hitSound = ci(hitSound, 0, HitSounds.NAMES.length - 1);
        soundVolume = cd(soundVolume, 0.0, 1.0);
        soundPitch = cd(soundPitch, 0.5, 2.0);
        sky = ci(sky, 0, SkyPresets.NAMES.length - 1);
        blockColorMode = ci(blockColorMode, 0, 2);
        blockFill = cd(blockFill, 0.0, 0.6);
        zoomFov = ci(zoomFov, 5, 60);
        handScale = cd(handScale, 0.3, 1.5);
        handX = cd(handX, -0.6, 0.6);
        handY = cd(handY, -0.6, 0.6);
        handZ = cd(handZ, -0.8, 0.8);
        handRotX = cd(handRotX, -90, 90);
        handRotY = cd(handRotY, -90, 90);
        handRotZ = cd(handRotZ, -90, 90);
        swingStyle = ci(swingStyle, 0, 2);
        swordSkin = ci(swordSkin, 0, SkinPacks.NAMES.length - 1);
        maceSkin = ci(maceSkin, 0, SkinPacks.NAMES.length - 1);
        crossStyle = ci(crossStyle, 0, 4);
        crossSize = ci(crossSize, 1, 20);
        crossThickness = ci(crossThickness, 1, 6);
        crossGap = ci(crossGap, 0, 14);
        crossColorMode = ci(crossColorMode, 0, 3);
        lyricsStyle = ci(lyricsStyle, 0, 1);
        lyricsSize = cd(lyricsSize, 0.4, 2.0);
        lyricsOffset = cd(lyricsOffset, -5.0, 5.0);
        musicScale = cd(musicScale, 0.4, 1.6);
        targetScale = cd(targetScale, 0.4, 1.6);
        lyricsBoxScale = cd(lyricsBoxScale, 0.4, 1.6);
        musicX = cd(musicX, 0, 1);
        musicY = cd(musicY, 0, 1);
        targetX = cd(targetX, 0, 1);
        targetY = cd(targetY, 0, 1);
        lyricsX = cd(lyricsX, 0, 1);
        lyricsY = cd(lyricsY, 0, 1);
        if (friends == null) friends = new ArrayList<>();
        if (marks == null) marks = new ArrayList<>();
    }
}
