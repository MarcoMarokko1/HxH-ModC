package com.hxhmod.nen;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/** ENHANCER = Gon (grüne Aura), TRANSMUTER = Killua (blau-weiße Blitz-Aura), XENO = Zeno Zoldyck (goldene Drachen-Aura). */
public enum NenType {
    ENHANCER("enhancer", 0.35f, 1.00f, 0.35f, 50, ParticleTypes.CRIT),
    TRANSMUTER("transmuter", 0.45f, 0.75f, 1.00f, 50, ParticleTypes.ELECTRIC_SPARK),
    XENO("xeno", 1.00f, 0.72f, 0.22f, 12, ParticleTypes.FLAME),
    POUF("pouf", 0.78f, 0.50f, 1.00f, 10, ParticleTypes.END_ROD);

    public final String id;
    public final float r, g, b;
    public final int weight;
    public final ParticleOptions accent;

    NenType(String id, float r, float g, float b, int weight, ParticleOptions accent) {
        this.id = id;
        this.r = r;
        this.g = g;
        this.b = b;
        this.weight = weight;
        this.accent = accent;
    }

    public DustParticleOptions dust(float scale) {
        return new DustParticleOptions(new Vector3f(r, g, b), scale);
    }

    /** Farbverlauf von der Aura-Farbe zu Weiß: wirkt deutlich "heißer" als einfacher Staub. */
    public DustColorTransitionOptions glow(float scale) {
        return new DustColorTransitionOptions(new Vector3f(r, g, b), new Vector3f(1f, 1f, 1f), scale);
    }

    public int rgb() {
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static NenType byOrdinal(int i) {
        NenType[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    public static NenType roll(RandomSource rnd) {
        int total = 0;
        for (NenType t : values()) total += t.weight;
        int roll = rnd.nextInt(total);
        for (NenType t : values()) {
            roll -= t.weight;
            if (roll < 0) return t;
        }
        return ENHANCER;
    }
}
