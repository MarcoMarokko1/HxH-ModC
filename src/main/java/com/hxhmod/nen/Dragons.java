package com.hxhmod.nen;

import com.hxhmod.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Xenos Aura-Drachen: ein Drachenkopf (Billboard) mit leuchtendem Schlangenkörper aus Partikeln. */
public final class Dragons {

    @FunctionalInterface
    public interface Visit {
        void at(Vec3 pos, Vec3 dir, int i);
    }

    private static void body(ServerLevel l, NenType t, Vec3 pos, int i) {
        FX.fx(l, ModParticles.FLAME, pos, t, 2, 0.35);
        FX.fx(l, ModParticles.GLOW, pos, t, 2, 0.45);
        if (i % 2 == 0) FX.fx(l, ModParticles.SPARK, pos, t, 1, 0.6);
        if (i % 3 == 0) FX.fx(l, ModParticles.RING, pos, t, 1, 0);
    }

    public static void head(ServerLevel l, NenType t, Vec3 pos, boolean big) {
        FX.fx(l, big ? ModParticles.DRAGON_BIG : ModParticles.DRAGON, pos, t, 1, 0);
        FX.fx(l, ModParticles.BLAST, pos, t, 1, 0);
    }

    private static Vec3 pathAt(Vec3 from, Vec3 d, Vec3 right, Vec3 upv, double wobble, double u) {
        double env = Math.sin(Math.PI * u);
        return from.add(d.scale(u))
                .add(right.scale(Math.sin(u * Math.PI * 4) * wobble * env))
                .add(upv.scale(Math.cos(u * Math.PI * 3) * wobble * 0.6 * env));
    }

    /** Der Drache schlängelt sich in `ticks` Ticks von `from` nach `to`. visit wird pro Tick mit der Kopfposition aufgerufen. */
    public static void fly(ServerLevel l, NenType t, Vec3 from, Vec3 to, int ticks, double wobble, boolean big, Visit visit) {
        FxEntity.dragonFly(l, t, from, to, ticks, wobble, big);
        final Vec3 d = to.subtract(from);
        final Vec3 dir = d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
        Vec3 upRef = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        final Vec3 right = dir.cross(upRef).normalize();
        final Vec3 upv = right.cross(dir).normalize();
        for (int i = 1; i <= ticks; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                Vec3 prev = pathAt(from, d, right, upv, wobble, (k - 1) / (double) ticks);
                Vec3 pos = pathAt(from, d, right, upv, wobble, k / (double) ticks);
                for (int s = 0; s < 3; s++) body(l, t, prev.lerp(pos, s / 3.0), k + s);
                if (visit != null) visit.at(pos, dir, k);
            });
        }
    }

    /** Ein Drache schwebt kreisend um einen Punkt (z. B. über dem Ziel beim Dragon Dive). */
    public static void hover(ServerLevel l, NenType t, Vec3 center, int ticks, double radius, double rise, boolean big) {
        FxEntity.dragonCoil(l, t, center, ticks, radius, rise, big);
    }

    private static Vec3 helix(Vec3 base, double radius, double rise, int k, int ticks) {
        double a = k * 0.6;
        double u = k / (double) ticks;
        double r = radius * (1.0 - 0.25 * u);
        return base.add(Math.cos(a) * r, u * rise + 0.3, Math.sin(a) * r);
    }

    /** Der Drache windet sich spiralförmig um den Spieler nach oben. */
    public static void coil(ServerPlayer p, NenType t, int ticks, double radius, double rise, boolean big) {
        ServerLevel l = p.serverLevel();
        FxEntity.dragonCoil(l, t, p.position(), ticks, radius, rise, big);
        for (int i = 1; i <= ticks; i++) {
            final int k = i;
            Scheduler.later(i, () -> {
                if (!p.isAlive()) return;
                Vec3 base = p.position();
                Vec3 prev = helix(base, radius, rise, k - 1, ticks);
                Vec3 pos = helix(base, radius, rise, k, ticks);
                for (int s = 0; s < 3; s++) body(l, t, prev.lerp(pos, s / 3.0), k + s);
            });
        }
    }

    private Dragons() {}
}
