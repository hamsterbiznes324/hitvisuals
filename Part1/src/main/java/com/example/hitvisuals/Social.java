package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Друзья и игроки, которые тоже играют с HamsterVisuals. */
public final class Social {
    /** Невидимая метка, которая дописывается к твоим сообщениям в чате. */
    public static final String TAG = "\u200B\u200C\u200B\u200D";

    private static final Set<String> MOD_USERS = new LinkedHashSet<>();
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public static boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : VisualsConfig.I.friends) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static void addFriend(String name) {
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty() || name.length() > 16 || isFriend(name)) return;
        VisualsConfig.I.friends.add(name);
        VisualsConfig.save();
    }

    public static void removeFriend(String name) {
        VisualsConfig.I.friends.removeIf(f -> f.equalsIgnoreCase(name));
        VisualsConfig.save();
    }

    /** Возвращает true, если после вызова игрок в друзьях. */
    public static boolean toggleFriend(String name) {
        if (isFriend(name)) {
            removeFriend(name);
            return false;
        }
        addFriend(name);
        return true;
    }

    /** Запоминает игрока по тексту его имени из чата (скобки и префиксы отбрасываются). */
    public static void noteModUserFromText(String text) {
        if (text == null) return;
        Matcher m = NAME.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group();
        }
        if (last != null) {
            MOD_USERS.add(last);
        }
    }

    public static boolean isModUser(String name) {
        if (name == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() != null && name.equalsIgnoreCase(mc.getSession().getUsername())) {
            return true;
        }
        String low = name.toLowerCase(Locale.ROOT);
        for (String u : MOD_USERS) {
            if (u.toLowerCase(Locale.ROOT).equals(low)) return true;
        }
        return false;
    }

    public static List<String> modUsers() {
        return new ArrayList<>(MOD_USERS);
    }

    private Social() {}
}
