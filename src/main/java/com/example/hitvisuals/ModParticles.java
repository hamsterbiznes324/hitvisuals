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
    public static final SimpleParticleType HEART = FabricParticleTypes.simple();
    public static final SimpleParticleType SNOW = FabricParticleTypes.simple();
    public static final SimpleParticleType BOLT = FabricParticleTypes.simple();
    public static final SimpleParticleType FLOWER = FabricParticleTypes.simple();
    public static final SimpleParticleType CROWN = FabricParticleTypes.simple();
    public static final SimpleParticleType GEM = FabricParticleTypes.simple();
    public static final SimpleParticleType PAW = FabricParticleTypes.simple();

    private static final SimpleParticleType[] CUSTOM = {STAR, MOON, SKULL, HEART, SNOW, BOLT, FLOWER, CROWN, GEM, PAW};
    private static final String[] IDS = {"star", "moon", "skull", "heart", "snow", "bolt", "flower", "crown", "gem", "paw"};

    /** Порядок первых шести совпадает со старыми версиями, чтобы сохранённые настройки не сломались. */
    public static final String[] NAMES = {
            "Звёзды", "Луны", "Черепа", "Смесь", "Огонь", "Сердечки (игра)",
            "Сердца", "Снежинки", "Молнии", "Цветы", "Короны", "Кристаллы", "Лапки"
    };

    /** Регистрирует типы частиц (вызывается из основной точки входа). */
    public static void registerTypes() {
        for (int i = 0; i < CUSTOM.length; i++) {
            Registry.register(Registries.PARTICLE_TYPE, Identifier.of("hitvisuals", IDS[i]), CUSTOM[i]);
        }
    }

    /** Привязывает к типам частиц их отрисовку (клиент). */
    public static void registerFactories() {
        ParticleFactoryRegistry r = ParticleFactoryRegistry.getInstance();
        for (SimpleParticleType t : CUSTOM) {
            r.register(t, provider -> new IconParticle.Factory(provider));
        }
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
            case 3:
                return CUSTOM[(int) (Math.random() * CUSTOM.length)];
            case 4:
                return ParticleTypes.FLAME;
            case 5:
                return ParticleTypes.HEART;
            case 6:
                return HEART;
            case 7:
                return SNOW;
            case 8:
                return BOLT;
            case 9:
                return FLOWER;
            case 10:
                return CROWN;
            case 11:
                return GEM;
            default:
                return PAW;
        }
    }

    private ModParticles() {}
}
