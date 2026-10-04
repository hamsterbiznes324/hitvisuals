package com.example.hitvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class HitVisualsClient implements ClientModInitializer {
    private static final long FLASH_MS = 350L;
    private static long lastHit = 0L;
    private static boolean fullbrightApplied = false;
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        VisualsConfig.load();
        ModParticles.registerFactories();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.hitvisuals"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(HitVisualsClient::tick);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient && VisualsConfig.I.hitEffects) {
                onHit(world, entity);
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (!VisualsConfig.I.flash) return;
            long elapsed = System.currentTimeMillis() - lastHit;
            if (elapsed < 0 || elapsed > FLASH_MS) return;
            float t = 1f - (elapsed / (float) FLASH_MS);
            int alpha = (int) (t * 90);
            if (alpha <= 0) return;
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                    Ui.argb(alpha, Ui.accent()));
        });
    }

    private static void tick(MinecraftClient client) {
        while (openMenu.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new VisualsScreen(null));
            }
        }

        ClientPlayerEntity p = client.player;
        if (p == null || client.world == null) {
            fullbrightApplied = false;
            return;
        }
        VisualsConfig c = VisualsConfig.I;

        // Fullbright: клиентский эффект ночного зрения
        if (c.fullbright) {
            StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 1200) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 24000, 0, false, false, false));
            }
            fullbrightApplied = true;
        } else if (fullbrightApplied) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            fullbrightApplied = false;
        }

        // Трейл за игроком
        if (c.trail) {
            double dx = p.getX() - p.prevX;
            double dy = p.getY() - p.prevY;
            double dz = p.getZ() - p.prevZ;
            if (dx * dx + dz * dz > 0.0009 || Math.abs(dy) > 0.05) {
                for (int i = 0; i < 2; i++) {
                    double ox = (client.world.random.nextDouble() - 0.5) * 0.5;
                    double oy = client.world.random.nextDouble() * 1.4;
                    double oz = (client.world.random.nextDouble() - 0.5) * 0.5;
                    client.world.addParticle(ModParticles.pick(c.trailParticle),
                            p.getX() + ox, p.getY() + 0.2 + oy, p.getZ() + oz,
                            -dx * 0.15, 0.02, -dz * 0.15);
                }
            }
        }
    }

    private static void onHit(World world, Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        VisualsConfig c = VisualsConfig.I;
        lastHit = System.currentTimeMillis();

        Box box = target.getBoundingBox();
        for (int i = 0; i < c.particleCount; i++) {
            ParticleEffect effect = ModParticles.pick(c.hitParticle);
            double x = box.minX + world.random.nextDouble() * (box.maxX - box.minX);
            double y = box.minY + world.random.nextDouble() * (box.maxY - box.minY);
            double z = box.minZ + world.random.nextDouble() * (box.maxZ - box.minZ);
            double vx = (world.random.nextDouble() - 0.5) * 0.3;
            double vy = world.random.nextDouble() * 0.25;
            double vz = (world.random.nextDouble() - 0.5) * 0.3;
            world.addParticle(effect, x, y, z, vx, vy, vz);
        }

        if (c.sound && mc.player != null) {
            world.playSound(mc.player, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
        }
    }
}
