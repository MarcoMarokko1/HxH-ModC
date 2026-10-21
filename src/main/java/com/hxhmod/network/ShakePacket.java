package com.hxhmod.network;

import com.hxhmod.client.ClientNenData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: Kamerawackeln, weißer Blitz (flash Ticks) und FOV-Stoß. */
public record ShakePacket(float intensity, int ticks, int flash, float fov) {
    public static void encode(ShakePacket m, FriendlyByteBuf b) {
        b.writeFloat(m.intensity);
        b.writeVarInt(m.ticks);
        b.writeVarInt(m.flash);
        b.writeFloat(m.fov);
    }

    public static ShakePacket decode(FriendlyByteBuf b) {
        return new ShakePacket(b.readFloat(), b.readVarInt(), b.readVarInt(), b.readFloat());
    }

    public static void handle(ShakePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (m.intensity >= ClientNenData.shakeIntensity || ClientNenData.shakeTicks <= 0) {
                ClientNenData.shakeIntensity = m.intensity;
            }
            ClientNenData.shakeTicks = Math.max(ClientNenData.shakeTicks, m.ticks);
            if (m.flash > 0) {
                ClientNenData.flashTicks = m.flash;
                ClientNenData.flashMax = m.flash;
            }
            if (m.fov != 0f) {
                ClientNenData.fovKick = m.fov;
                ClientNenData.fovTicks = 8;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
