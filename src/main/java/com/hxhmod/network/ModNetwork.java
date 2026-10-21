package com.hxhmod.network;

import com.hxhmod.HxhConfig;
import com.hxhmod.HxhMod;
import com.hxhmod.nen.Abilities;
import com.hxhmod.nen.Ability;
import com.hxhmod.nen.Cooldowns;
import com.hxhmod.nen.NenData;
import com.hxhmod.nen.NenType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HxhMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ActionPacket.class, ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, ShakePacket.class, ShakePacket::encode, ShakePacket::decode, ShakePacket::handle);
        CHANNEL.registerMessage(id++, AnimPacket.class, AnimPacket::encode, AnimPacket::decode, AnimPacket::handle);
        CHANNEL.registerMessage(id++, CutscenePacket.class, CutscenePacket::encode, CutscenePacket::decode, CutscenePacket::handle);
    }

    /** Schickt dem Spieler seinen aktuellen Nen-Zustand für das HUD. */
    public static void sync(ServerPlayer p) {
        NenType t = NenData.getType(p);
        int cdLeft = 0, cdTotal = 1;
        if (t != null) {
            List<Ability> list = Abilities.forType(t);
            Ability a = list.get(Math.floorMod(NenData.getSelected(p), list.size()));
            cdLeft = Cooldowns.remaining(p, a.id);
            cdTotal = Math.max(1, (int) (a.cooldown * HxhConfig.COOLDOWN_MULT.get()));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new SyncPacket(
                t == null ? -1 : t.ordinal(), NenData.getLevel(p), NenData.getXp(p),
                NenData.getAura(p), NenData.maxAura(p), NenData.getStance(p),
                NenData.getSelected(p), cdLeft, cdTotal));
    }

    private ModNetwork() {}
}
