package com.hxhmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: spiele Körper-Animation `name` beim Spieler mit der Entity-ID. */
public record AnimPacket(int entityId, String name) {
    public static void encode(AnimPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.entityId);
        b.writeUtf(m.name, 64);
    }

    public static AnimPacket decode(FriendlyByteBuf b) { return new AnimPacket(b.readVarInt(), b.readUtf(64)); }

    public static void handle(AnimPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.hxhmod.client.ClientAnim.play(m.entityId, m.name)));
        ctx.get().setPacketHandled(true);
    }
}
