package com.example.hitvisuals;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
    public static final SimpleParticleType STAR = FabricParticleTypes.simple();
    public static final SimpleParticleType MOON = FabricParticleTypes.simple();
    public static final SimpleParticleType SKULL = FabricParticleTypes.simple();

    public static final String[] NAMES = {"Звёзды", "Луны", "Черепа", "Смесь", "Огонь", "Сердечки"};

    /** Регистрирует типы частиц (вызывается из основной точки входа). */
    public static void registerTypes() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("hitvisuals", "star"), STAR);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("hitvisuals", "moon"), MOON);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("hitvisuals", "skull"), SKULL);
    }

    /** Привязывает к типам частиц их отрисовку (клиент). */
    public static void registerFactories() {
        ParticleFactoryRegistry r = ParticleFactoryRegistry.getInstance();
        r.register(STAR, provider -> new IconParticle.Factory(provider));
        r.register(MOON, provider -> new IconParticle.Factory(provider));
        r.register(SKULL, provider -> new IconParticle.Factory(provider));
    }

    /** Возвращает частицу по номеру из списка NAMES. */
    public static ParticleEffect pick(int index) {
        switch (index) {
            case 0:
                return STAR;
            case 1:
                return MOON;
            case 2:
                return SKULL;
            case 3: {
                int r = (int) (Math.random() * 3);
                return r == 0 ? STAR : (r == 1 ? MOON : SKULL);
            }
            case 4:
                return ParticleTypes.FLAME;
            default:
                return ParticleTypes.HEART;
        }
    }

    private ModParticles() {}
}
