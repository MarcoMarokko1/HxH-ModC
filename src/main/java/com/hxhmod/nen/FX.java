package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import com.hxhmod.ModParticles;
import com.hxhmod.network.AnimPacket;
import com.hxhmod.network.CutscenePacket;
import com.hxhmod.network.ModNetwork;
import com.hxhmod.network.ShakePacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Partikel-, Sound-, Kamera- und Animations-Helfer. Alles serverseitig gesendet, damit jeder die Effekte sieht. */
public final class FX {

    public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int pts) {
        for (int i = 0; i < pts; i++) {
            double a = Math.PI * 2 * i / pts;
            l.sendParticles(o, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Ring in beliebiger Ebene (senkrecht zu axis). */
    public static void ringAxis(ServerLevel l, ParticleOptions o, Vec3 c, Vec3 axis, double r, int pts) {
        Vec3 a = axis.normalize();
        Vec3 helper = Math.abs(a.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = a.cross(helper).normalize();
        Vec3 v = a.cross(u).normalize();
        for (int i = 0; i < pts; i++) {
            double ang = Math.PI * 2 * i / pts;
            Vec3 q = c.add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
            l.sendParticles(o, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    public static void line(ServerLevel l, ParticleOptions o, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) return;
        Vec3 dir = d.scale(1.0 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 q = a.add(dir.scale(t));
            l.sendParticles(o, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** Gezackter Blitzbogen zwischen zwei Punkten. */
    public static void arc(ServerLevel l, ParticleOptions dust, Vec3 a, Vec3 b, double jitter) {
        Vec3 d = b.subtract(a);
        int segs = Math.max(2, (int) (d.length() / 0.9));
        Vec3 prev = a;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.add(d.scale(i / (double) segs));
            if (i < segs) {
                q = q.add((l.random.nextDouble() - 0.5) * 2 * jitter, (l.random.nextDouble() - 0.5) * 2 * jitter,
                        (l.random.nextDouble() - 0.5) * 2 * jitter);
            }
            line(l, dust, prev, q, 0.25);
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.02);
            prev = q;
        }
    }

    public static void burst(ServerLevel l, ParticleOptions o, Vec3 c, int n, double speed) {
        l.sendParticles(o, c.x, c.y, c.z, n, 0.3, 0.3, 0.3, speed);
    }

    public static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double y = 1 - (i / (double) Math.max(1, n - 1)) * 2;
            double rad = Math.sqrt(1 - y * y);
            double th = golden * i;
            l.sendParticles(o, c.x + Math.cos(th) * rad * r, c.y + y * r, c.z + Math.sin(th) * rad * r, 1, 0, 0, 0, 0);
        }
    }

    public static void column(ServerLevel l, ParticleOptions o, Vec3 base, double height, double step) {
        line(l, o, base, base.add(0, height, 0), step);
    }

    /** Sich ausbreitende Bodenringe (Schockwelle). */
    public static void shockwave(ServerLevel l, ParticleOptions o, Vec3 c, double maxR) {
        for (int i = 1; i <= 8; i++) {
            final double r = maxR * i / 8.0;
            Scheduler.later(i, () -> ring(l, o, c.add(0, 0.15, 0), r, (int) Math.max(12, r * 10)));
        }
    }

    public static void sound(ServerLevel l, Vec3 c, SoundEvent s, float vol, float pitch) {
        l.playSound(null, c.x, c.y, c.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** Körper-Animation (PlayerAnimator) für den Spieler und alle, die ihn sehen. */
    public static void anim(ServerPlayer p, String name) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new AnimPacket(p.getId(), name));
    }

    public static void shake(ServerPlayer p, float intensity, int ticks) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ShakePacket(intensity, ticks, 0, 0f));
    }

    public static void shakeNear(ServerLevel l, Vec3 c, double radius, float intensity, int ticks) {
        impactNear(l, c, radius, intensity, ticks, 0, 0f);
    }

    /** Kamerawackeln + weißer Blitz + FOV-Stoß für alle Spieler in der Nähe. */
    public static void impactNear(ServerLevel l, Vec3 c, double radius, float intensity, int ticks, int flash, float fov) {
        for (ServerPlayer sp : l.players()) {
            double d = sp.position().distanceTo(c);
            if (d <= radius) {
                float f = (float) (1.0 - 0.6 * d / radius);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new ShakePacket(intensity * f, ticks, d < radius * 0.5 ? flash : 0, fov * f));
            }
        }
    }

    private static float[] bright(NenType t) {
        return new float[]{Math.min(1f, t.r * 0.65f + 0.35f), Math.min(1f, t.g * 0.65f + 0.35f), Math.min(1f, t.b * 0.65f + 0.35f)};
    }

    /** Eigene Partikel: Farbe wird als "Geschwindigkeit" (r,g,b) übertragen. count Stück, zufällig um p verteilt. */
    public static void fx(ServerLevel l, RegistryObject<SimpleParticleType> type, Vec3 p, NenType t, int count, double spread) {
        float[] c = bright(t);
        for (int i = 0; i < count; i++) {
            double ox = (l.random.nextDouble() - 0.5) * 2 * spread;
            double oy = (l.random.nextDouble() - 0.5) * 2 * spread;
            double oz = (l.random.nextDouble() - 0.5) * 2 * spread;
            l.sendParticles(type.get(), p.x + ox, p.y + oy, p.z + oz, 0, c[0], c[1], c[2], 1.0);
        }
    }

    public static void blast(ServerLevel l, NenType t, Vec3 p, boolean big) {
        fx(l, big ? ModParticles.BLAST_BIG : ModParticles.BLAST, p, t, 1, 0);
    }

    /** Kompletter Einschlag: Sternenblitz, Druckring, Funken und Flammen. */
    public static void impact(ServerLevel l, NenType t, Vec3 p, boolean big) {
        blast(l, t, p, big);
        fx(l, ModParticles.RING, p, t, 1, 0);
        if (big) Scheduler.later(3, () -> fx(l, ModParticles.RING, p, t, 1, 0));
        fx(l, ModParticles.SPARK, p, t, big ? 16 : 8, big ? 2.5 : 1.2);
        fx(l, ModParticles.FLAME, p, t, big ? 10 : 5, big ? 2.0 : 1.0);
    }

    /** Kamera-Cutscene für den Spieler (wenn in der Config aktiviert). */
    public static void cutscene(ServerPlayer p, String id, int ticks, Vec3 focus) {
        if (!HxhConfig.CUTSCENES.get()) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CutscenePacket(id, ticks, focus.x, focus.y, focus.z));
    }

    private static double[] bez(double[] a, double[] c, double[] b, double u) {
        double v = 1 - u;
        return new double[]{v * v * a[0] + 2 * v * u * c[0] + u * u * b[0],
                v * v * a[1] + 2 * v * u * c[1] + u * u * b[1],
                v * v * a[2] + 2 * v * u * c[2] + u * u * b[2]};
    }

    /** Leuchtender Schweif einer Schlagbewegung: Bezier-Bogen (rechts, hoch, vorn) relativ zum Spieler, über mehrere Ticks. */
    public static void swing(ServerLevel l, ServerPlayer p, NenType t, double[] a, double[] c, double[] b, int ticks) {
        for (int i = 0; i < ticks; i++) {
            final double u0 = i / (double) ticks;
            final double u1 = (i + 1) / (double) ticks;
            Scheduler.later(i + 1, () -> {
                if (!p.isAlive()) return;
                float yaw = p.getYRot() * Mth.DEG_TO_RAD;
                Vec3 f = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
                Vec3 r = new Vec3(-f.z, 0, f.x);
                Vec3 base = p.position();
                double[] q0 = bez(a, c, b, u0);
                double[] q1 = bez(a, c, b, u1);
                Vec3 pa = base.add(r.scale(q0[0])).add(0, q0[1], 0).add(f.scale(q0[2]));
                Vec3 pb = base.add(r.scale(q1[0])).add(0, q1[1], 0).add(f.scale(q1[2]));
                line(l, t.glow(1.3f), pa, pb, 0.12);
                fx(l, ModParticles.STREAK, pa.add(pb).scale(0.5), t, 1, 0.0);
                fx(l, ModParticles.GLOW, pb, t, 2, 0.12);
            });
        }
    }

    /** Dauer-Aura um den Spieler (alle 4 Ticks aufgerufen). Zetsu = keine Aura. */
    public static void aura(ServerLevel l, ServerPlayer p, NenType t, int stance) {
        if (stance == 2) return;
        double h = p.getBbHeight();
        Vec3 base = p.position();
        if (stance == 3) {
            int lvl = NenData.getLevel(p);
            fx(l, ModParticles.FLAME, base.add(0, 0.1, 0), t, 3 + lvl / 2, 0.55);
            fx(l, ModParticles.FLAME, base.add(0, h * 0.6, 0), t, 2, 0.5);
            fx(l, ModParticles.GLOW, base.add(0, h * 0.5, 0), t, 4, 0.6);
            double pulse = 0.8 + 0.5 * Math.abs(Math.sin(p.tickCount * 0.15));
            ring(l, t.dust(0.9f), base.add(0, 0.05, 0), pulse, 14);
            if (p.tickCount % 12 == 0) fx(l, ModParticles.RING, base.add(0, 0.15, 0), t, 1, 0);
            if (t == NenType.XENO && p.tickCount % 80 == 0) Dragons.coil(p, t, 16, 1.6, 2.6, false);
            if (t == NenType.POUF && p.tickCount % 40 == 0)
                FxEntity.wings(l, t, p, 52, 1.3f, 0.3f, 20);
            if (t == NenType.TRANSMUTER) {
                fx(l, ModParticles.BOLT, base.add(0, h * 0.5, 0), t, 1, 0.9);
                if (l.random.nextFloat() < 0.5f) {
                    Vec3 c = base.add(0, h * 0.5, 0);
                    Vec3 q = c.add((l.random.nextDouble() - 0.5) * 3, (l.random.nextDouble() - 0.3) * 2, (l.random.nextDouble() - 0.5) * 3);
                    arc(l, t.glow(0.9f), c, q, 0.25);
                }
            } else {
                fx(l, ModParticles.SPARK, base.add(0, h * 0.5, 0), t, 2, 0.9);
            }
        } else {
            fx(l, ModParticles.GLOW, base.add(0, h * 0.5, 0), t, stance == 1 ? 3 : 1, 0.5);
        }
    }

    private FX() {}
}
