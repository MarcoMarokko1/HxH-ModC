package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import com.hxhmod.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/**
 * Träger für 3D-Effekt-Modelle. Server spawnt ihn mit allen Parametern, Server UND Client berechnen Bahn und
 * Animation selbst aus der Zeit (tickCount), es werden keine Positionen übertragen.
 */
public class FxEntity extends Entity implements IEntityAdditionalSpawnData {
    public static final int DRAGON = 0, FIST = 1, PALM = 2, SCISSORS = 3, CLAWS = 4, BOLT = 5, TORNADO = 6, DOME = 7, WINGS = 8, SWARM = 9, COCOON = 10;

    public int kind, nen, life = 10, pathTicks = 10, path;
    public float scale = 1f, f0, f1;
    public Vec3 p0 = Vec3.ZERO, p1 = Vec3.ZERO;

    public FxEntity(EntityType<? extends FxEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf b) {
        b.writeVarInt(kind);
        b.writeVarInt(nen);
        b.writeVarInt(life);
        b.writeVarInt(pathTicks);
        b.writeVarInt(path);
        b.writeFloat(scale);
        b.writeFloat(f0);
        b.writeFloat(f1);
        b.writeDouble(p0.x);
        b.writeDouble(p0.y);
        b.writeDouble(p0.z);
        b.writeDouble(p1.x);
        b.writeDouble(p1.y);
        b.writeDouble(p1.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf b) {
        kind = b.readVarInt();
        nen = b.readVarInt();
        life = b.readVarInt();
        pathTicks = b.readVarInt();
        path = b.readVarInt();
        scale = b.readFloat();
        f0 = b.readFloat();
        f1 = b.readFloat();
        p0 = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        p1 = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        setPos(p0.x, p0.y, p0.z);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 pos = FxPath.position(this, this.tickCount);
        this.setPos(pos.x, pos.y, pos.z);
        if (!this.level().isClientSide && this.tickCount >= this.life) this.discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return true;
    }

    /** Ungefähre Geschwindigkeit des Kopfes in Blöcken pro Tick (bestimmt den Abstand der Drachen-Segmente). */
    public double speedHint() {
        double s;
        if (path == 1) s = f0 * 0.6 + f1 / Math.max(1, pathTicks);
        else s = p1.subtract(p0).length() / Math.max(1, pathTicks);
        return Math.max(0.5, s);
    }

    public static FxEntity spawn(ServerLevel l, int kind, NenType t, int life, int pathTicks, float scale, int path,
                                 Vec3 p0, Vec3 p1, float f0, float f1) {
        if (!HxhConfig.MODELS_3D.get()) return null;
        FxEntity e = ModEntities.FX.get().create(l);
        if (e == null) return null;
        e.kind = kind;
        e.nen = t.ordinal();
        e.life = Math.max(2, life);
        e.pathTicks = Math.max(1, pathTicks);
        e.scale = scale;
        e.path = path;
        e.p0 = p0;
        e.p1 = p1;
        e.f0 = f0;
        e.f1 = f1;
        e.setPos(p0.x, p0.y, p0.z);
        l.addFreshEntity(e);
        return e;
    }

    public static FxEntity spawn(ServerLevel l, int kind, NenType t, int life, float scale, int path,
                                 Vec3 p0, Vec3 p1, float f0, float f1) {
        return spawn(l, kind, t, life, life, scale, path, p0, p1, f0, f1);
    }

    /**
     * Schmetterlingsflügel am Rücken von `owner`. period = Ticks pro Flügelschlag (offenster Punkt bei Tick 0, period, 2*period ...),
     * spread = Spreizung (0 ruhig ... 1 kraftvoll). pathTicks wird bei diesem Typ als Schlagdauer genutzt.
     */
    public static FxEntity wings(ServerLevel l, NenType t, Entity owner, int life, float scale, float spread, int period) {
        return spawn(l, WINGS, t, life, Math.max(2, period), scale, 2, owner.position(), owner.position(), (float) owner.getId(), spread);
    }

    /** Drache, der von `from` nach `to` fliegt (path 0) und danach noch den Körper nachzieht. */
    public static FxEntity dragonFly(ServerLevel l, NenType t, Vec3 from, Vec3 to, int ticks, double wobble, boolean big) {
        float scale = big ? 4.6f : 2.3f;
        double speed = Math.max(0.5, to.subtract(from).length() / Math.max(1, ticks));
        int tail = (int) Math.ceil(18 * 0.32 * scale / speed) + 3;
        return spawn(l, DRAGON, t, ticks + tail, ticks, scale, 0, from, to, (float) wobble, 0f);
    }

    /** Drache, der sich spiralförmig um `center` windet (path 1). */
    public static FxEntity dragonCoil(ServerLevel l, NenType t, Vec3 center, int ticks, double radius, double rise, boolean big) {
        return spawn(l, DRAGON, t, ticks, ticks, big ? 4.6f : 2.3f, 1, center, center, (float) radius, (float) rise);
    }
}
