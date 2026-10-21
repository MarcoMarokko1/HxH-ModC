package com.hxhmod.network;

import com.hxhmod.nen.Abilities;
import com.hxhmod.nen.Combos;
import com.hxhmod.nen.NenLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: Tastendruck. 0 nutzen, 1 nächste, 2 vorherige, 3 Ten, 4 Zetsu, 5 Ren, 6 Gyo, 7 En. */
public record ActionPacket(int action) {
    public static void encode(ActionPacket m, FriendlyByteBuf buf) { buf.writeVarInt(m.action); }

    public static ActionPacket decode(FriendlyByteBuf buf) { return new ActionPacket(buf.readVarInt()); }

    public static void handle(ActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer p = ctx.get().getSender();
            if (p == null) return;
            switch (m.action) {
                case 0 -> NenLogic.useSelected(p);
                case 1 -> NenLogic.cycle(p, 1);
                case 2 -> NenLogic.cycle(p, -1);
                case 3 -> NenLogic.toggleStance(p, 1);
                case 4 -> NenLogic.toggleStance(p, 2);
                case 5 -> NenLogic.toggleStance(p, 3);
                case 6 -> NenLogic.use(p, Abilities.GYO);
                case 7 -> NenLogic.use(p, Abilities.EN);
                case 8 -> Combos.attack(p);
                default -> { }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
