package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/** Звуки удара: звуки игры, свои звуки мода (со звёздочкой) и случайный. */
public final class HitSounds {
    private static final String[] IDS = {
            "entity.experience_orb.pickup",
            "entity.player.attack.crit",
            "block.amethyst_block.chime",
            "entity.arrow.hit_player",
            "entity.player.levelup",
            "entity.enderman.teleport",
            "entity.firework_rocket.blast",
            "entity.lightning_bolt.thunder"
    };
    private static final float[] BASE_PITCH = {1.4f, 1.0f, 1.0f, 1.0f, 1.0f, 1.2f, 1.0f, 1.0f};
    private static final String[] VANILLA_NAMES = {
            "Звон", "Крит", "Колокольчик", "Стрела", "Левел-ап", "Телепорт", "Салют", "Гром"
    };

    public static final String[] NAMES = buildNames();

    private static String[] buildNames() {
        String[] out = new String[VANILLA_NAMES.length + CustomSounds.NAMES.length + 1];
        int k = 0;
        for (String s : VANILLA_NAMES) out[k++] = s;
        for (String s : CustomSounds.NAMES) out[k++] = s + " *";
        out[k] = "Случайный";
        return out;
    }

    public static void play(World world, PlayerEntity player, Entity target) {
        play(world, player, target.getX(), target.getY(), target.getZ());
    }

    /** Проигрывает выбранный звук, чтобы услышать его в меню. */
    public static void preview() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            playCustomOnly();
            return;
        }
        play(mc.world, p, p.getX(), p.getY(), p.getZ());
    }

    private static void playCustomOnly() {
        int idx = VisualsConfig.I.hitSound - VANILLA_NAMES.length;
        if (idx >= 0 && idx < CustomSounds.NAMES.length) {
            CustomSounds.play(idx, (float) VisualsConfig.I.soundVolume, (float) VisualsConfig.I.soundPitch);
        }
    }

    private static void play(World world, PlayerEntity player, double x, double y, double z) {
        VisualsConfig c = VisualsConfig.I;
        int idx = c.hitSound;
        int random = NAMES.length - 1;
        if (idx >= random) {
            idx = (int) (Math.random() * random);
        }
        if (idx >= IDS.length) {
            CustomSounds.play(idx - IDS.length, (float) c.soundVolume, (float) c.soundPitch);
            return;
        }
        SoundEvent ev = Registries.SOUND_EVENT.get(Identifier.of("minecraft", IDS[idx]));
        if (ev == null) return;
        float vol = (float) c.soundVolume;
        float pitch = (float) c.soundPitch * BASE_PITCH[idx];
        world.playSound(player, x, y, z, ev, SoundCategory.PLAYERS, vol, pitch);
    }

    private HitSounds() {}
}
