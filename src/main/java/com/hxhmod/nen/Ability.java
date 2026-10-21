package com.hxhmod.nen;

import com.hxhmod.HxhMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Ability {
    @FunctionalInterface
    public interface Effect {
        /** @return false, wenn die Fähigkeit nicht ausgelöst wurde (z. B. kein Ziel) - dann keine Kosten. */
        boolean run(ServerPlayer p, ServerLevel level, int nenLevel);
    }

    public final String id;
    public final NenType type; // null = allgemeine Technik (Gyo, En)
    public final int minLevel, cost, cooldown;
    private final Effect effect;

    public Ability(String id, NenType type, int minLevel, int cost, int cooldown, Effect effect) {
        this.id = id;
        this.type = type;
        this.minLevel = minLevel;
        this.cost = cost;
        this.cooldown = cooldown;
        this.effect = effect;
    }

    public boolean run(ServerPlayer p, ServerLevel l) {
        return effect.run(p, l, NenData.getLevel(p));
    }

    public String langKey() {
        return "ability." + HxhMod.MODID + "." + id;
    }
}
