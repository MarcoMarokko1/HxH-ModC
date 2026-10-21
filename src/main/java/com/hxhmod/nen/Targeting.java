package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Zielsuche, Schaden und Rückstoß. */
public final class Targeting {

    public static boolean valid(ServerPlayer p, Entity e) {
        return e instanceof LivingEntity le && le != p && le.isAlive() && !e.isSpectator()
                && !(e instanceof ArmorStand) && !(e instanceof TamableAnimal t && t.isOwnedBy(p));
    }

    /** Erstes gültiges Lebewesen im Blickfeld (Blöcke blockieren die Sicht). */
    public static LivingEntity ray(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        boolean miss = bh.getType() == HitResult.Type.MISS;
        Vec3 limit = miss ? end : bh.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, eye, limit, box, e -> valid(p, e), range * range);
        return r == null ? null : (LivingEntity) r.getEntity();
    }

    /** Zielpunkt: Mitte des getroffenen Wesens, sonst Blocktreffer, sonst Maximalreichweite. */
    public static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity e = ray(p, range);
        if (e != null) return e.getBoundingBox().getCenter();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        return p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getLocation();
    }

    public static List<LivingEntity> near(ServerPlayer p, ServerLevel l, Vec3 c, double r) {
        AABB box = new AABB(c.subtract(r, r, r), c.add(r, r, r));
        return l.getEntitiesOfClass(LivingEntity.class, box,
                e -> valid(p, e) && e.getBoundingBox().getCenter().distanceToSqr(c) <= r * r);
    }

    /** Alle gültigen Lebewesen in einem Korridor entlang einer (normalisierten) Richtung. */
    public static List<LivingEntity> alongRay(ServerPlayer p, ServerLevel l, Vec3 origin, Vec3 dir, double len, double halfWidth) {
        AABB box = new AABB(origin, origin.add(dir.scale(len))).inflate(halfWidth);
        return l.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!valid(p, e)) return false;
            Vec3 v = e.getBoundingBox().getCenter().subtract(origin);
            double t = v.dot(dir);
            if (t < 0 || t > len) return false;
            return v.subtract(dir.scale(t)).length() <= halfWidth + e.getBbWidth() / 2;
        });
    }

    /** Wie weit kann der Spieler in dieser Richtung gehen, ohne in Blöcke zu geraten? */
    public static double freeDistance(ServerPlayer p, ServerLevel l, Vec3 dir, double max) {
        double dist = 0;
        for (double s = 0.5; s <= max; s += 0.5) {
            if (!l.noCollision(p, p.getBoundingBox().move(dir.scale(s)))) break;
            dist = s;
        }
        return dist;
    }

    /** Skalierter Nen-Schaden (Level-Bonus, Config, PvP-Faktor). */
    public static void hurt(ServerPlayer p, LivingEntity t, float base) {
        double dmg = base * (1.0 + 0.08 * (NenData.getLevel(p) - 1)) * HxhConfig.DAMAGE_MULT.get();
        if (t instanceof Player) dmg *= HxhConfig.PVP_MULT.get();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), (float) dmg);
    }

    public static int pvpScaled(LivingEntity t, int ticks) {
        return t instanceof Player ? (int) (ticks * HxhConfig.PVP_MULT.get()) : ticks;
    }

    public static void push(LivingEntity t, Vec3 v) {
        t.setDeltaMovement(v);
        t.hurtMarked = true;
    }

    private Targeting() {}
}
