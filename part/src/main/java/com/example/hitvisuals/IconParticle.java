package com.example.hitvisuals;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/** Своя частица: звезда, луна или череп (форма зависит от текстуры). */
public class IconParticle extends SpriteBillboardParticle {
    private final float spin;

    protected IconParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z);
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
        this.maxAge = 28 + this.random.nextInt(24);
        this.gravityStrength = 0.015f;
        this.velocityMultiplier = 0.96f;
        this.collidesWithWorld = false;
        this.scale = 0.09f + this.random.nextFloat() * 0.06f;
        this.spin = (this.random.nextFloat() - 0.5f) * 0.15f;
        int rgb = ParticleColors.pick();
        this.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAngle = this.angle;
        this.angle += this.spin;
        float t = (float) this.age / (float) this.maxAge;
        this.setAlpha(Math.max(0f, 1f - t * t));
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getBrightness(float tint) {
        return 0xF000F0;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            IconParticle p = new IconParticle(world, x, y, z, vx, vy, vz);
            p.setSprite(this.sprites);
            return p;
        }
    }
}
