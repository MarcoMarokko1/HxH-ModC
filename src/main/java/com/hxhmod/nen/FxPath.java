package com.hxhmod.nen;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Bahnformeln der 3D-Effekte. Gleiche Mathe auf Server und Client, deshalb muss die Position nie übertragen werden. */
public final class FxPath {

    private static Vec3 baseDir(FxEntity e) {
        Vec3 d = e.p1.subtract(e.p0);
        return d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /** ticks darf Bruchteile enthalten und negativ sein (für Körpersegmente hinter dem Kopf). */
    public static Vec3 position(FxEntity e, double ticks) {
        double raw = ticks / Math.max(1, e.pathTicks);
        switch (e.path) {
            case 0: { // Gerade mit Schlangenbewegung (f0 = Ausschlag)
                Vec3 d = e.p1.subtract(e.p0);
                if (raw <= 0) return e.p0.add(d.scale(raw));
                double u = Math.min(raw, 1.0);
                if (e.f0 == 0f) return e.p0.add(d.scale(u));
                Vec3 dir = baseDir(e);
                Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = dir.cross(up).normalize();
                Vec3 upv = right.cross(dir).normalize();
                double env = Math.sin(Math.PI * u);
                return e.p0.add(d.scale(u))
                        .add(right.scale(Math.sin(u * Math.PI * 4) * e.f0 * env))
                        .add(upv.scale(Math.cos(u * Math.PI * 3) * e.f0 * 0.6 * env));
            }
            case 1: { // Spirale um p0 (f0 = Radius, f1 = Höhe)
                double tt = Math.max(0, ticks);
                double u = Math.min(1.0, tt / Math.max(1, e.pathTicks));
                double a = tt * 0.6;
                double r = e.f0 * (1.0 - 0.25 * u);
                return e.p0.add(Math.cos(a) * r, u * e.f1 + 0.3, Math.sin(a) * r);
            }
            case 3: { // Halten (f0 = Anteil der Lebensdauer), dann Schlag nach p1 (bis f1)
                double u = ticks / Math.max(1, e.life);
                if (u <= e.f0) return e.p0;
                if (u >= e.f1) return e.p1;
                double s = (u - e.f0) / Math.max(1e-4, e.f1 - e.f0);
                s = 1.0 - (1.0 - s) * (1.0 - s);
                return e.p0.add(e.p1.subtract(e.p0).scale(s));
            }
            default:
                return e.p0;
        }
    }

    public static Vec3 tangent(FxEntity e, double ticks) {
        Vec3 d = position(e, ticks + 0.4).subtract(position(e, ticks - 0.4));
        return d.lengthSqr() < 1e-8 ? baseDir(e) : d.normalize();
    }

    private FxPath() {}
}
