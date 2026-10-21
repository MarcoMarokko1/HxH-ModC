package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Blockzerstörung mit herumfliegenden Trümmern. Respektiert Config, Härte-Limit und Schutz-Mods (BreakEvent). */
public final class Destruction {

    private static boolean cheap(ServerLevel l, BlockPos pos, double maxHard) {
        BlockState st = l.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty()) return false;
        float hard = st.getDestroySpeed(l, pos);
        if (hard < 0 || hard > Math.min(maxHard, HxhConfig.MAX_HARDNESS.get())) return false;
        return l.getBlockEntity(pos) == null;
    }

    private static boolean allowed(ServerPlayer p, ServerLevel l, BlockPos pos, BlockState st) {
        if (!l.mayInteract(p, pos)) return false;
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l, pos, st, p));
    }

    /** Kugelförmiger Krater mit ausgefranstem Rand. outward (optional) gibt den Trümmern eine Flugrichtung. */
    public static int crater(ServerPlayer p, ServerLevel l, Vec3 center, double radius, double maxHard, int debris, Vec3 outward) {
        if (!HxhConfig.BLOCK_DAMAGE.get()) return 0;
        int r = (int) Math.ceil(radius);
        BlockPos c = BlockPos.containing(center);
        List<BlockPos> cand = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z) + (l.random.nextDouble() - 0.5) * 0.9;
                    if (d > radius) continue;
                    BlockPos pos = c.offset(x, y, z);
                    if (cheap(l, pos, maxHard)) cand.add(pos);
                }
            }
        }
        return remove(p, l, cand, center, debris, outward);
    }

    /** Schneise: Quader entlang einer Richtung (Jajanken Choki/Pa, Godspeed, Gu). */
    public static int slit(ServerPlayer p, ServerLevel l, Vec3 start, Vec3 dir, double length, double halfWidth,
                           double height, double maxHard, int debris) {
        if (!HxhConfig.BLOCK_DAMAGE.get()) return 0;
        Vec3 d = dir.normalize();
        Vec3 up = Math.abs(d.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = d.cross(up).normalize();
        Vec3 upv = right.cross(d).normalize();
        Set<BlockPos> cand = new LinkedHashSet<>();
        for (double s = 0.5; s <= length; s += 0.5) {
            for (double a = -halfWidth; a <= halfWidth; a += 0.5) {
                for (double b = -height / 2; b <= height / 2; b += 0.5) {
                    BlockPos pos = BlockPos.containing(start.add(d.scale(s)).add(right.scale(a)).add(upv.scale(b)));
                    if (cheap(l, pos, maxHard)) cand.add(pos);
                }
            }
        }
        return remove(p, l, cand, start, debris, d);
    }

    private static int remove(ServerPlayer p, ServerLevel l, Collection<BlockPos> candidates, Vec3 center, int debris, Vec3 outward) {
        List<BlockPos> cand = new ArrayList<>(candidates);
        BlockPos cb = BlockPos.containing(center);
        cand.sort(Comparator.comparingDouble(pp -> pp.distSqr(cb)));
        int limit = HxhConfig.BLOCK_LIMIT.get();
        int maxDebris = Math.min(debris, HxhConfig.DEBRIS_MAX.get());
        int removed = 0, flying = 0, puffs = 0;
        for (BlockPos pos : cand) {
            if (removed >= limit) break;
            BlockState st = l.getBlockState(pos);
            if (st.isAir() || !allowed(p, l, pos, st)) continue;
            boolean fly = flying < maxDebris && l.random.nextInt(3) == 0 && st.isCollisionShapeFullBlock(l, pos);
            if (fly) {
                FallingBlockEntity fb = FallingBlockEntity.fall(l, pos, st);
                launch(l, fb, pos, center, outward, st);
                flying++;
            } else {
                if (HxhConfig.DROP_BLOCKS.get()) Block.dropResources(st, l, pos, null, p, p.getMainHandItem());
                l.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                if (puffs < 20 && l.random.nextInt(3) == 0) {
                    puffs++;
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.12);
                }
            }
            removed++;
        }
        return removed;
    }

    private static void launch(ServerLevel l, FallingBlockEntity fb, BlockPos pos, Vec3 center, Vec3 outward, BlockState st) {
        Vec3 dir = new Vec3(pos.getX() + 0.5 - center.x, 0, pos.getZ() + 0.5 - center.z);
        dir = dir.lengthSqr() < 0.01 ? new Vec3(l.random.nextDouble() - 0.5, 0, l.random.nextDouble() - 0.5).normalize() : dir.normalize();
        double sp = 0.30 + l.random.nextDouble() * 0.45;
        double ox = outward == null ? 0 : outward.x * 0.35;
        double oz = outward == null ? 0 : outward.z * 0.35;
        fb.dropItem = false;
        fb.setDeltaMovement(dir.x * sp + ox, 0.45 + l.random.nextDouble() * 0.7, dir.z * sp + oz);
        fb.hurtMarked = true;
        Scheduler.later(14 + l.random.nextInt(10), () -> {
            if (!fb.isRemoved()) {
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), fb.getX(), fb.getY() + 0.5, fb.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                fb.discard();
            }
        });
    }

    private Destruction() {}
}
