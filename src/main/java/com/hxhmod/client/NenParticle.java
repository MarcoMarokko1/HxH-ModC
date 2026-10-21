package com.hxhmod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Selbstleuchtende Nen-Partikel. Farbe = Übergabe-"Geschwindigkeit" (r,g,b). */
public class NenParticle extends TextureSheetParticle {

    public enum Kind {
        //     minLife maxLife start end   rise  spin  anim   randRoll growEase
        GLOW   (8, 12, 0.40f, 0.00f, 0.020f, 0.0f, false, false, false),
        FLAME  (12, 18, 0.55f, 0.15f, 0.090f, 0.05f, true, true, false),
        SPARK  (6, 10, 0.55f, 0.00f, 0.000f, 0.30f, false, true, false),
        BLAST  (7, 7, 1.20f, 3.60f, 0.000f, 0.00f, false, false, true),
        BLAST_BIG(10, 10, 2.50f, 8.00f, 0.000f, 0.00f, false, false, true),
        RING   (9, 9, 0.50f, 5.50f, 0.000f, 0.00f, false, false, true),
        STREAK (5, 5, 1.70f, 1.90f, 0.000f, 0.00f, false, true, false),
        BOLT   (4, 4, 1.50f, 1.50f, 0.000f, 0.00f, false, true, false),
        DRAGON (2, 2, 3.20f, 3.20f, 0.000f, 0.00f, false, false, false),
        DRAGON_BIG(3, 3, 7.50f, 7.50f, 0.000f, 0.00f, false, false, false);

        final int minLife, maxLife;
        final float start, end, rise, spin;
        final boolean animated, randRoll, growEase;

        Kind(int minLife, int maxLife, float start, float end, float rise, float spin, boolean animated,
             boolean randRoll, boolean growEase) {
            this.minLife = minLife;
            this.maxLife = maxLife;
            this.start = start;
            this.end = end;
            this.rise = rise;
            this.spin = spin;
            this.animated = animated;
            this.randRoll = randRoll;
            this.growEase = growEase;
        }
    }

    private final SpriteSet sprites;
    private final Kind kind;

    protected NenParticle(ClientLevel level, double x, double y, double z, double r, double g, double b,
                          SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.lifetime = kind.minLife + (kind.maxLife > kind.minLife ? this.random.nextInt(kind.maxLife - kind.minLife + 1) : 0);
        this.xd = (this.random.nextDouble() - 0.5) * (kind.rise > 0 ? 0.02 : 0.0);
        this.zd = (this.random.nextDouble() - 0.5) * (kind.rise > 0 ? 0.02 : 0.0);
        this.yd = kind.rise * (0.7 + this.random.nextDouble() * 0.6);
        if (r + g + b < 0.01) { r = g = b = 1.0; }
        this.setColor((float) r, (float) g, (float) b);
        this.quadSize = kind.start;
        if (kind.randRoll) {
            this.roll = this.oRoll = this.random.nextFloat() * Mth.TWO_PI;
        }
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        if (kind.animated) this.setSpriteFromAge(sprites);
        this.move(this.xd, this.yd, this.zd);
        this.roll += kind.spin;
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = kind == Kind.BOLT ? (this.random.nextBoolean() ? 1f : 0.45f) * (1f - t * 0.5f)
                : (kind.growEase ? 1f - t * t : 1f - t * t * t);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp((this.age + partialTicks) / (float) this.lifetime, 0f, 1f);
        float e = kind.growEase ? 1f - (1f - t) * (1f - t) : t;
        return Mth.lerp(e, kind.start, kind.end);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xs, double ys, double zs) {
            return new NenParticle(level, x, y, z, xs, ys, zs, sprites, kind);
        }
    }
}
