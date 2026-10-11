package com.example.hitvisuals;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Скины для мечей и булавы. Внутри мода лежат ресурспаки, меню включает и выключает нужный. */
public final class SkinPacks {
    public static final String[] NAMES = {"Обычный", "Огненный", "Ледяной", "Тёмный", "Золотой", "Радужный", "Хомяк"};
    private static final String[] IDS = {"", "fire", "ice", "void", "gold", "rainbow", "hamster"};

    /** Регистрирует встроенные ресурспаки (вызывается при запуске). */
    public static void register() {
        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("hitvisuals");
        if (mod.isEmpty()) return;
        for (int i = 1; i < IDS.length; i++) {
            ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of("hitvisuals", "hv_skin_sword_" + IDS[i]), mod.get(), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of("hitvisuals", "hv_skin_mace_" + IDS[i]), mod.get(), ResourcePackActivationType.NORMAL);
        }
    }

    private static String find(ResourcePackManager m, String kind, int skin) {
        if (skin <= 0 || skin >= IDS.length) return null;
        String needle = "hv_skin_" + kind + "_" + IDS[skin];
        for (ResourcePackProfile p : m.getProfiles()) {
            if (p.getId().contains(needle)) return p.getId();
        }
        return null;
    }

    /** Включает выбранные скины и перезагружает ресурсы. */
    public static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            ResourcePackManager m = mc.getResourcePackManager();
            m.scanPacks();
            List<String> enabled = new ArrayList<>(m.getEnabledIds());
            enabled.removeIf(id -> id.contains("hv_skin_"));
            String sword = find(m, "sword", VisualsConfig.I.swordSkin);
            String mace = find(m, "mace", VisualsConfig.I.maceSkin);
            if (sword != null) enabled.add(sword);
            if (mace != null) enabled.add(mace);
            m.setEnabledProfiles(enabled);
            mc.options.refreshResourcePacks(m);
        } catch (Throwable t) {
            System.err.println("[HamsterVisuals] Не удалось применить скин: " + t);
        }
    }

    private SkinPacks() {}
}
