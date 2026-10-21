package com.hxhmod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Eigene Partikel. Die Farbe wird beim Senden als "Geschwindigkeit" (r,g,b) mitgegeben. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, HxhMod.MODID);

    public static final RegistryObject<SimpleParticleType> GLOW = PARTICLES.register("glow", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> FLAME = PARTICLES.register("flame", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPARK = PARTICLES.register("spark", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BLAST = PARTICLES.register("blast", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BLAST_BIG = PARTICLES.register("blast_big", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> RING = PARTICLES.register("ring", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> STREAK = PARTICLES.register("streak", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> DRAGON = PARTICLES.register("dragon", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> DRAGON_BIG = PARTICLES.register("dragon_big", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BOLT = PARTICLES.register("bolt", () -> new SimpleParticleType(true));

    private ModParticles() {}
}
