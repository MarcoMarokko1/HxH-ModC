package com.hxhmod.nen;

import com.hxhmod.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nahkampf-Kombos auf Linksklick (leere Hand, Gegner in der Nähe): Gon 4 Schläge, Killua 5 Hiebe.
 * Pro Schlag: Körper-Animation, Ausfallschritt zum Gegner, Waffen-Schweif, Trefferframe mit Hitstop.
 */
public final class Combos {

    private static final class State { int step; long lockedUntil; long windowEnd; }

    private static final Map<UUID, State> STATES = new HashMap<>();

    /** kind: 0 normal, 1 Uppercut, 2 Gon-Finisher (Krater), 3 Drehtritt (Flächenschaden), 4 Killua-Finisher (Blitze). */
    private record Step(String anim, int lock, int hit, float dmg, double lunge, double radius, double launch,
                        double knock, int kind, double[] a, double[] c, double[] b, int trail) {}

    private static final Step[] GON = {
            new Step("gon_c1", 8, 3, 3.5f, 0.35, 2.0, 0.0, 0.6, 0, new double[]{0.3, 1.3, 0.2}, new double[]{0.1, 1.35, 1.6}, new double[]{0, 1.3, 2.6}, 3),
            new Step("gon_c2", 9, 3, 4.0f, 0.30, 2.2, 0.0, 0.7, 0, new double[]{-1.8, 1.3, 1.0}, new double[]{-0.6, 1.4, 2.2}, new double[]{1.4, 1.3, 1.2}, 4),
            new Step("gon_c3", 11, 4, 5.0f, 0.25, 2.0, 0.9, 0.3, 1, new double[]{0.3, 0.4, 1.0}, new double[]{0.4, 1.2, 2.0}, new double[]{0.2, 2.4, 1.4}, 4),
            new Step("gon_c4", 18, 7, 8.5f, 0.55, 2.6, 0.2, 1.7, 2, new double[]{0.5, 2.6, 0.2}, new double[]{0.6, 2.4, 1.4}, new double[]{0, 0.5, 2.4}, 5)};

    private static final Step[] KILLUA = {
            new Step("killua_c1", 7, 2, 3.0f, 0.40, 2.0, 0.0, 0.5, 0, new double[]{-1.2, 2.0, 0.8}, new double[]{0, 1.6, 1.8}, new double[]{1.3, 0.7, 1.2}, 3),
            new Step("killua_c2", 7, 2, 3.0f, 0.40, 2.0, 0.0, 0.5, 0, new double[]{1.2, 2.0, 0.8}, new double[]{0, 1.6, 1.8}, new double[]{-1.3, 0.7, 1.2}, 3),
            new Step("killua_c3", 9, 4, 3.5f, 0.30, 2.8, 0.0, 0.9, 3, new double[]{0, 0, 0}, new double[]{0, 0, 0}, new double[]{0, 0, 0}, 0),
            new Step("killua_c4", 10, 3, 4.5f, 0.35, 2.2, 0.0, 0.6, 0, new double[]{-1.4, 2.2, 0.9}, new double[]{0, 1.2, 1.8}, new double[]{1.4, 0.3, 1.1}, 3),
            new Step("killua_c5", 16, 3, 6.5f, 1.20, 2.4, 0.2, 1.1, 4, new double[]{0.4, 1.0, 0.2}, new double[]{0.2, 1.1, 1.8}, new double[]{0, 1.2, 3.2}, 4)};

    private static final Step[] XENO = {
            new Step("xeno_c1", 8, 3, 3.5f, 0.35, 2.0, 0.0, 0.6, 0, new double[]{0.3, 1.3, 0.2}, new double[]{0.1, 1.35, 1.6}, new double[]{0, 1.3, 2.6}, 3),
            new Step("xeno_c2", 9, 3, 4.0f, 0.30, 2.2, 0.0, 0.7, 0, new double[]{1.8, 1.3, 1.0}, new double[]{0.6, 1.4, 2.2}, new double[]{-1.4, 1.3, 1.2}, 4),
            new Step("xeno_c3", 10, 4, 4.0f, 0.30, 2.8, 0.0, 1.0, 3, new double[]{0, 0, 0}, new double[]{0, 0, 0}, new double[]{0, 0, 0}, 0),
            new Step("xeno_c4", 18, 5, 9.0f, 0.55, 2.8, 0.2, 1.8, 5, new double[]{-0.8, 1.2, 0.2}, new double[]{-0.2, 1.3, 1.8}, new double[]{0, 1.3, 3.0}, 4)};

