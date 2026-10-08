package com.example.hitvisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class HitVisualsClient implements ClientModInitializer {
    private static boolean fullbrightApplied = false;
    private static boolean hintShown = false;

    private static LivingEntity lastHitEntity = null;
    private static long lastHitEntityTime = 0L;
    private static int killStreak = 0;
    private static long lastKillTime = 0L;

    public static KeyBinding openMenu;
    public static KeyBinding markKey;
    public static KeyBinding friendKey;
    public static KeyBinding zoomKey;
    public static KeyBinding hudEditKey;

    @Override
    public void onInitializeClient() {
        VisualsConfig.load();
        ModParticles.registerFactories();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.hitvisuals"));
        markKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.mark", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.hitvisuals"));
        friendKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.friend", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.hitvisuals"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.hitvisuals"));
        hudEditKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hitvisuals.hudedit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.hitvisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(HitVisualsClient::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            Zoom.restore();
            MusicTracker.shutdown();
        });

        WorldRenderEvents.START.register(ctx -> Zoom.update(MinecraftClient.getInstance()));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BlockHighlight::render);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (VisualsConfig.I.protectFriends && entity instanceof PlayerEntity pe
                    && Social.isFriend(pe.getName().getString())) {
                return ActionResult.FAIL;
            }
            if (world.isClient && VisualsConfig.I.hitEffects) {
                onHit(world, entity);
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> HudRenderer.render(context));

        // Невидимая метка в чате, чтобы другие игроки с HamsterVisuals видели, что ты тоже с модом
        ClientSendMessageEvents.MODIFY_CHAT.register(msg -> VisualsConfig.I.shareTag ? msg + Social.TAG : msg);
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) -> {
            if (message.getString().contains(Social.TAG)) {
                Social.noteModUserFromText(params.name().getString());
            }
        });
    }

    private static void tick(MinecraftClient client) {
        VisualsConfig c = VisualsConfig.I;
        MusicTracker.update(c.musicHud);

        // Своё главное меню (запасной способ, если подмена экрана не сработала)
        if (c.customTitle && client.currentScreen instanceof TitleScreen) {
            client.setScreen(new CustomTitleScreen());
        }

        while (openMenu.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new VisualsScreen(null));
            }
        }
        while (hudEditKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new HudEditScreen(null));
            }
        }

        ClientPlayerEntity p = client.player;
        if (p == null || client.world == null) {
            fullbrightApplied = false;
            lastHitEntity = null;
            hintShown = false;
            return;
        }
        if (!hintShown) {
            hintShown = true;
            p.sendMessage(Text.literal("HamsterVisuals: Right Shift - меню, Right Ctrl - двигать панели"), true);
        }

        while (markKey.wasPressed()) {
            if (client.currentScreen == null) {
                Marks.toggleAtCrosshair(client);
            }
        }
        while (friendKey.wasPressed()) {
            if (client.currentScreen == null) {
                toggleFriendOnTarget(client, p);
            }
        }

        // Убийство: цель, по которой недавно били, умерла
        if (lastHitEntity != null) {
            long now = System.currentTimeMillis();
            if (now - lastHitEntityTime > 4000) {
                lastHitEntity = null;
            } else if (!lastHitEntity.isAlive()) {
                onKill(client, lastHitEntity);
                lastHitEntity = null;
            }
        }

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

    private static void toggleFriendOnTarget(MinecraftClient client, ClientPlayerEntity p) {
        Entity e = client.targetedEntity;
        if (e instanceof PlayerEntity pe) {
            String n = pe.getName().getString();
            boolean now = Social.toggleFriend(n);
            p.sendMessage(Text.literal(now ? "Друг добавлен: " + n : "Друг удалён: " + n), true);
        } else {
            p.sendMessage(Text.literal("Наведись на игрока"), true);
        }
    }

    private static void onHit(World world, Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        VisualsConfig c = VisualsConfig.I;
        HudRenderer.markHit();

        if (target instanceof LivingEntity le) {
            lastHitEntity = le;
            lastHitEntityTime = System.currentTimeMillis();
        }

        Box box = target.getBoundingBox();
        double cx = (box.minX + box.maxX) / 2.0;
        double cy = (box.minY + box.maxY) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double h = box.maxY - box.minY;
        int n = c.particleCount;
        for (int i = 0; i < n; i++) {
            ParticleEffect effect = ModParticles.pick(c.hitParticle);
            double t = i / (double) n;
            double ang = t * Math.PI * 2;
            switch (c.hitPattern) {
                case 1 -> // кольцо вокруг цели
                        world.addParticle(effect, cx, cy, cz, Math.cos(ang) * 0.2, 0.02, Math.sin(ang) * 0.2);
                case 2 -> { // спираль вверх
                    double a2 = i * 0.8;
                    world.addParticle(effect, cx, box.minY + h * t, cz,
                            -Math.sin(a2) * 0.1 + Math.cos(a2) * 0.05, 0.05, Math.cos(a2) * 0.1 + Math.sin(a2) * 0.05);
                }
                case 3 -> // фонтан сверху
                        world.addParticle(effect, cx, box.maxY, cz,
                                (world.random.nextDouble() - 0.5) * 0.12, 0.22 + world.random.nextDouble() * 0.15,
                                (world.random.nextDouble() - 0.5) * 0.12);
                case 4 -> { // сфера
                    double ux = world.random.nextDouble() * 2 - 1;
                    double uy = world.random.nextDouble() * 2 - 1;
                    double uz = world.random.nextDouble() * 2 - 1;
                    double len = Math.max(0.001, Math.sqrt(ux * ux + uy * uy + uz * uz));
                    world.addParticle(effect, cx, cy, cz, ux / len * 0.22, uy / len * 0.22, uz / len * 0.22);
                }
                default -> { // разлёт по всему телу
                    double x = box.minX + world.random.nextDouble() * (box.maxX - box.minX);
                    double y = box.minY + world.random.nextDouble() * (box.maxY - box.minY);
                    double z = box.minZ + world.random.nextDouble() * (box.maxZ - box.minZ);
                    world.addParticle(effect, x, y, z, (world.random.nextDouble() - 0.5) * 0.3,
                            world.random.nextDouble() * 0.25, (world.random.nextDouble() - 0.5) * 0.3);
                }
            }
        }

        if (c.sound && mc.player != null) {
            HitSounds.play(world, mc.player, target);
        }
    }

    private static void onKill(MinecraftClient client, LivingEntity e) {
        VisualsConfig c = VisualsConfig.I;
        if (!c.killEffect || client.world == null) return;

        long now = System.currentTimeMillis();
        killStreak = (now - lastKillTime < 8000) ? killStreak + 1 : 1;
        lastKillTime = now;

        double cx = e.getX();
        double cy = e.getY() + e.getHeight() / 2.0;
        double cz = e.getZ();

        // всплеск из черепов, звёзд и лун
        for (int i = 0; i < 36; i++) {
            double ang = i * (Math.PI * 2 / 12.0);
            double up = 0.05 + (i / 12) * 0.08;
            double sp = 0.12 + (i / 12) * 0.05;
            ParticleEffect eff = ModParticles.pick(i % 3 == 0 ? 2 : (i % 3 == 1 ? 0 : 1));
            client.world.addParticle(eff, cx, cy, cz, Math.cos(ang) * sp, up, Math.sin(ang) * sp);
        }

        String text = killStreak > 1 ? "УБИЙСТВО x" + killStreak : "УБИЙСТВО";
        FloatTexts.spawn(text, cx, cy + 1.0, cz, 1800, 0xFF4D6D, 0.4, true);
        HudRenderer.markHit();

        if (c.sound) {
            CustomSounds.play(CustomSounds.KILL, (float) c.soundVolume, 1.0f);
        }
    }
}
