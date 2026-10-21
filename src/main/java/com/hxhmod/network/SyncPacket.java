package com.hxhmod.network;

import com.hxhmod.client.ClientNenData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: HUD-Daten. */
public record SyncPacket(int type, int level, int xp, float aura, float maxAura, int stance, int selected,
                         int cdLeft, int cdTotal) {
    public static void encode(SyncPacket m, FriendlyByteBuf b) {
        b.writeInt(m.type);
        b.writeVarInt(m.level);
        b.writeVarInt(m.xp);
        b.writeFloat(m.aura);
        b.writeFloat(m.maxAura);
        b.writeVarInt(m.stance);
        b.writeVarInt(m.selected);
        b.writeVarInt(m.cdLeft);
        b.writeVarInt(m.cdTotal);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        return new SyncPacket(b.readInt(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientNenData.type = m.type;
            ClientNenData.level = m.level;
            ClientNenData.xp = m.xp;
            ClientNenData.aura = m.aura;
            ClientNenData.maxAura = m.maxAura;
            ClientNenData.stance = m.stance;
            ClientNenData.selected = m.selected;
            ClientNenData.cdLeft = m.cdLeft;
            ClientNenData.cdTotal = m.cdTotal;
        });
        ctx.get().setPacketHandled(true);
    }
}