    private static final Step[] POUF = {
            new Step("pouf_c1", 7, 3, 3.2f, 0.35, 2.0, 0.0, 0.6, 0, new double[]{1.2, 1.4, 0.4}, new double[]{0, 1.4, 1.6}, new double[]{-1.3, 1.2, 1.2}, 3),
            new Step("pouf_c2", 7, 3, 3.6f, 0.35, 2.0, 0.0, 0.6, 0, new double[]{-1.2, 1.4, 0.4}, new double[]{0, 1.4, 1.6}, new double[]{1.3, 1.2, 1.2}, 3),
            new Step("pouf_c3", 10, 4, 3.8f, 0.30, 3.0, 0.0, 1.0, 3, new double[]{0, 0, 0}, new double[]{0, 0, 0}, new double[]{0, 0, 0}, 0),
            new Step("pouf_c4", 18, 5, 8.0f, 0.50, 2.8, 0.2, 1.6, 6, new double[]{-1.2, 2.2, 0.4}, new double[]{0, 1.4, 1.8}, new double[]{1.2, 0.4, 1.2}, 4)};

    private static Vec3 horizontal(Vec3 d, Vec3 fallback) {
        Vec3 h = d.multiply(1, 0, 1);
        return h.lengthSqr() < 0.01 ? fallback.multiply(1, 0, 1).normalize() : h.normalize();
    }

    private static LivingEntity nearest(ServerPlayer p, ServerLevel l, double range) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        LivingEntity best = null;
        double bd = range * range;
        for (LivingEntity e : Targeting.near(p, l, p.position().add(0, 1, 0), range)) {
            Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && look.lengthSqr() > 0.01 && to.normalize().dot(look.normalize()) < 0.1) continue;
            double d = e.distanceToSqr(p);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    public static void attack(ServerPlayer p) {
        NenType t = NenData.getType(p);
        if (t == null || NenData.getStance(p) == 2 || !p.isAlive()) return;
        Step[] steps = switch (t) {
            case ENHANCER -> GON;
            case TRANSMUTER -> KILLUA;
            case POUF -> POUF;
            default -> XENO;
        };
        long now = Scheduler.now();
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        if (now < s.lockedUntil) return;
        if (now > s.windowEnd) s.step = 0;
        Step st = steps[s.step];
        s.step = (s.step + 1) % steps.length;
        s.lockedUntil = now + st.lock();
        s.windowEnd = s.lockedUntil + 12;

        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        LivingEntity target = nearest(p, l, 5.0);
        final Vec3 dir = target != null ? horizontal(target.position().subtract(p.position()), look) : horizontal(look, look);
        double dist = target == null ? 99 : target.position().distanceTo(p.position());
        if (dist > 1.5) {
            p.setDeltaMovement(dir.x * st.lunge(), p.onGround() ? 0.08 : p.getDeltaMovement().y, dir.z * st.lunge());
            p.hurtMarked = true;
        }
        FX.anim(p, st.anim());
        if (t == NenType.POUF && (st.kind() == 3 || st.kind() == 6))
            FxEntity.wings(l, t, p, 20, 1.3f, 0.9f, 4);
        if (st.trail() > 0) FX.swing(l, p, t, st.a(), st.c(), st.b(), st.trail());
        if (st.kind() == 5) {
            Vec3 hand = p.getEyePosition().add(dir.scale(0.8)).add(0, -0.5, 0);
            Dragons.fly(l, t, hand, hand.add(dir.scale(3.2)), st.hit(), 0.5, false, null);
        }
        Scheduler.later(st.hit(), () -> doHit(p, l, t, st, dir));
        NenLogic.gainXp(p, 1);
    }

