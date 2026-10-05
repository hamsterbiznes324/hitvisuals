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

/** Разные звуки при ударе. */
public final class HitSounds {
    public static final String[] NAMES = {
            "Звон", "Крит", "Колокольчик", "Стрела", "Левел-ап", "Телепорт", "Салют", "Гром", "Случайный"
    };

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

    public static void play(World world, PlayerEntity player, Entity target) {
        play(world, player, target.getX(), target.getY(), target.getZ());
    }

    /** Проигрывает звук рядом с игроком, чтобы услышать выбор в меню. */
    public static void preview() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        play(mc.world, p, p.getX(), p.getY(), p.getZ());
    }

    private static void play(World world, PlayerEntity player, double x, double y, double z) {
        VisualsConfig c = VisualsConfig.I;
        int idx = c.hitSound;
        if (idx >= IDS.length) {
            idx = (int) (Math.random() * IDS.length);
        }
        SoundEvent ev = Registries.SOUND_EVENT.get(Identifier.of("minecraft", IDS[idx]));
        if (ev == null) return;
        float vol = (float) c.soundVolume;
        float pitch = (float) c.soundPitch * BASE_PITCH[idx];
        world.playSound(player, x, y, z, ev, SoundCategory.PLAYERS, vol, pitch);
    }

    private HitSounds() {}
}
