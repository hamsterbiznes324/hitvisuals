package com.example.hitvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class HitVisualsClient implements ClientModInitializer {
    public static final String[] PARTICLE_NAMES = {
            "Крит", "Огонь", "Сердечки", "Звёздный свет", "Тотем", "Искры"
    };

    private static final ParticleEffect[] PARTICLES = {
            ParticleTypes.ENCHANTED_HIT,
            ParticleTypes.FLAME,
            ParticleTypes.HEART,
            ParticleTypes.END_ROD,
            ParticleTypes.TOTEM_OF_UNDYING,
            ParticleTypes.ELECTRIC_SPARK
    };

    private static final long FLASH_MS = 350L;
    private static long lastHit = 0L;
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        VisualsConfig.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.hitvisuals"
        ));

        // Открытие меню на Right Shift
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new VisualsScreen());
                }
            }
        });

        // Эффекты при ударе
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && VisualsConfig.hitEffects) {
                onHit(world, entity);
            }
            return ActionResult.PASS;
        });

        // Вспышка экрана
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!VisualsConfig.flash) return;
            long elapsed = System.currentTimeMillis() - lastHit;
            if (elapsed < 0 || elapsed > FLASH_MS) return;
            float t = 1f - (elapsed / (float) FLASH_MS);
            int alpha = (int) (t * 90);
            if (alpha <= 0) return;
            int color = (alpha << 24) | 0xFFFFFF;
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
        });
    }

    private static void onHit(World world, Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        lastHit = System.currentTimeMillis();

        ParticleEffect effect = PARTICLES[VisualsConfig.particle % PARTICLES.length];
        Box box = target.getBoundingBox();
        for (int i = 0; i < VisualsConfig.particleCount; i++) {
            double x = box.minX + world.random.nextDouble() * (box.maxX - box.minX);
            double y = box.minY + world.random.nextDouble() * (box.maxY - box.minY);
            double z = box.minZ + world.random.nextDouble() * (box.maxZ - box.minZ);
            double vx = (world.random.nextDouble() - 0.5) * 0.3;
            double vy = world.random.nextDouble() * 0.25;
            double vz = (world.random.nextDouble() - 0.5) * 0.3;
            world.addParticle(effect, x, y, z, vx, vy, vz);
        }

        if (VisualsConfig.sound && mc.player != null) {
            world.playSound(mc.player, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
    }
}
