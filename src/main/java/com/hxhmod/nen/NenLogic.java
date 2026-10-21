package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import com.hxhmod.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/** Zentrale Spiellogik: Fähigkeiten auslösen, Haltungen (Ten/Zetsu/Ren), Erwecken, Level-Up. */
public final class NenLogic {

    public static void useSelected(ServerPlayer p) {
        NenType t = NenData.getType(p);
        if (t == null) { p.displayClientMessage(Component.translatable("msg.hxhmod.no_nen"), true); return; }
        List<Ability> list = Abilities.forType(t);
        use(p, list.get(Math.floorMod(NenData.getSelected(p), list.size())));
    }

    public static void use(ServerPlayer p, Ability a) {
        NenType t = NenData.getType(p);
        if (t == null) { p.displayClientMessage(Component.translatable("msg.hxhmod.no_nen"), true); return; }
        if (NenData.getLevel(p) < a.minLevel) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.locked", a.minLevel), true);
            return;
        }
        if (NenData.getStance(p) == 2) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.zetsu_block"), true);
            return;
        }
        int cd = Cooldowns.remaining(p, a.id);
        if (cd > 0) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.cooldown", String.format("%.1f", cd / 20.0)), true);
            return;
        }
        int cost = (int) Math.ceil(a.cost * HxhConfig.COST_MULT.get());
        if (NenData.getAura(p) < cost) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.no_aura"), true);
            return;
        }
        ServerLevel l = p.serverLevel();
        if (!a.run(p, l)) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.no_target"), true);
            return;
        }
        NenData.setAura(p, NenData.getAura(p) - cost);
        Cooldowns.set(p, a.id, (int) (a.cooldown * HxhConfig.COOLDOWN_MULT.get()));
        if (NenData.addXp(p, 5 + a.minLevel * 2 + a.cost / 10)) levelUp(p, l, t);
        ModNetwork.sync(p);
    }

    public static void cycle(ServerPlayer p, int dir) {
        NenType t = NenData.getType(p);
        if (t == null) return;
        int size = Abilities.forType(t).size();
        NenData.setSelected(p, Math.floorMod(NenData.getSelected(p) + dir, size));
        FX.sound(p.serverLevel(), p.position(), SoundEvents.UI_BUTTON_CLICK.value(), 0.4f, 1.4f);
        ModNetwork.sync(p);
    }

    /** s: 1 = Ten, 2 = Zetsu, 3 = Ren. Nochmaliges Drücken schaltet zurück auf offen (0). */
    public static void toggleStance(ServerPlayer p, int s) {
        if (NenData.getType(p) == null) { p.displayClientMessage(Component.translatable("msg.hxhmod.no_nen"), true); return; }
        int n = NenData.getStance(p) == s ? 0 : s;
        if (n == 3 && NenData.getAura(p) < 10) {
            p.displayClientMessage(Component.translatable("msg.hxhmod.no_aura"), true);
            return;
        }
        NenData.setStance(p, n);
        p.displayClientMessage(Component.translatable("msg.hxhmod.stance." + n), true);
        ServerLevel l = p.serverLevel();
        SoundEvent snd = switch (n) {
            case 3 -> SoundEvents.BEACON_ACTIVATE;
            case 2 -> SoundEvents.BEACON_DEACTIVATE;
            case 1 -> SoundEvents.AMETHYST_BLOCK_CHIME;
            default -> SoundEvents.AMETHYST_BLOCK_RESONATE;
        };
        FX.sound(l, p.position(), snd, 0.8f, n == 3 ? 0.8f : 1.2f);
        if (n == 3) {
            NenType t = NenData.getType(p);
            for (int i = 1; i <= 3; i++) {
                final double r = i * 1.3;
                Scheduler.later(i, () -> FX.ring(l, t.dust(1.4f), p.position().add(0, 0.1, 0), r, (int) (r * 10)));
            }
            FX.burst(l, t.accent, p.position().add(0, 1, 0), 30, 0.25);
            FX.shake(p, 0.4f, 8);
            if (t == NenType.XENO) Dragons.coil(p, t, 18, 2.0, 4.0, true);
        }
        ModNetwork.sync(p);
    }

    public static void awaken(ServerPlayer p, NenType t) {
        NenData.setType(p, t);
        NenData.setSelected(p, 0);
        NenData.setAura(p, NenData.maxAura(p));
        ServerLevel l = p.serverLevel();
        p.displayClientMessage(Component.translatable("msg.hxhmod.awakened", Component.translatable("nen.hxhmod.type." + t.id)), true);
        for (int i = 1; i <= 5; i++) {
            final double r = i * 0.9;
            Scheduler.later(i * 2, () -> FX.ring(l, t.dust(1.5f), p.position().add(0, 0.1 + r * 0.3, 0), r, (int) (r * 12)));
        }
        FX.column(l, t.dust(1.6f), p.position(), 4, 0.25);
        FX.burst(l, ParticleTypes.TOTEM_OF_UNDYING, p.position().add(0, 1, 0), 40, 0.4);
        FX.sound(l, p.position(), SoundEvents.PLAYER_LEVELUP, 1f, 0.8f);
        FX.shake(p, 0.5f, 12);
        ModNetwork.sync(p);
    }

    /** Nen-Erfahrung vergeben (inkl. Level-Up-Effekt). */
    public static void gainXp(ServerPlayer p, int amount) {
        NenType t = NenData.getType(p);
        if (t != null && NenData.addXp(p, amount)) levelUp(p, p.serverLevel(), t);
    }

    private static void levelUp(ServerPlayer p, ServerLevel l, NenType t) {
        p.displayClientMessage(Component.translatable("msg.hxhmod.levelup", NenData.getLevel(p)), true);
        FX.burst(l, ParticleTypes.TOTEM_OF_UNDYING, p.position().add(0, 1, 0), 30, 0.35);
        FX.ring(l, t.dust(1.5f), p.position().add(0, 0.1, 0), 1.5, 24);
        FX.sound(l, p.position(), SoundEvents.PLAYER_LEVELUP, 0.9f, 1.0f);
    }

    private NenLogic() {}
}