    private static void doHit(ServerPlayer p, ServerLevel l, NenType t, Step st, Vec3 dir) {
        if (!p.isAlive()) return;
        Vec3 center = p.position().add(dir.scale(1.7)).add(0, 1.0, 0);
        switch (t) {
            case ENHANCER -> FxEntity.spawn(l, FxEntity.FIST, t, 5, st.kind() == 2 ? 1.9f : 0.9f, 0,
                    p.position().add(dir.scale(0.6)).add(0, 1.2, 0), center, 0f, 0f);
            default -> {
                if (st.kind() == 0) {
                    FxEntity.spawn(l, FxEntity.CLAWS, t, 6, 1.1f, 2, center, center.add(dir), l.random.nextBoolean() ? 1f : -1f, 0f);
                } else if (st.kind() == 3) {
                    FxEntity.spawn(l, FxEntity.TORNADO, t, 9, 0.7f, 2, p.position(), p.position(), 0f, 0f);
                } else if (st.kind() == 4) {
                    FxEntity.spawn(l, FxEntity.BOLT, t, 8, 1.0f, 2, p.position().add(0, 1, 0), center, 0f, 0f);
                }
            }
        }
        List<LivingEntity> hits = Targeting.near(p, l, center, st.radius());
        for (LivingEntity e : hits) {
            Targeting.hurt(p, e, st.dmg());
            Vec3 away = horizontal(e.position().subtract(p.position()), dir);
            Targeting.push(e, away.scale(st.knock()).add(0, 0.15 + st.launch(), 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 5, false, false));
            Vec3 ec = e.getBoundingBox().getCenter();
            FX.blast(l, t, ec, false);
            FX.fx(l, ModParticles.SPARK, ec, t, 6, 0.6);
        }
        boolean finisher = st.kind() == 2 || st.kind() == 4 || st.kind() == 5 || st.kind() == 6;
        if (!hits.isEmpty()) {
            // Hitstop: kurzer Stillstand wie bei Epic Fight
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 5, false, false));
            FX.impactNear(l, center, 18, finisher ? 0.9f : 0.4f, finisher ? 8 : 4, 0, finisher ? 0.06f : 0.03f);
            FX.sound(l, center, finisher ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.9f + l.random.nextFloat() * 0.2f);
        } else {
            FX.sound(l, center, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8f, 1.1f);
        }
        switch (st.kind()) {
            case 2 -> {
                Destruction.crater(p, l, center.add(0, -0.7, 0), 2.3, 3.0, 8, dir);
                FX.impact(l, t, center, true);
                FX.shockwave(l, t.glow(1.4f), p.position().add(dir.scale(1.5)), 4);
                FX.sound(l, center, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.1f);
            }
            case 3 -> {
                FX.shockwave(l, t.glow(1.5f), p.position(), 3.5);
                FX.fx(l, t == NenType.TRANSMUTER ? ModParticles.BOLT : t == NenType.POUF ? ModParticles.GLOW : ModParticles.FLAME, p.position().add(0, 1, 0), t, 4, 1.6);
            }
            case 6 -> {
                FX.impact(l, t, center, true);
                FxEntity.spawn(l, FxEntity.CLAWS, t, 7, 2.2f, 2, center, center.add(dir), 1f, 0f);
                FxEntity.spawn(l, FxEntity.CLAWS, t, 7, 2.2f, 2, center, center.add(dir), -1f, 0f);
                FX.fx(l, ModParticles.GLOW, center, t, 10, 1.2);
                FX.sound(l, center, SoundEvents.PHANTOM_FLAP, 1.0f, 0.7f);
            }
            case 5 -> {
                Destruction.crater(p, l, center.add(0, -0.7, 0), 2.0, 3.0, 6, dir);
                FX.impact(l, t, center, true);
                FX.sound(l, center, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            }
            case 4 -> {
                for (LivingEntity e : hits) FX.arc(l, t.glow(1.2f), p.position().add(0, 1, 0), e.getBoundingBox().getCenter(), 0.4);
                FX.fx(l, ModParticles.BOLT, center, t, 4, 1.2);
                FX.sound(l, center, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
            }
            default -> { }
        }
    }

    public static void clear(UUID id) { STATES.remove(id); }

    private Combos() {}
}
