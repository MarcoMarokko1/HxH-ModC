package com.hxhmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: starte eine Kamera-Cutscene (id, Dauer in Ticks, Fokuspunkt in Weltkoordinaten). */
public record CutscenePacket(String id, int ticks, double fx, double fy, double fz) {
    public static void encode(CutscenePacket m, FriendlyByteBuf b) {
        b.writeUtf(m.id, 32);
        b.writeVarInt(m.ticks);
        b.writeDouble(m.fx);
        b.writeDouble(m.fy);
        b.writeDouble(m.fz);
    }

    public static CutscenePacket decode(FriendlyByteBuf b) {
        return new CutscenePacket(b.readUtf(32), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble());
    }

    public static void handle(CutscenePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.hxhmod.client.ClientCutscene.start(m.id, m.ticks, new Vec3(m.fx, m.fy, m.fz))));
        ctx.get().setPacketHandled(true);
    }
}
