package com.hxhmod.nen;

import com.hxhmod.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fähigkeiten von Gon (Verstärker) und Killua (Transmuter).
 * Kosten = Aura, Cooldown in Ticks (20 = 1 s). Schaden wird in Targeting.hurt mit Level und Config skaliert.
 */
public final class Abilities {
    public static final List<Ability> ALL = new ArrayList<>();
    private static final Map<NenType, List<Ability>> BY_TYPE = new ConcurrentHashMap<>();

    private static Ability reg(String id, NenType t, int minLevel, int cost, int cd, Ability.Effect e) {
        Ability a = new Ability(id, t, minLevel, cost, cd, e);
        ALL.add(a);
        return a;
    }

    public static List<Ability> forType(NenType t) {
        return BY_TYPE.computeIfAbsent(t, k -> ALL.stream().filter(a -> a.type == k).toList());
    }

    private static Vec3 horizontal(Vec3 d, Vec3 fallback) {
        Vec3 h = d.multiply(1, 0, 1);
        return h.lengthSqr() < 0.01 ? fallback.multiply(1, 0, 1).normalize() : h.normalize();
    }

    // ================================================================== Allgemeine Techniken

    public static final Ability GYO = reg("gyo", null, 1, 10, 100, (p, l, lvl) -> {
        double r = 20 + lvl * 2;
        for (LivingEntity e : Targeting.near(p, l, p.position(), r))
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120 + lvl * 10, 0, false, false));
        Vec3 eye = p.getEyePosition().add(p.getLookAngle().scale(0.4));
        FX.burst(l, ParticleTypes.END_ROD, eye, 14, 0.06);
        FX.sound(l, eye, SoundEvents.ENDER_EYE_LAUNCH, 0.8f, 1.4f);
        return true;
    });

    public static final Ability EN = reg("en", null, 1, 15, 160, (p, l, lvl) -> {
        double r = 8 + lvl * 2.5;
        Vec3 c = p.position().add(0, 0.1, 0);
        NenType t = NenData.getType(p);
        for (int i = 1; i <= 4; i++) {
            final double rr = r * i / 4.0;
            Scheduler.later(i * 3, () -> FX.ring(l, t.glow(1f), c, rr, (int) (rr * 10)));
        }
        for (LivingEntity e : Targeting.near(p, l, p.position(), r))
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false));
        FX.sound(l, c, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.6f);
        return true;
    });

    // ================================================================== GON (Verstärker)

    /** Lv1: schneller Aura-Schlag, bricht kleine Risse in Blöcke. */
    public static final Ability AURA_FIST = reg("aura_fist", NenType.ENHANCER, 1, 12, 24, (p, l, lvl) -> {
        Vec3 lk = p.getLookAngle();
        p.setDeltaMovement(lk.x * 0.45, 0.1, lk.z * 0.45);
        p.hurtMarked = true;
        FX.anim(p, "gon_fist");
        Scheduler.later(3, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            Vec3 look = p.getLookAngle();
            Vec3 c = p.getEyePosition().add(look.scale(2.0));
            for (LivingEntity e : Targeting.near(p, l, c, 2.2)) {
                Targeting.hurt(p, e, 7f);
                Targeting.push(e, horizontal(e.position().subtract(p.position()), look).scale(1.1).add(0, 0.35, 0));
            }
            Destruction.crater(p, l, c, 1.7, 3.0, 5, look);
            NenType t = NenType.ENHANCER;
            FxEntity.spawn(l, FxEntity.FIST, t, 6, 1.0f, 0, p.getEyePosition().add(look.scale(0.6)).add(0, -0.4, 0), c, 0f, 0f);
            FX.burst(l, t.glow(1.6f), c, 30, 0.18);
            FX.burst(l, ParticleTypes.CRIT, c, 22, 0.6);
            FX.burst(l, ParticleTypes.SWEEP_ATTACK, c, 2, 0);
            FX.ringAxis(l, t.dust(1.3f), c, look, 1.2, 20);
            FX.impact(l, t, c, false);
            FX.sound(l, c, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f);
            FX.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.6f);
            FX.impactNear(l, c, 18, 0.35f, 6, 0, 0f);
        });
        return true;
    });

    /** Lv3: Jajanken Choki (Schere): gekreuzte Aura-Schnitte fliegen nach vorn und schneiden eine Schneise. */
    public static final Ability JAJANKEN_CHOKI = reg("jajanken_choki", NenType.ENHANCER, 3, 28, 100, (p, l, lvl) -> {
        FX.cutscene(p, "choki", 30, p.getEyePosition().add(p.getLookAngle().scale(8)));
        FX.anim(p, "gon_choki");
        NenType t = NenType.ENHANCER;
        FX.sound(l, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.8f);
        for (int i = 0; i < 8; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(1.3)).add(0, -0.3, 0);
                FX.sphere(l, t.dust(1.0f), hand, 1.4 - k * 0.15, 10);
            });
        }
        Scheduler.later(9, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            final Vec3 dir = p.getLookAngle().normalize();
            final Vec3 origin = p.getEyePosition().add(0, -0.3, 0);
            Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            final Vec3 right = dir.cross(up).normalize();
            final Vec3 upv = right.cross(dir).normalize();
            final double len = 15;
            FxEntity.spawn(l, FxEntity.SCISSORS, t, 12, 1.6f, 0, origin.add(dir.scale(1.0)), origin.add(dir.scale(len)), 0f, 0f);
            for (int i = 0; i < 10; i++) {
                final int k = i;
                Scheduler.later(i + 1, () -> {
                    double s = 1.5 + k * 1.5;
                    double w = 1.6 + k * 0.12;
                    Vec3 c = origin.add(dir.scale(s));
                    Vec3 d1 = right.add(upv).normalize().scale(w);
                    Vec3 d2 = right.subtract(upv).normalize().scale(w);
                    FX.line(l, t.glow(1.4f), c.subtract(d1), c.add(d1), 0.3);
                    FX.line(l, t.glow(1.4f), c.subtract(d2), c.add(d2), 0.3);
                    FX.burst(l, ParticleTypes.CRIT, c, 6, 0.4);
                    FX.fx(l, ModParticles.STREAK, c, t, 2, 0.25);
                });
            }
            for (LivingEntity e : Targeting.alongRay(p, l, origin, dir, len, 1.8)) {
                Targeting.hurt(p, e, 11f);
                Targeting.push(e, dir.scale(0.9).add(0, 0.3, 0));
                FX.burst(l, ParticleTypes.SWEEP_ATTACK, e.getBoundingBox().getCenter(), 2, 0);
            }
            Destruction.slit(p, l, origin, dir, len, 1.0, 2.4, 2.5, 10);
            FX.sound(l, origin, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
            FX.blast(l, t, origin.add(dir.scale(2)), false);
            FX.sound(l, origin, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.4f);
            FX.impactNear(l, origin, 20, 0.6f, 8, 0, 0.05f);
        });
        return true;
    });

    /** Lv4: Riesen-Aufprall: Sprung, Sturzflug, Krater bei der Landung. */
    public static final Ability GIANT_SLAM = reg("giant_slam", NenType.ENHANCER, 4, 30, 160, (p, l, lvl) -> {
        FX.anim(p, "gon_slam");
        p.setDeltaMovement(p.getDeltaMovement().x, 1.15, p.getDeltaMovement().z);
        p.hurtMarked = true;
        Pending.SLAM.put(p.getUUID(), Scheduler.now());
        Pending.NO_FALL.put(p.getUUID(), Scheduler.now() + 160);
        FX.ring(l, NenType.ENHANCER.glow(1.4f), p.position().add(0, 0.1, 0), 1.5, 20);
        FX.burst(l, ParticleTypes.CLOUD, p.position(), 20, 0.2);
        FX.sound(l, p.position(), SoundEvents.IRON_GOLEM_ATTACK, 1f, 0.7f);
        return true;
    });

    /** Wird aus NenEvents aufgerufen, sobald der Spieler nach dem Sprung landet. */
    public static void slamImpact(ServerPlayer p, ServerLevel l) {
        NenType t = NenType.ENHANCER;
        Vec3 c = p.position();
        for (LivingEntity e : Targeting.near(p, l, c, 4.8)) {
            Targeting.hurt(p, e, 11f);
            Vec3 d = e.position().subtract(c).multiply(1, 0, 1);
            d = d.lengthSqr() < 0.01 ? Vec3.ZERO : d.normalize();
            Targeting.push(e, d.scale(1.1).add(0, 0.6, 0));
        }
        Destruction.crater(p, l, c.add(0, -0.6, 0), 3.9, 4.0, 16, null);
        FX.shockwave(l, t.glow(1.5f), c, 7);
        FX.burst(l, ParticleTypes.EXPLOSION, c.add(0, 0.3, 0), 6, 0);
        FX.burst(l, ParticleTypes.POOF, c.add(0, 0.2, 0), 50, 0.3);
        FX.sphere(l, t.dust(1.6f), c.add(0, 0.3, 0), 2.5, 40);
        FX.impact(l, t, c.add(0, 0.5, 0), true);
        FxEntity.spawn(l, FxEntity.DOME, t, 10, 6.5f, 2, c.add(0, 0.1, 0), c, 0f, 0f);
        FX.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.0f, 0.9f);
        FX.impactNear(l, c, 24, 1.2f, 12, 2, 0.08f);
    }

    /** Lv6: Jajanken Pa (Papier): riesige Aura-Welle, schneidet Schneise und schlägt Krater am Ende. */
    public static final Ability JAJANKEN_PA = reg("jajanken_pa", NenType.ENHANCER, 6, 45, 220, (p, l, lvl) -> {
        FX.cutscene(p, "pa", 40, p.getEyePosition().add(p.getLookAngle().scale(10)));
        FX.anim(p, "gon_pa");
        NenType t = NenType.ENHANCER;
        for (int i = 0; i < 9; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(1.0)).add(0, -0.3, 0);
                FX.sphere(l, t.glow(1.2f), hand, 1.8 - k * 0.15, 12);
            });
        }
        Scheduler.later(11, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            final Vec3 dir = p.getLookAngle().normalize();
            final Vec3 origin = p.getEyePosition().add(0, -0.3, 0);
            final double len = 14;
            FxEntity.spawn(l, FxEntity.PALM, t, 14, 2.4f, 0, origin.add(dir.scale(0.8)), origin.add(dir.scale(len)), 0f, 0f);
            for (int i = 0; i < 7; i++) {
                final int k = i;
                Scheduler.later(i + 1, () -> {
                    double s = 2 + k * 2;
                    Vec3 c = origin.add(dir.scale(s));
                    FX.ringAxis(l, t.glow(1.7f), c, dir, 0.6 + s * 0.28, (int) (12 + s * 4));
                    FX.burst(l, t.accent, c, 10, 0.25);
                    FX.fx(l, ModParticles.RING, c, t, 1, 0);
                });
            }
            for (LivingEntity e : Targeting.alongRay(p, l, origin, dir, len, 3.0)) {
                Targeting.hurt(p, e, 12f);
                Targeting.push(e, dir.scale(1.8).add(0, 0.4, 0));
            }
            Destruction.slit(p, l, origin, dir, len, 1.4, 2.8, 1.5, 12);
            Vec3 end = Targeting.aimPoint(p, len);
            Destruction.crater(p, l, end, 2.8, 5.0, 14, dir);
            FX.burst(l, ParticleTypes.EXPLOSION_EMITTER, end, 1, 0);
            FX.sphere(l, t.glow(2.0f), end, 3.0, 50);
            FX.impact(l, t, end, true);
            p.setDeltaMovement(-dir.x * 0.8, 0.2, -dir.z * 0.8);
            p.hurtMarked = true;
            FX.sound(l, origin, SoundEvents.GENERIC_EXPLODE, 1.3f, 0.7f);
            FX.impactNear(l, origin, 28, 1.2f, 12, 4, 0.1f);
        });
        return true;
    });

    /** Lv8: Jajanken Gu (Stein): voll aufgeladener Schlag mit Riesenkrater. Kostet 4 HP. */
    public static final Ability JAJANKEN_GU = reg("jajanken_gu", NenType.ENHANCER, 8, 70, 600, (p, l, lvl) -> {
        FX.cutscene(p, "gu", 60, p.getEyePosition().add(p.getLookAngle().scale(3.5)).add(0, -0.6, 0));
        FX.anim(p, "gon_gu_charge");
        NenType t = NenType.ENHANCER;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 3, false, false));
        Vec3 lk0 = p.getLookAngle().normalize();
        FxEntity.spawn(l, FxEntity.FIST, t, 40, 4.2f, 3, p.getEyePosition().add(lk0.scale(1.2)).add(0, -0.4, 0),
                p.getEyePosition().add(lk0.scale(3.5)).add(0, -0.6, 0), 21f / 40f, 27f / 40f);
        for (int i = 0; i < 20; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 fist = p.getEyePosition().add(p.getLookAngle().scale(1.2)).add(0, -0.4, 0);
                FX.sphere(l, t.glow(1.4f), fist, 3.0 - k * 0.13, 18);
                FX.ring(l, t.dust(1.2f), p.position().add(0, 0.1, 0), 4.5 - k * 0.2, 24);
                FX.burst(l, ParticleTypes.CRIT, fist, 4, 0.3);
                FX.fx(l, ModParticles.GLOW, fist, t, 3, 0.6);
                FX.fx(l, ModParticles.SPARK, fist, t, 2, 1.2);
                if (k % 5 == 0) FX.sound(l, fist, SoundEvents.BEACON_AMBIENT, 0.8f, 0.5f + k * 0.06f);
                FX.shake(p, 0.08f + k * 0.02f, 2);
            });
        }
        Scheduler.later(21, () -> { if (p.isAlive()) FX.anim(p, "gon_gu_punch"); });
        Scheduler.later(23, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            Vec3 look = p.getLookAngle().normalize();
            Vec3 eye = p.getEyePosition();
            Vec3 c = eye.add(look.scale(3.5)).add(0, -0.6, 0);
            for (LivingEntity e : Targeting.near(p, l, c, 5.5)) {
                Targeting.hurt(p, e, 22f);
                Targeting.push(e, horizontal(e.position().subtract(p.position()), look).scale(2.2).add(0, 0.8, 0));
            }
            Destruction.crater(p, l, c, 5.0, 8.0, 36, look);
            Destruction.slit(p, l, eye.add(0, -0.5, 0), look, 9, 1.6, 3.2, 3.0, 10);
            FX.burst(l, ParticleTypes.EXPLOSION_EMITTER, c, 1, 0);
            FX.burst(l, ParticleTypes.EXPLOSION_EMITTER, c.add(look.scale(2)), 1, 0);
            FX.burst(l, ParticleTypes.FLASH, c, 1, 0);
            FX.sphere(l, t.glow(2.4f), c, 4.5, 90);
            FX.impact(l, t, c, true);
            FX.impact(l, t, c.add(look.scale(2)), true);
            p.setDeltaMovement(-look.x * 0.9, 0.25, -look.z * 0.9);
            p.hurtMarked = true;
            FX.shockwave(l, t.glow(1.8f), p.position(), 12);
            FX.line(l, t.glow(2f), eye, c.add(look.scale(3)), 0.3);
            FX.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.5f);
            FX.sound(l, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 0.6f);
            FX.impactNear(l, c, 32, 2.6f, 20, 10, 0.25f);
            p.setHealth(Math.max(1f, p.getHealth() - 4f));
        });
        return true;
    });

    // ================================================================== KILLUA (Transmuter)

    /** Lv1: Blitz-Handfläche: kurzer Elektroschock mit Lähmung. */
    public static final Ability LIGHTNING_PALM = reg("lightning_palm", NenType.TRANSMUTER, 1, 14, 30, (p, l, lvl) -> {
        FX.anim(p, "killua_palm");
        Scheduler.later(3, () -> {
            if (!p.isAlive()) return;
            NenType t = NenType.TRANSMUTER;
            p.swing(InteractionHand.MAIN_HAND, true);
            Vec3 look = p.getLookAngle();
            Vec3 c = p.getEyePosition().add(look.scale(1.8));
            for (LivingEntity e : Targeting.near(p, l, c, 2.3)) {
                Targeting.hurt(p, e, 5f);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Targeting.pvpScaled(e, 25), 2));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Targeting.pvpScaled(e, 40), 0));
                FX.arc(l, t.glow(1.2f), c, e.getBoundingBox().getCenter(), 0.35);
            }
            for (int i = 0; i < 4; i++) {
                Vec3 q = c.add((l.random.nextDouble() - 0.5) * 3, (l.random.nextDouble() - 0.5) * 2, (l.random.nextDouble() - 0.5) * 3);
                FX.arc(l, t.glow(1.0f), c, q, 0.3);
            }
            FX.burst(l, ParticleTypes.ELECTRIC_SPARK, c, 30, 0.5);
            FX.sphere(l, t.glow(1.2f), c, 0.9, 14);
            FX.blast(l, t, c, false);
            FX.fx(l, ModParticles.BOLT, c, t, 3, 1.0);
            for (LivingEntity e2 : Targeting.near(p, l, c, 2.3))
                FxEntity.spawn(l, FxEntity.BOLT, t, 6, 0.8f, 2, c, e2.getBoundingBox().getCenter(), 0f, 0f);
            FxEntity.spawn(l, FxEntity.BOLT, t, 6, 0.8f, 2, c, c.add(look.scale(2.4)), 0f, 0f);
            FX.sound(l, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f);
            FX.impactNear(l, c, 16, 0.3f, 5, 0, 0f);
        });
        return true;
    });

    /** Lv3: Götterblitz: Dash in Lichtgeschwindigkeit, schlägt durch Glas und Laub und schockt alles auf dem Weg. */
    public static final Ability GODSPEED = reg("godspeed", NenType.TRANSMUTER, 3, 22, 60, (p, l, lvl) -> {
        NenType t = NenType.TRANSMUTER;
        Vec3 dir = p.getLookAngle();
        Vec3 start = p.position();
        double max = 10 + lvl;
        Destruction.slit(p, l, p.getEyePosition().add(0, -0.9, 0), dir, max, 0.7, 2.0, 0.8, 0);
        double dist = Targeting.freeDistance(p, l, dir, max);
        if (dist < 1.0) return false;
        FX.anim(p, "killua_godspeed");
        Vec3 end = start.add(dir.scale(dist));
        for (LivingEntity e : Targeting.alongRay(p, l, start.add(0, 0.9, 0), dir.normalize(), dist, 1.2)) {
            Targeting.hurt(p, e, 5f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Targeting.pvpScaled(e, 20), 2));
            FX.burst(l, ParticleTypes.ELECTRIC_SPARK, e.getBoundingBox().getCenter(), 20, 0.4);
        }
        for (int i = 0; i < 4; i++) {
            Vec3 a = start.add(0, 0.5 + i * 0.4, 0);
            Vec3 b = end.add(0, 0.5 + i * 0.4, 0);
            FX.arc(l, t.glow(1.2f), a, b, 0.6);
        }
        FX.line(l, t.glow(1.6f), start.add(0, 0.9, 0), end.add(0, 0.9, 0), 0.3);
        for (int s = 0; s < 6; s++)
            FX.fx(l, ModParticles.STREAK, start.add(dir.scale(dist * s / 5.0)).add(0, 1, 0), t, 1, 0.5);
        FX.blast(l, t, start.add(0, 1, 0), false);
        FX.blast(l, t, end.add(0, 1, 0), false);
        for (int bi = 0; bi < 3; bi++)
            FxEntity.spawn(l, FxEntity.BOLT, t, 8, 0.9f, 2, start.add(0, 0.5 + bi * 0.5, 0), end.add(0, 0.5 + bi * 0.5, 0), 0f, 0f);
        for (int i = 0; i <= 4; i++) {
            Vec3 q = start.add(dir.scale(dist * i / 4.0)).add(0, 1, 0);
            FX.sphere(l, t.glow(1.0f), q, 0.5, 10);
        }
        FX.burst(l, ParticleTypes.ELECTRIC_SPARK, start.add(0, 1, 0), 30, 0.5);
        p.connection.teleport(end.x, end.y, end.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        FX.burst(l, ParticleTypes.ELECTRIC_SPARK, end.add(0, 1, 0), 30, 0.5);
        FX.sound(l, end, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
        FX.impactNear(l, end, 14, 0.3f, 5, 2, 0.2f);
        return true;
    });

    private static void claw(ServerLevel l, Vec3 c, Vec3 look, int k) {
        NenType t = NenType.TRANSMUTER;
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 s = right.scale(0.9).add(0, k % 2 == 0 ? 0.9 : -0.9, 0);
        for (int off = -1; off <= 1; off++) {
            Vec3 base = c.add(right.scale(off * 0.25));
            FX.line(l, t.glow(1.2f), base.subtract(s), base.add(s), 0.2);
        }
        FX.burst(l, ParticleTypes.SWEEP_ATTACK, c, 1, 0);
        FX.burst(l, ParticleTypes.ELECTRIC_SPARK, c, 14, 0.4);
        FX.fx(l, ModParticles.STREAK, c, t, 3, 0.4);
        FX.blast(l, t, c, false);
    }

    /** Lv4: Assassinen-Krallen: Sprung zum Ziel und 5 blitzschnelle Krallenhiebe. */
    public static final Ability ASSASSIN_CLAWS = reg("assassin_claws", NenType.TRANSMUTER, 4, 30, 120, (p, l, lvl) -> {
        LivingEntity target = Targeting.ray(p, 6.5);
        if (target == null) return false;
        FX.anim(p, "killua_claws");
        Vec3 to = target.position().subtract(p.position()).multiply(1, 0, 1);
        final Vec3 push = to.lengthSqr() < 0.01 ? Vec3.ZERO : to.normalize();
        p.setDeltaMovement(push.x * 0.7, 0.15, push.z * 0.7);
        p.hurtMarked = true;
        for (int i = 0; i < 5; i++) {
            final int k = i;
            Scheduler.later(2 + i * 2, () -> {
                if (!p.isAlive() || !target.isAlive()) return;
                Vec3 c = target.getBoundingBox().getCenter();
                Targeting.hurt(p, target, 3.4f);
                if (k == 4) Targeting.push(target, push.scale(1.2).add(0, 0.4, 0));
                claw(l, c, p.getLookAngle(), k);
                FxEntity.spawn(l, FxEntity.CLAWS, NenType.TRANSMUTER, 6, 1.3f, 2, c, c.add(p.getLookAngle()), k % 2 == 0 ? 1f : -1f, 0f);
                FX.sound(l, c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f + k * 0.1f);
                FX.sound(l, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.25f, 2.0f);
                FX.impactNear(l, c, 14, 0.25f, 4, 0, 0f);
            });
        }
        return true;
    });

    /** Lv6: Wirbelsturm: Drehung mit Blitz-Tornado, schleudert alles weg und bricht weiche Blöcke. */
    public static final Ability WHIRLWIND = reg("whirlwind", NenType.TRANSMUTER, 6, 40, 200, (p, l, lvl) -> {
        FX.cutscene(p, "whirlwind", 34, p.position());
        FX.anim(p, "killua_whirlwind");
        NenType t = NenType.TRANSMUTER;
        FxEntity.spawn(l, FxEntity.TORNADO, t, 16, 1.3f, 2, p.position(), p.position(), 0f, 0f);
        for (int i = 0; i < 10; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 c = p.position();
                double a = k * 0.8;
                for (int j = 0; j < 4; j++) {
                    double ang = a + j * Math.PI / 2;
                    double rr = 1.5 + k * 0.3;
                    Vec3 q = c.add(Math.cos(ang) * rr, 0.6 + k * 0.1, Math.sin(ang) * rr);
                    FX.arc(l, t.glow(1.0f), c.add(0, 1, 0), q, 0.25);
                }
                FX.ring(l, t.glow(1.3f), c.add(0, 0.2 + k * 0.12, 0), 1.5 + k * 0.35, 28);
                FX.burst(l, ParticleTypes.CLOUD, c.add(0, 0.3, 0), 4, 0.35);
                FX.fx(l, ModParticles.BOLT, c.add(0, 0.5 + k * 0.1, 0), t, 2, 1.4);
                FX.fx(l, ModParticles.RING, c.add(0, 0.3 + k * 0.12, 0), t, 1, 0);
            });
        }
        Scheduler.later(6, () -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position();
            for (LivingEntity e : Targeting.near(p, l, c, 5.0)) {
                Targeting.hurt(p, e, 9f);
                Targeting.push(e, horizontal(e.position().subtract(c), p.getLookAngle()).scale(1.5).add(0, 0.5, 0));
            }
            Destruction.crater(p, l, c.add(0, 0.5, 0), 3.5, 1.0, 10, null);
            FX.shockwave(l, t.glow(1.5f), c, 6);
            FX.impact(l, t, c.add(0, 0.5, 0), true);
            FxEntity.spawn(l, FxEntity.DOME, t, 9, 5.0f, 2, c.add(0, 0.2, 0), c, 0f, 0f);
            FX.sound(l, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.4f);
            FX.sound(l, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
            FX.impactNear(l, c, 22, 0.9f, 10, 0, 0.08f);
        });
        return true;
    });

    private static void strike(ServerPlayer p, ServerLevel l, Vec3 pos, float dmg, double dmgRadius,
                               double craterRadius, int debris, boolean chain) {
        NenType t = NenType.TRANSMUTER;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            l.addFreshEntity(bolt);
        }
        List<LivingEntity> hit = new ArrayList<>(Targeting.near(p, l, pos, dmgRadius));
        for (LivingEntity e : hit) {
            Targeting.hurt(p, e, dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Targeting.pvpScaled(e, 40), 3));
        }
        Destruction.crater(p, l, pos.add(0, -0.5, 0), craterRadius, 6.0, debris, null);
        FX.column(l, t.glow(2.2f), pos, 30, 0.5);
        FX.burst(l, ParticleTypes.EXPLOSION, pos, 4, 0);
        FX.impact(l, t, pos.add(0, 0.5, 0), craterRadius > 3);
        FX.fx(l, ModParticles.BOLT, pos.add(0, 2, 0), t, 6, 2.5);
        FxEntity.spawn(l, FxEntity.BOLT, t, 10, craterRadius > 3 ? 2.2f : 1.4f, 2, pos.add(0, 42, 0), pos, 0f, 0f);
        FxEntity.spawn(l, FxEntity.BOLT, t, 10, 0.9f, 2, pos.add(1.5, 38, -1.0), pos, 0f, 0f);
        FX.sphere(l, t.glow(1.8f), pos.add(0, 0.5, 0), craterRadius + 0.5, 50);
        FX.shockwave(l, t.glow(1.6f), pos, craterRadius + 3);
        FX.sound(l, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4f, 0.9f);
        FX.impactNear(l, pos, 32, 1.8f, 14, 6, 0.12f);
        if (chain) {
            Vec3 from = pos;
            for (int i = 0; i < 4; i++) {
                LivingEntity next = null;
                double bd = 144;
                for (LivingEntity c : Targeting.near(p, l, from, 12)) {
                    if (hit.contains(c)) continue;
                    double d = c.getBoundingBox().getCenter().distanceToSqr(from);
                    if (d < bd) { bd = d; next = c; }
                }
                if (next == null) break;
                Vec3 to = next.getBoundingBox().getCenter();
                FX.arc(l, t.glow(1.4f), from.add(0, 0.5, 0), to, 0.5);
                Targeting.hurt(p, next, 10f);
                hit.add(next);
                from = to;
            }
        }
    }

    /** Lv8: Narukami: aufgeladener Blitzschlag mit Kettenblitzen und Nachbeben. */
    public static final Ability NARUKAMI = reg("narukami", NenType.TRANSMUTER, 8, 75, 500, (p, l, lvl) -> {
        final Vec3 target = Targeting.aimPoint(p, 40);
        NenType t = NenType.TRANSMUTER;
        FX.cutscene(p, "narukami", 60, target);
        FX.anim(p, "killua_narukami");
        for (int i = 0; i < 16; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 hand = p.position().add(0, 2.6, 0);
                for (int j = 0; j < 3; j++) {
                    Vec3 q = hand.add((l.random.nextDouble() - 0.5) * (4 - k * 0.15), (l.random.nextDouble() - 0.2) * 2.5,
                            (l.random.nextDouble() - 0.5) * (4 - k * 0.15));
                    FX.arc(l, t.glow(1.0f), hand, q, 0.3);
                }
                FX.sphere(l, t.glow(1.4f), hand, 0.6 + k * 0.05, 12);
                FX.fx(l, ModParticles.GLOW, hand, t, 3, 0.8);
                FX.fx(l, ModParticles.BOLT, hand, t, 1, 1.5);
                if (k % 4 == 0) FX.sound(l, hand, SoundEvents.BEACON_AMBIENT, 0.8f, 1.2f + k * 0.05f);
                FX.shake(p, 0.06f + k * 0.015f, 2);
            });
        }
        Scheduler.later(17, () -> strike(p, l, target, 20f, 4.0, 3.5, 22, true));
        Scheduler.later(21, () -> strike(p, l, target.add((l.random.nextDouble() - 0.5) * 8, 0, (l.random.nextDouble() - 0.5) * 8), 9f, 2.5, 2.0, 8, false));
        Scheduler.later(25, () -> strike(p, l, target.add((l.random.nextDouble() - 0.5) * 8, 0, (l.random.nextDouble() - 0.5) * 8), 9f, 2.5, 2.0, 8, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2, false, false));
        return true;
    });

    // ================================================================== XENO (Zeno Zoldyck): Drachenmeister

    private static void arrow(ServerPlayer p, ServerLevel l, NenType t, Vec3 center, Vec3 top, double radius, float dmg, double craterR) {
        double ang = l.random.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(l.random.nextDouble()) * radius;
        double x = center.x + Math.cos(ang) * r;
        double z = center.z + Math.sin(ang) * r;
        int gy = l.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        Vec3 imp = new Vec3(x, Math.max(gy, center.y - 6), z);
        Vec3 from = top.add((l.random.nextDouble() - 0.5) * 3, (l.random.nextDouble() - 0.5) * 2, (l.random.nextDouble() - 0.5) * 3);
        FX.line(l, t.glow(1.3f), from, imp, 1.0);
        FX.fx(l, ModParticles.SPARK, imp, t, 3, 0.5);
        FX.blast(l, t, imp, false);
        for (LivingEntity e : Targeting.near(p, l, imp, 1.8)) Targeting.hurt(p, e, dmg);
        if (l.random.nextInt(2) == 0) Destruction.crater(p, l, imp.add(0, -0.3, 0), craterR, 3.0, 1, null);
        if (l.random.nextInt(4) == 0) FX.sound(l, imp, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.8f);
    }

    /** Lv1: Dragon Head: ein Aura-Drache schießt aus dem Arm und beißt zu. */
    public static final Ability DRAGON_HEAD = reg("dragon_head", NenType.XENO, 1, 18, 40, (p, l, lvl) -> {
        FX.anim(p, "xeno_dragonhead");
        NenType t = NenType.XENO;
        Scheduler.later(4, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            Vec3 look = p.getLookAngle().normalize();
            Vec3 from = p.getEyePosition().add(look.scale(0.8)).add(0, -0.5, 0);
            Vec3 end = Targeting.aimPoint(p, 9);
            Dragons.fly(l, t, from, end, 5, 0.9, false, null);
            Scheduler.later(5, () -> {
                for (LivingEntity e : Targeting.near(p, l, end, 2.4)) {
                    Targeting.hurt(p, e, 8f);
                    Targeting.push(e, look.scale(1.0).add(0, 0.3, 0));
                }
                Destruction.crater(p, l, end, 1.5, 3.0, 4, look);
                FX.impact(l, t, end, false);
                FX.sound(l, end, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f);
                FX.sound(l, end, SoundEvents.ENDER_DRAGON_GROWL, 0.35f, 1.6f);
                FX.impactNear(l, end, 18, 0.5f, 6, 0, 0.05f);
            });
        });
        return true;
    });

    /** Lv3: Dragon Lance: der Drache fliegt als Lanze durch alles hindurch und schlägt am Ende einen Krater. */
    public static final Ability DRAGON_LANCE = reg("dragon_lance", NenType.XENO, 3, 30, 120, (p, l, lvl) -> {
        FX.cutscene(p, "lance", 36, p.getEyePosition().add(p.getLookAngle().scale(10)));
        FX.anim(p, "xeno_lance");
        NenType t = NenType.XENO;
        for (int i = 0; i < 10; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(1.1)).add(0, -0.4, 0);
                FX.fx(l, ModParticles.GLOW, hand, t, 3, 0.5);
                FX.sphere(l, t.glow(1.2f), hand, 1.5 - k * 0.1, 10);
                if (k == 6) FX.fx(l, ModParticles.DRAGON, hand.add(p.getLookAngle().scale(0.6)), t, 1, 0);
                if (k % 4 == 0) FX.sound(l, hand, SoundEvents.BEACON_AMBIENT, 0.7f, 1.0f + k * 0.07f);
                FX.shake(p, 0.06f + k * 0.012f, 2);
            });
        }
        Scheduler.later(11, () -> {
            if (!p.isAlive()) return;
            p.swing(InteractionHand.MAIN_HAND, true);
            final Vec3 dir = p.getLookAngle().normalize();
            final Vec3 from = p.getEyePosition().add(dir.scale(1.0)).add(0, -0.4, 0);
            final Vec3 end = Targeting.aimPoint(p, 26);
            final double dist = from.distanceTo(end);
            final int ticks = Math.max(4, (int) (dist / 2.6));
            final Set<LivingEntity> hit = new HashSet<>();
            Dragons.fly(l, t, from, end, ticks, 0.8, false, (pp, dd, ii) -> {
                for (LivingEntity e : Targeting.near(p, l, pp, 2.0)) {
                    if (hit.add(e)) {
                        Targeting.hurt(p, e, 11f);
                        Targeting.push(e, dd.scale(1.4).add(0, 0.3, 0));
                        FX.impact(l, t, e.getBoundingBox().getCenter(), false);
                    }
                }
            });
            Destruction.slit(p, l, from, dir, dist, 1.2, 2.4, 2.5, 10);
            FX.sound(l, from, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.2f);
            FX.impactNear(l, from, 24, 0.9f, 10, 3, 0.12f);
            Scheduler.later(ticks, () -> {
                Destruction.crater(p, l, end, 2.6, 4.0, 12, dir);
                FX.impact(l, t, end, true);
                FX.sound(l, end, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.8f);
                FX.impactNear(l, end, 28, 1.3f, 12, 4, 0.1f);
            });
            p.setDeltaMovement(-dir.x * 0.5, 0.15, -dir.z * 0.5);
            p.hurtMarked = true;
        });
        return true;
    });

    /** Lv4: Drachenritt: Xeno reitet den Aura-Drachen in einem Satz nach vorn (Angriff und Fortbewegung). */
    public static final Ability DRAGON_RIDE = reg("dragon_ride", NenType.XENO, 4, 28, 100, (p, l, lvl) -> {
        FX.anim(p, "xeno_ride");
        NenType t = NenType.XENO;
        Vec3 lk = p.getLookAngle();
        final Vec3 dir = new Vec3(lk.x, Mth.clamp(lk.y, -0.25, 0.45), lk.z).normalize();
        Pending.NO_FALL.put(p.getUUID(), Scheduler.now() + 120);
        final Set<LivingEntity> hit = new HashSet<>();
        FX.sound(l, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.2f, 1.0f);
        Vec3 s0 = p.position().add(0, 1, 0);
        Dragons.fly(l, t, s0, s0.add(dir.scale(17)), 9, 0.7, false, null);
        for (int i = 0; i < 9; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                p.setDeltaMovement(dir.x * 1.9, k < 3 ? 0.45 : dir.y * 1.2, dir.z * 1.9);
                p.hurtMarked = true;
                Vec3 c = p.position().add(0, 1, 0);
                FX.fx(l, ModParticles.FLAME, c, t, 3, 0.5);
                FX.fx(l, ModParticles.GLOW, c, t, 3, 0.6);
                FX.fx(l, ModParticles.SPARK, c, t, 2, 0.8);
                for (LivingEntity e : Targeting.near(p, l, c, 2.2)) {
                    if (hit.add(e)) {
                        Targeting.hurt(p, e, 6f);
                        Targeting.push(e, dir.scale(1.2).add(0, 0.4, 0));
                        FX.impact(l, t, e.getBoundingBox().getCenter(), false);
                    }
                }
            });
        }
        FX.impactNear(l, p.position(), 12, 0.3f, 10, 0, 0.2f);
        return true;
    });

    /** Lv6: Dragon Dive: ein Drache bildet sich über dem Ziel, zerspringt und regnet als Aura-Pfeile herab. */
    public static final Ability DRAGON_DIVE = reg("dragon_dive", NenType.XENO, 6, 50, 300, (p, l, lvl) -> {
        final Vec3 target = Targeting.aimPoint(p, 35);
        final Vec3 top = target.add(0, 10, 0);
        NenType t = NenType.XENO;
        FX.cutscene(p, "dive", 64, target);
        FX.anim(p, "xeno_dive");
        Dragons.hover(l, t, top, 16, 2.5, 3.0, true);
        for (int i = 0; i < 14; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                FX.fx(l, ModParticles.GLOW, top, t, 6, 2.5);
                FX.sphere(l, t.glow(1.5f), top, 0.5 + 3.0 * k / 13.0, 16);
                if (k % 4 == 0) FX.fx(l, ModParticles.RING, top, t, 1, 0);
                if (p.isAlive()) FX.shake(p, 0.05f + k * 0.012f, 2);
            });
        }
        Scheduler.later(16, () -> {
            FX.blast(l, t, top, true);
            FX.sphere(l, t.glow(2.4f), top, 4.0, 70);
            FX.sound(l, top, SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.8f);
            FX.sound(l, top, SoundEvents.GENERIC_EXPLODE, 1.0f, 1.4f);
            FX.impactNear(l, target, 40, 1.2f, 10, 4, 0.1f);
        });
        for (int j = 0; j < 24; j++) {
            Scheduler.later(17 + j, () -> {
                for (int n = 0; n < 3; n++) arrow(p, l, t, target, top, 7.0, 3.2f, 1.1);
            });
        }
        Scheduler.later(42, () -> {
            FX.shockwave(l, t.glow(1.6f), target, 9);
            FX.impactNear(l, target, 40, 1.0f, 10, 3, 0.05f);
        });
        return true;
    });

    /** Lv8: Drachensturm: der Drache windet sich in den Himmel, stürzt aufs Ziel und zerspringt in einen Pfeilregen. */
    public static final Ability DRAGON_STORM = reg("dragon_storm", NenType.XENO, 8, 80, 600, (p, l, lvl) -> {
        final Vec3 target = Targeting.aimPoint(p, 40);
        NenType t = NenType.XENO;
        FX.cutscene(p, "ult", 84, target);
        FX.anim(p, "xeno_ult");
        Dragons.coil(p, t, 20, 3.0, 14.0, true);
        FX.sound(l, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.4f, 0.7f);
        Scheduler.later(21, () -> {
            if (!p.isAlive()) return;
            Vec3 from = p.position().add(0, 14, 0);
            Dragons.fly(l, t, from, target, 9, 2.0, true, null);
            FX.sound(l, from, SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 0.6f);
        });
        Scheduler.later(31, () -> {
            for (LivingEntity e : Targeting.near(p, l, target, 6.0)) {
                Targeting.hurt(p, e, 20f);
                Targeting.push(e, horizontal(e.position().subtract(target), p.getLookAngle()).scale(1.6).add(0, 0.8, 0));
            }
            Destruction.crater(p, l, target.add(0, -0.5, 0), 4.5, 6.0, 30, null);
            FX.impact(l, t, target.add(0, 1, 0), true);
            FX.burst(l, ParticleTypes.EXPLOSION_EMITTER, target, 1, 0);
            FX.burst(l, ParticleTypes.FLASH, target, 1, 0);
            FX.sphere(l, t.glow(2.6f), target.add(0, 1, 0), 6.0, 100);
            FX.shockwave(l, t.glow(1.8f), target, 12);
            FX.sound(l, target, SoundEvents.GENERIC_EXPLODE, 1.8f, 0.5f);
            FX.sound(l, target, SoundEvents.ENDER_DRAGON_GROWL, 1.6f, 0.6f);
            FX.impactNear(l, target, 48, 2.4f, 18, 10, 0.2f);
        });
        final Vec3 top = target.add(0, 16, 0);
        for (int j = 0; j < 38; j++) {
            Scheduler.later(33 + j, () -> {
                for (int n = 0; n < 5; n++) arrow(p, l, t, target, top, 12.0, 4.0f, 1.2);
            });
        }
        return true;
    });

    // ================================================================== POUF (Shaiapouf): Schmetterlingswächter

    private static void wings(ServerPlayer p, ServerLevel l, int life, float scale, float spread) {
        wings(p, l, life, scale, spread, 8);
    }

    private static void wings(ServerPlayer p, ServerLevel l, int life, float scale, float spread, int period) {
        FxEntity.wings(l, NenType.POUF, p, life, scale, spread, period);
    }

    /** Lv1: Schuppenpuder (Spiritual Message): ein schimmernder Puderkegel markiert und schwächt Gegner. */
    public static final Ability SCALE_POWDER = reg("scale_powder", NenType.POUF, 1, 14, 60, (p, l, lvl) -> {
        NenType t = NenType.POUF;
        FX.anim(p, "pouf_powder");
        wings(p, l, 22, 1.2f, 0.4f, 10);
        Scheduler.later(4, () -> {
            if (!p.isAlive()) return;
            Vec3 look = p.getLookAngle().normalize();
            Vec3 eye = p.getEyePosition().add(0, -0.3, 0);
            for (int i = 0; i < 10; i++) {
                final int k = i;
                Scheduler.later(i, () -> {
                    Vec3 c = eye.add(look.scale(1.0 + k * 0.9));
                    double spread = 0.4 + k * 0.28;
                    FX.fx(l, ModParticles.GLOW, c, t, 6, spread);
                    FX.fx(l, ModParticles.SPARK, c, t, 4, spread);
                    FX.burst(l, ParticleTypes.END_ROD, c, 2, 0.02);
                });
            }
            for (LivingEntity e : Targeting.near(p, l, eye.add(look.scale(4.5)), 5.5)) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                if (to.lengthSqr() < 0.01 || to.normalize().dot(look) < 0.7) continue;
                Targeting.hurt(p, e, 2.5f);
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Targeting.pvpScaled(e, 100), 0));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Targeting.pvpScaled(e, 100), 0));
            }
            FX.sound(l, eye, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.6f);
            FX.sound(l, eye, SoundEvents.PHANTOM_FLAP, 0.8f, 1.4f);
            FX.impactNear(l, eye, 14, 0.25f, 5, 0, 0.04f);
        });
        return true;
    });

    /** Lv3: Flügelsturm: drei Flügelschläge schicken Luftklingen nach vorn. */
    public static final Ability WING_GALE = reg("wing_gale", NenType.POUF, 3, 28, 100, (p, l, lvl) -> {
        NenType t = NenType.POUF;
        FX.anim(p, "pouf_gale");
        wings(p, l, 26, 1.6f, 1.0f, 3);
        for (int w = 0; w < 3; w++) {
            final int wave = w;
            Scheduler.later(6 + w * 3, () -> {
                if (!p.isAlive()) return;
                p.swing(InteractionHand.MAIN_HAND, true);
                final Vec3 look = p.getLookAngle().normalize();
                final Vec3 from = p.getEyePosition().add(look.scale(1.2)).add(0, -0.4, 0);
                for (int i = 0; i < 5; i++) {
                    final int k = i;
                    Scheduler.later(i, () -> FxEntity.spawn(l, FxEntity.CLAWS, t, 6, 2.4f, 2,
                            from.add(look.scale(1.0 + k * 2.2)), from.add(look.scale(2.0 + k * 2.2)), (wave + k) % 2 == 0 ? 1f : -1f, 0f));
                }
                for (LivingEntity e : Targeting.alongRay(p, l, from, look, 11, 2.2)) {
                    Targeting.hurt(p, e, 6f);
                    Targeting.push(e, look.scale(1.3).add(0, 0.35, 0));
                    FX.impact(l, t, e.getBoundingBox().getCenter(), false);
                }
                Destruction.slit(p, l, from, look, 11, 1.2, 2.4, 0.8, 4);
                FX.line(l, t.glow(1.3f), from, from.add(look.scale(11)), 0.5);
                FX.sound(l, from, SoundEvents.PHANTOM_FLAP, 1.2f, 0.6f);
                FX.impactNear(l, from, 22, 0.55f, 5, 0, 0.05f);
            });
        }
        return true;
    });

    /** Lv4: Beelzebub-Sprung: Pouf zerfällt in einen Fliegenschwarm, rast nach vorn und setzt sich neu zusammen. */
    public static final Ability FLY_SWARM = reg("fly_swarm", NenType.POUF, 4, 30, 80, (p, l, lvl) -> {
        NenType t = NenType.POUF;
        Vec3 dir = p.getLookAngle().normalize();
        double dist = Targeting.freeDistance(p, l, dir, 16 + lvl);
        if (dist < 2.0) return false;
        final Vec3 start = p.position();
        final Vec3 end = start.add(dir.scale(dist));
        FX.anim(p, "pouf_swarm");
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 8, 0, false, false));
        final int ticks = Math.max(3, (int) (dist / 3.0));
        FxEntity.spawn(l, FxEntity.SWARM, t, ticks + 6, ticks, 1.1f, 0, start.add(0, 1, 0), end.add(0, 1, 0), 60f, 0f);
        FX.fx(l, ModParticles.GLOW, start.add(0, 1, 0), t, 12, 0.8);
        FX.fx(l, ModParticles.SPARK, start.add(0, 1, 0), t, 8, 0.8);
        // Schaden genau dann, wenn der Schwarm den Gegner erreicht (ca. 3 Blöcke pro Tick)
        final Vec3 rayStart = start.add(0, 0.9, 0);
        for (LivingEntity e : Targeting.alongRay(p, l, rayStart, dir, dist, 1.8)) {
            double along = e.getBoundingBox().getCenter().subtract(rayStart).dot(dir);
            int delay = Math.max(0, Math.min(ticks, (int) (along / 3.0)));
            Scheduler.later(delay, () -> {
                if (!e.isAlive()) return;
                Targeting.hurt(p, e, 6f);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Targeting.pvpScaled(e, 40), 0));
                Targeting.push(e, dir.scale(0.4).add(0, 0.25, 0));
                FX.impact(l, t, e.getBoundingBox().getCenter(), false);
                FX.sound(l, e.position(), SoundEvents.BEE_STING, 0.8f, 0.8f);
            });
        }
        // Schuppen-Spur entlang des Flugwegs
        for (int i = 0; i <= ticks; i++) {
            final Vec3 c = start.add(0, 1, 0).add(dir.scale(dist * i / (double) ticks));
            Scheduler.later(i, () -> {
                FX.fx(l, ModParticles.GLOW, c, t, 4, 0.7);
                FX.fx(l, ModParticles.SPARK, c, t, 2, 0.9);
                FX.burst(l, ParticleTypes.END_ROD, c, 2, 0.04);
            });
        }
        FX.sound(l, start, SoundEvents.PHANTOM_SWOOP, 1f, 1.6f);
        Scheduler.later(ticks, () -> {
            if (!p.isAlive()) return;
            p.connection.teleport(end.x, end.y, end.z, p.getYRot(), p.getXRot());
            p.fallDistance = 0;
            FX.impact(l, t, end.add(0, 1, 0), false);
            // Beim Zusammensetzen stößt der Schwarm Gegner in der Nähe weg
            for (LivingEntity e : Targeting.near(p, l, end.add(0, 1, 0), 3.2)) {
                Targeting.hurt(p, e, 3.5f);
                Vec3 away = e.position().subtract(end);
                away = new Vec3(away.x, 0, away.z);
                away = away.lengthSqr() < 1e-4 ? dir : away.normalize();
                Targeting.push(e, away.scale(0.9).add(0, 0.35, 0));
            }
            FX.shockwave(l, t.glow(1.2f), end, 3);
            wings(p, l, 14, 1.4f, 0.8f, 5);
            FX.sound(l, end, SoundEvents.PHANTOM_SWOOP, 1f, 1.2f);
            FX.impactNear(l, end, 16, 0.35f, 6, 2, 0.15f);
        });
        return true;
    });

    /** Lv6: Kokon: ein Kokon aus Seidenfäden wächst um das Ziel, hält es fest und platzt zum Schluss auf. */
    public static final Ability COCOON = reg("cocoon", NenType.POUF, 6, 45, 240, (p, l, lvl) -> {
        LivingEntity target = Targeting.ray(p, 22);
        if (target == null) return false;
        NenType t = NenType.POUF;
        FX.cutscene(p, "cocoon", 40, target.position());
        FX.anim(p, "pouf_cocoon");
        wings(p, l, 40, 1.4f, 0.5f, 12);
        int dur = Targeting.pvpScaled(target, 34);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 6));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
        Targeting.push(target, Vec3.ZERO);
        FxEntity.spawn(l, FxEntity.COCOON, t, 36, 1f, 2, target.position(), target.position(),
                target.getBbWidth() * 0.9f + 0.3f, target.getBbHeight() + 0.3f);
        for (int i = 0; i < 12; i++) {
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive() || !target.isAlive()) return;
                FX.line(l, t.glow(1.0f), p.position().add(0, 1.2, 0), target.getBoundingBox().getCenter(), 0.5);
                FX.fx(l, ModParticles.GLOW, target.getBoundingBox().getCenter(), t, 3, 0.5);
            });
        }
        Scheduler.later(34, () -> {
            if (!target.isAlive()) return;
            Vec3 c = target.getBoundingBox().getCenter();
            Targeting.hurt(p, target, 14f);
            Targeting.push(target, new Vec3(0, 0.7, 0));
            FX.impact(l, t, c, true);
            FX.sphere(l, t.glow(2.0f), c, 2.5, 50);
            FX.sound(l, c, SoundEvents.TURTLE_EGG_CRACK, 1.2f, 0.7f);
            FX.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f);
            FX.impactNear(l, c, 24, 0.9f, 8, 3, 0.08f);
        });
        return true;
    });

    /** Lv8: Schwarmsturm: riesige Flügel, ein Fliegenschwarm verschlingt das Zielgebiet und heilt Pouf. */
    public static final Ability SWARM_STORM = reg("swarm_storm", NenType.POUF, 8, 75, 560, (p, l, lvl) -> {
        final Vec3 target = Targeting.aimPoint(p, 30);
        NenType t = NenType.POUF;
        FX.cutscene(p, "swarm", 70, target);
        FX.anim(p, "pouf_ult");
        wings(p, l, 68, 2.8f, 1.0f, 10);
        p.setDeltaMovement(p.getDeltaMovement().x, 0.9, p.getDeltaMovement().z);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0, false, false));
        Pending.NO_FALL.put(p.getUUID(), Scheduler.now() + 160);
        FX.sound(l, p.position(), SoundEvents.PHANTOM_FLAP, 1.5f, 0.5f);
        for (int i = 0; i < 14; i++) {
            final int k = i;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                Vec3 c = p.position().add(0, 1.3, 0);
                FX.sphere(l, t.glow(1.4f), c, 0.6 + k * 0.25, 14);
                FX.fx(l, ModParticles.GLOW, c, t, 4, 1.5);
                FX.shake(p, 0.05f + k * 0.01f, 2);
            });
        }
        Scheduler.later(15, () -> {
            if (!p.isAlive()) return;
            Vec3 s = p.position().add(0, 2, 0);
            FxEntity.spawn(l, FxEntity.SWARM, t, 14, 8, 2.0f, 0, s, target.add(0, 2, 0), 120f, 0f);
            FX.sound(l, s, SoundEvents.PHANTOM_SWOOP, 1.6f, 0.7f);
        });
        Scheduler.later(23, () -> {
            FxEntity.spawn(l, FxEntity.SWARM, t, 46, 46, 9.0f, 2, target.add(0, 2.5, 0), target.add(0, 2.5, 0), 180f, 0f);
            FX.impact(l, t, target.add(0, 1, 0), true);
            FX.impactNear(l, target, 40, 1.4f, 14, 6, 0.15f);
        });
        for (int j = 0; j < 40; j++) {
            Scheduler.later(24 + j, () -> {
                FX.fx(l, ModParticles.GLOW, target.add(0, 2.5, 0), t, 8, 8.0);
                FX.fx(l, ModParticles.SPARK, target.add(0, 2.5, 0), t, 4, 8.0);
            });
        }
        for (int j = 0; j < 8; j++) {
            Scheduler.later(26 + j * 5, () -> {
                boolean any = false;
                for (LivingEntity e : Targeting.near(p, l, target.add(0, 2, 0), 9.0)) {
                    Targeting.hurt(p, e, 5.5f);
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Targeting.pvpScaled(e, 30), 0));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Targeting.pvpScaled(e, 20), 1));
                    any = true;
                }
                if (any && p.isAlive()) p.heal(1.0f);
                FX.sound(l, target, SoundEvents.BEE_STING, 1.0f, 0.6f);
            });
        }
        Scheduler.later(66, () -> {
            FX.shockwave(l, t.glow(1.8f), target, 12);
            FX.impact(l, t, target.add(0, 1, 0), true);
            FX.impactNear(l, target, 40, 1.2f, 10, 4, 0.08f);
        });
        return true;
    });

    private Abilities() {}
}
