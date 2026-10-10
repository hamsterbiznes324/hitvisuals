package com.example.hitvisuals;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Паки настроек: можно сохранить всё меню под именем и потом вернуть одним нажатием. */
public final class Profiles {
    public static final String[] BUILTIN = {"Стандартный", "Минимализм", "PvP", "Красота"};

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("hamstervisuals");
    }

    /** Оставляет в имени только буквы, цифры, пробел, дефис и подчёркивание. */
    public static String safe(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : name.trim().toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') {
                sb.append(ch);
            }
        }
        String r = sb.toString().trim();
        return r.length() > 24 ? r.substring(0, 24).trim() : r;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Path d = dir();
            if (!Files.isDirectory(d)) return out;
            try (Stream<Path> st = Files.list(d)) {
                st.forEach(p -> {
                    String fn = p.getFileName().toString();
                    if (fn.endsWith(".json")) out.add(fn.substring(0, fn.length() - 5));
                });
            }
        } catch (Exception ignored) {
        }
        Collections.sort(out);
        return out;
    }

    public static boolean save(String rawName) {
        String name = safe(rawName);
        if (name.isEmpty()) return false;
        try {
            Files.createDirectories(dir());
            VisualsConfig snap = VisualsConfig.fromJson(VisualsConfig.toJson(VisualsConfig.I));
            snap.friends = new ArrayList<>();
            snap.marks = new ArrayList<>();
            Files.writeString(dir().resolve(name + ".json"), VisualsConfig.toJson(snap));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean load(String name) {
        try {
            Path f = dir().resolve(safe(name) + ".json");
            if (!Files.exists(f)) return false;
            VisualsConfig c = VisualsConfig.fromJson(Files.readString(f));
            if (c == null) return false;
            c.clamp();
            applyConfig(c);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void delete(String name) {
        try {
            Files.deleteIfExists(dir().resolve(safe(name) + ".json"));
        } catch (Exception ignored) {
        }
    }

    private static void applyConfig(VisualsConfig c) {
        VisualsConfig cur = VisualsConfig.I;
        int oldSword = cur.swordSkin;
        int oldMace = cur.maceSkin;
        cur.copyFrom(c);
        cur.clamp();
        VisualsConfig.save();
        if (oldSword != cur.swordSkin || oldMace != cur.maceSkin) {
            SkinPacks.apply();
        }
    }

    /** Готовые наборы настроек. */
    public static void applyBuiltin(int i) {
        VisualsConfig d = new VisualsConfig();
        switch (i) {
            case 1 -> {
                d.hitEffects = false;
                d.flash = false;
                d.killEffect = false;
                d.targetHud = false;
                d.blockHighlight = false;
                d.hitMarker = false;
            }
            case 2 -> {
                d.hitParticle = 2;
                d.hitPattern = 4;
                d.particleCount = 10;
                d.hitSound = 1;
                d.customCrosshair = true;
                d.crossStyle = 3;
                d.hitMarker = true;
                d.fullbright = true;
                d.blockHighlight = false;
                d.zoomFov = 22;
            }
            case 3 -> {
                d.sky = 1;
                d.trail = true;
                d.trailParticle = 3;
                d.hitParticle = 3;
                d.hitPattern = 2;
                d.blockColorMode = 1;
                d.musicHud = true;
                d.accent = 0xFF5CD0;
            }
            default -> {
            }
        }
        applyConfig(d);
    }

    private Profiles() {}
}
