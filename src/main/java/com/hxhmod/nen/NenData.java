package com.hxhmod.nen;

import com.hxhmod.HxhConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/** Spielerdaten im persistenten NBT (überlebt Tod/Neustart). stance: 0 offen, 1 Ten, 2 Zetsu, 3 Ren. */
public final class NenData {
    public static final int MAX_LEVEL = 10;

    private static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains("hxhmod", Tag.TAG_COMPOUND)) persisted.put("hxhmod", new CompoundTag());
        return persisted.getCompound("hxhmod");
    }

    public static NenType getType(Player p) {
        CompoundTag t = tag(p);
        return t.contains("type") ? NenType.byOrdinal(t.getInt("type")) : null;
    }

    public static void setType(Player p, NenType type) {
        CompoundTag t = tag(p);
        if (type == null) t.remove("type"); else t.putInt("type", type.ordinal());
    }

    public static int getLevel(Player p) { return Mth.clamp(tag(p).getInt("level"), 1, MAX_LEVEL); }

    public static void setLevel(Player p, int level) {
        CompoundTag t = tag(p);
        t.putInt("level", Mth.clamp(level, 1, MAX_LEVEL));
        t.putInt("xp", 0);
    }

    public static int getXp(Player p) { return tag(p).getInt("xp"); }

    public static int xpNeeded(int level) { return 100 * level; }

    /** @return true bei Level-Up */
    public static boolean addXp(Player p, int amount) {
        CompoundTag t = tag(p);
        int lvl = getLevel(p);
        if (lvl >= MAX_LEVEL) return false;
        int xp = t.getInt("xp") + amount;
        boolean up = false;
        while (lvl < MAX_LEVEL && xp >= xpNeeded(lvl)) {
            xp -= xpNeeded(lvl);
            lvl++;
            up = true;
        }
        t.putInt("level", lvl);
        t.putInt("xp", lvl >= MAX_LEVEL ? 0 : xp);
        return up;
    }

    public static float maxAura(Player p) {
        return HxhConfig.BASE_AURA.get() + (getLevel(p) - 1) * HxhConfig.AURA_PER_LEVEL.get();
    }

    public static float getAura(Player p) {
        CompoundTag t = tag(p);
        return t.contains("aura") ? t.getFloat("aura") : maxAura(p);
    }

    public static void setAura(Player p, float v) { tag(p).putFloat("aura", Mth.clamp(v, 0, maxAura(p))); }

    public static void addAura(Player p, float v) { setAura(p, getAura(p) + v); }

    public static int getStance(Player p) { return tag(p).getInt("stance"); }

    public static void setStance(Player p, int s) { tag(p).putInt("stance", s); }

    public static int getSelected(Player p) { return tag(p).getInt("selected"); }

    public static void setSelected(Player p, int s) { tag(p).putInt("selected", s); }

    public static void reset(Player p) {
        CompoundTag t = tag(p);
        for (String k : new ArrayList<>(t.getAllKeys())) t.remove(k);
    }

    private NenData() {}
}
